package ChatBot;

import java.lang.InterruptedException;
import java.lang.Thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
  public static final int HP_UNIT = 5;
  public static boolean isProd = false;

  private static final String[] mainButtons = {"fight", "profile"};
  private static final String[] levelPointsButtons = {
    "improve strength", "improve vitality", "improve luck"
  };
  private static final int CHAT_TIMEOUT = 600;
  private static final int FIGHT_TIMEOUT = 60;

  private static Set<Integer> activeChats = new HashSet<>();
  private static Set<Integer> injuredChats = new HashSet<>();
  private static Set<Integer> readyToFightChats = new HashSet<>();
  private static Set<Integer> fightingChats = new HashSet<>();
  private static ArrayList<String[]> dict = null;

  private static int curTime;

  public static void main(String[] args)
      throws InterruptedException, Exception {
    initialize(args);
    System.out.println("German Club Server started...");
    while (true) {
      try {
        int maxUpdateId = Storage.getMaxUpdateId();
        // TODO: download updates async and put to queue
        Telegram.Update[] updates = TelegramApi.getUpdates(maxUpdateId + 1);
        Arrays.sort(updates, new Comparator<Telegram.Update>() {
          public int compare(Telegram.Update u1, Telegram.Update u2) {
            return u1.update_id - u2.update_id;
          }
        });
        // Handle user commands
        for (Telegram.Update upd : updates) {
          Storage.saveMaxUpdateId(upd.update_id);
          if (upd.message != null && upd.message.text != null) {
            handleUpdate(upd);
          }
        }
        // Background/async operations for each client
        Messenger.sendSheduledMessages();
        updateCurTime();
        restoreHpIfNeeded(Storage.getClientsByChatIds(injuredChats));
        assignBotsIfTimeout(Storage.getClientsByChatIds(readyToFightChats));
        Client[] fightingClients = Storage.getClientsByChatIds(fightingChats);
        sendTimoutWarningsIfNeeded(fightingClients);
        stopFightsTimeoutIfNeeded(fightingClients);
      } catch (Exception e) {
        if (isProd) {
          Logger.logException(e);
        } else {
          throw e;
        }
      }
      Thread.sleep(500); // 2s
    }
  }

  private static void initialize(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: ChatBot.jar path/to/db");
      System.exit(0);
    }
    Logger.setDbPath(args[0]);
    Logger.initialize();
    dict = Logger.getDictionary();
    TelegramApi.initialize();
    Phrases.initialize();

    if (args.length > 1 && args[1].equals("PROD")) {
      isProd = true;
    }

    // Read active & ready to fight clients
    updateCurTime();

    Storage.forEachClient(new ClientDo() {
      public void run(Client client) {
        if (client == null) {
          return; // this shouldn't happen
        }
        if (client.chatId < 0) {
          return; // bots have no async logic as of now
        }
        if (client.lastActivity > curTime - CHAT_TIMEOUT) {
          activeChats.add(client.chatId);
        }
        if (client.hp < client.getMaxHp()) {
          injuredChats.add(client.chatId);
        }
        if (client.status == Client.Status.READY_TO_FIGHT) {
          readyToFightChats.add(client.chatId);
        }
        if (client.status == Client.Status.FIGHTING) {
          fightingChats.add(client.chatId);
        }
      }
    });
  }

  private static void assignBotsIfTimeout(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.READY_TO_FIGHT
          || client.readyToFightSince > curTime - 10) {
        return;
      }
      Client bot = new Client(-client.chatId, client);
      prepareToFight(client, bot);
      Storage.saveClients(bot, client);

      Messenger.send(client.chatId, "You're now fighting with " + bot.username + ".");
      Messenger.send(client.chatId, getClientStats(bot));
      sendChallenge(client);
    }
  }

  private static void restoreHpIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.IDLE
          || client.hp == client.getMaxHp()
          || client.lastRestore > curTime - 3) {
        continue;
      }
      client.hp++;
      client.lastRestore = curTime;
      if (client.hp == client.getMaxHp()) {
        Messenger.send(client.chatId, "You are now fully recovered.");
        injuredChats.remove(client.chatId);
      }
      Storage.saveClient(client);
    }
  }

  private static void sendTimoutWarningsIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.FIGHTING
          || client.timeoutWarningSent
          || client.lastFightActivitySince > curTime - FIGHT_TIMEOUT) {
          continue;
      }
      client.timeoutWarningSent = true;
      Storage.saveClient(client);

      Messenger.send(client.chatId, "You have 5 seconds to make a decision.");
    }
  }

  private static void stopFightsTimeoutIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.FIGHTING
          || client.chatId < 0
          || !client.timeoutWarningSent
          || client.lastFightActivitySince > curTime - 50) {
        continue;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      Messenger.send(client.chatId, "Timeout!");
      Messenger.send(opponent.chatId, "Timeout!");
      finishFight(opponent, client);
      Storage.saveClients(opponent, client);
    }
  }

  private static void updateCurTime() {
    curTime = (int)(System.currentTimeMillis() / 1000L);
  }

  private static void handleUpdate(Telegram.Update upd) {
    updateCurTime();
    int chatId = upd.message.chat.id;
    Client client = Storage.getClientByChatId(chatId);
    boolean newClient = client == null;
    if (newClient) {
      String username;
      if (upd.message.from.username != null) {
        username = upd.message.from.username;
      } else {
        username = upd.message.from.first_name;
      }
      client = new Client(chatId, username);
    }
    client.lastActivity = curTime;
    activeChats.add(chatId);
    Storage.saveClient(client);

    if (newClient) {
      Messenger.send(client.chatId, "Welcome to the German Club!", mainButtons);
      sendToActiveUsers(PhraseGenerator.getJoinedTheFightClub(
          client.username));
    }

    String txt = upd.message.text;

    if (txt.equals("/start")) {
      return;
    }

    if (txt.equals("wiseman") || txt.equals("/wiseman")) {
      Messenger.send(client.chatId, PhraseGenerator.getWisdom(client).get(client.lang));
      return;
    }

    if (txt.equals("profile") || txt.equals("/profile")) {
      showProfile(client);
      return;
    }

    if (txt.startsWith("/username ")) {
      if (client.status != Client.Status.IDLE) {
        Messenger.send(client.chatId, "You can change your name only when you're not fighting.");
        return;
      }
      String newName = txt.substring(10, txt.length());
      if (!newName.matches("[A-z0-9]*")) {
        Messenger.send(client.chatId, "Incorrect name, please make sure it has " +
          "english characters and numbers only.");
        return;
      }
      changeUserName(client, newName);
      return;
    }

    if (txt.startsWith("improve ")) {
      String what = txt.substring(8, txt.length());
      if (client.levelPoints < 1) {
        Messenger.send(client.chatId, "You have no level points available. You will have some "
          + "when you level up.", mainButtons);
        return;
      }
      improveSkill(client, what);
      return;
    }

    if (txt.equals("fight") || txt.equals("/fight")) {
      if (client.status == Client.Status.FIGHTING) {
        Messenger.send(client.chatId, "You're already fighiting with somebody.");
        return;
      }
      if (client.status == Client.Status.READY_TO_FIGHT) {
        Messenger.send(client.chatId, "You're already searching for a victim.");
        return;
      }
      if (readyToFightChats.size() == 0) {
        setReadyToFight(client);
      } else {
        int opponentChatId = readyToFightChats.iterator().next();
        startFightReal(client, Storage.getClientByChatId(opponentChatId));
      }
      return;
    }

    if (txt.equals("/healing potion") || txt.startsWith("healing potion [")) {
      if (!client.hasItem(Game.Item.HPOTION)) {
        Messenger.send(client.chatId, "You don't have any potions.");
        return;
      }
      consumePotion(client);
      return;
    }

    if (txt.equals("/gp42")) {
      client.giveItem(Game.Item.HPOTION);
      Storage.saveClient(client);
      Messenger.send(client.chatId, "Now you have " + client.getItemNum(Game.Item.HPOTION) + " potions.");
      return;
    }

    if (txt.equals("/retreat42")) {
      if (client.status != Client.Status.FIGHTING) {
        return;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      Messenger.send(client.chatId, "Retreat42!");
      Messenger.send(opponent.chatId, "Retreat42!");
      finishFight(opponent, client);
      Storage.saveClients(opponent, client);
      return;
    }

    if (txt.equals("/kill42")) {
      if (client.status != Client.Status.FIGHTING) {
        return;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      Messenger.send(client.chatId, "Kill42 activated!");
      Messenger.send(opponent.chatId, "Kill42 activated!");
      finishFight(client, opponent);
      Storage.saveClients(opponent, client);
      return;
    }

    if (txt.equals("/reset42")) {
      Client cleanClient = new Client(client.chatId, client.username);
      Storage.saveClient(cleanClient);
      Messenger.send(cleanClient.chatId, "Reset42");
      return;
    }

    if (client.status == Client.Status.FIGHTING &&
        !txt.startsWith("/")) {
      client.lastFightActivitySince = curTime;
      client.timeoutWarningSent = false;
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      handleHit(client, opponent, txt);
      Storage.saveClients(opponent, client);
      if (opponent.chatId < 0 && opponent.status == Client.Status.FIGHTING) {
        activateBot(opponent);
      }
      return;
    }

    if (!txt.startsWith("/")) {
      String message = "\uD83D\uDCE2 " + client.username + ": " + txt;
      int numListeners = sendToActiveUsers(
        PhraseGenerator.getLangMap(message)) - 1;
      if (numListeners == 0) {
        Messenger.send(client.chatId, "You were not heard by anyone :(");
      }
      return;
    }


    // TODO: Add help page link here
    Messenger.send(client.chatId, "Use buttons below to make valid actions.");
  }

  // returns number of people who heard you
  private static int sendToActiveUsers(Map<String, String> message) {
    // If changed - also change the other function with the same name.
    int numListeners = 0;
    List<Integer> passive = new LinkedList<>();
    for (int recepientChatId : activeChats) {
      Client recepient = Storage.getClientByChatId(recepientChatId);
      if (recepient.lastActivity > curTime - CHAT_TIMEOUT) {
        Messenger.send(recepient.chatId, message.get(recepient.lang));
        numListeners++;
      } else {
        passive.add(recepientChatId);
      }
    }
    for (int passiveChatId : passive) {
      activeChats.remove(passiveChatId);
    }
    return numListeners;
  }

  private static void showProfile(Client client) {
    Messenger.send(client.chatId, getClientStats(client));
    if (client.levelPoints > 0) {
      Messenger.send(client.chatId, "You have " + client.levelPoints + " unassigned "
        + "level points.", levelPointsButtons);
    }
    Messenger.send(client.chatId, getInventoryDescription(client));
    if (!client.nameChangeHintSent) {
      Messenger.send(client.chatId, "You can change your name with the following command \n"
        + "`/username newname`.");
      client.nameChangeHintSent = true;
    }
    Storage.saveClient(client);
  }

  private static String getInventoryDescription(Client client) {
      StringBuilder result = new StringBuilder("You have:\n");
      int numValues = 0;
      for (Map.Entry<Integer, Integer> item : client.inventory.entrySet()) {
        numValues += item.getValue();
        if (item.getValue() <= 0) {
          continue;
        }
        result.append(item.getValue());
        result.append(" ");
        if (item.getValue() == 1) {
          result.append(Game.ITEM_VALUES[item.getKey()].singular);
        } else if (item.getValue() > 1) {
          result.append(Game.ITEM_VALUES[item.getKey()].plural);
        }
        result.append(".\n");
      }
      if (numValues == 0) {
        return "You don't have any items.";
      }
      return result.toString();
  }

  private static void setLanguage(Client client, String lang) {
    client.lang = lang;
    Storage.saveClient(client);
  }

  private static void changeUserName(Client client, String newName) {
    client.username = newName;
    Messenger.send(client.chatId, "Your name is now " + newName + ".");
    Storage.saveClient(client);
  }

  private static void activateBot(Client bot) {
    int[] prob = new int[] {97, 60, 50, 30, 20};
    int difficulty = Math.min(bot.challenge[1], prob.length - 1);
    boolean success = Utils.rndInRange(1, 100) < prob[difficulty];
    String response = "";
    if (success) {
      response = dict.get(bot.challenge[0])[0];
    }
    bot.lastFightActivitySince = curTime;
    bot.timeoutWarningSent = false;
    Client opponent = Storage.getClientByChatId(bot.fightingChatId);
    handleHit(bot, opponent, response);
    Storage.saveClients(opponent, bot);
  }

  private static void improveSkill(Client client, String skill) {
    int newValue = 0;
    if (skill.equals("strength")) {
      newValue = ++client.strength;
    } else if (skill.equals("vitality")) {
      newValue = ++client.vitality;
    } else if (skill.equals("luck")) {
      newValue = ++client.luck;
    }
    if (newValue == 0) {
      Messenger.send(client.chatId, "Don't know how to improve " + skill + ".");
      return;
    }
    client.levelPoints--;
    Messenger.send(client.chatId, "You have increased your " + skill + ", it is now "
      + newValue + ". You have " + client.levelPoints
      + " more level points.", mainButtons);
    Storage.saveClient(client);
  }

  private static void setReadyToFight(Client client) {
    // TODO: set ready to fight and save to index
    client.status = Client.Status.READY_TO_FIGHT;
    client.readyToFightSince = curTime;
    Storage.saveClient(client);
    readyToFightChats.add(client.chatId);
    sendToActiveUsers(PhraseGenerator.getReadyToFightPhrase(client));
  }

  private static void startFightReal(Client client, Client opponent) {
    prepareToFight(client, opponent);
    Storage.saveClients(client, opponent);
    Messenger.send(client.chatId, "You're now fighting with " + opponent.username + ".");
    Messenger.send(opponent.chatId, "You're now fighting with " + client.username + ".");
    Messenger.send(client.chatId, getClientStats(opponent));
    Messenger.send(opponent.chatId, getClientStats(client));
    sendChallenge(client);
    sendChallenge(opponent);
  }

  private static List<String> addPotions(Client client, List<String> options) {
    Storage.saveClient(client);
    int numPotions = client.getItemNum(Game.Item.HPOTION);
    if (numPotions > 0) {
      options.add("healing potion [" + numPotions + "]");
    }
    return options;
  }

  private static boolean hasArticle(String word) {
    return word.toLowerCase().startsWith("das ") ||
      word.toLowerCase().startsWith("der ") ||
      word.toLowerCase().startsWith("die ");
  }

  private static void consumePotion(Client client) {
    client.hp += HP_UNIT;
    if (client.hp > client.getMaxHp()) {
      client.hp = client.getMaxHp();
    }
    client.takeItem(Game.Item.HPOTION);
    Storage.saveClient(client);

    String clientMsg = "\uD83C\uDF76 Potion consumed, you have " +
        client.getItemNum(Game.Item.HPOTION) + " left. " +
        "[" + client.hp + "/" + client.getMaxHp() + "]";
    if (client.status == Client.Status.FIGHTING) {
      Messenger.send(client.chatId, clientMsg);
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      Messenger.send(opponent.chatId, "\uD83C\uDF76 " + client.username + " have consumed a healing potion " +
      "[" + client.hp + "/" + client.getMaxHp() + "]");
    } else {
      Messenger.send(client.chatId, clientMsg);
    }
  }

  private static Client.BodyPart getBodyPartFromString(String str) {
    if (str.equals("head")) {
      return Client.BodyPart.HEAD;
    }
    if (str.equals("torso") || str.equals("body")) {
      return Client.BodyPart.TORSO;
    }
    if (str.equals("legs")) {
      return Client.BodyPart.LEGS;
    }
    return null;
  }

  private static void makeHit(Client client, Client victim, boolean success) {
    String clientPrefix = "\uD83D\uDDE1 ";
    String victimPrefix = "\uD83D\uDEE1 ";
    String[] challengeWord = dict.get(client.challenge[0]);
    if (!success) {
      Messenger.send(victim.chatId, victimPrefix + 
        PhraseGenerator.incorrectTranslationToVictim(client, victim, challengeWord));
      Messenger.send(client.chatId,
                      clientPrefix +
                        PhraseGenerator.incorrectTranslationToOffender(client, victim, challengeWord),
                     addPotions(client, new ArrayList<String>()));
      return;
    }
    int clientHits = getDamage(client);
    victim.hp = Math.max(victim.hp - clientHits, 0);
    Messenger.send(victim.chatId, victimPrefix + 
        PhraseGenerator.correctTranslationToVictim(client,
                                                   victim,
                                                   clientHits,
                                                   challengeWord));
    Messenger.send(client.chatId,
                   clientPrefix +
                   PhraseGenerator.correctTranslationToOffender(client,
                                                                victim,
                                                                clientHits,
                                                                challengeWord),
                   addPotions(client, new ArrayList<String>()));
  }

  private static String normalizeGerman(String str) {
    return str.toLowerCase()
              .replace("ä", "a")
              .replace("ö", "o")
              .replace("ü", "u")
              .replace("ß", "s");
  }

  private static boolean isAnswerCorrect(String response, String answer) {
    return normalizeGerman(response).equals(normalizeGerman(answer));
  }

  private static void handleHit(Client client, Client opponent, String response) {
    boolean success = isAnswerCorrect(response, dict.get(client.challenge[0])[0]);
    if (success) {
      client.fightQuestions.put(client.challenge[0], client.challenge[1] + 1);
    }
    makeHit(client, opponent, success);
    // Finish fight if needed
    Client winner = null;
    Client loser = null;
    if (client.hp <= 0) {
      winner = opponent;
      loser = client;
    }
    if (opponent.hp <= 0) {
      winner = client;
      loser = opponent;
    }
    if (winner != null) {
      loser.hp = 0;
      finishFight(winner, loser);
    } else {
      sendChallenge(client);
    }
  }

  private static void finishFight(Client winner, Client loser) {
    winner.fightsWon++;
    winner.totalFights++;
    loser.totalFights++;
    int expGained = getExperience(loser);
    winner.exp += expGained;
    winner.status = Client.Status.IDLE;
    loser.status = Client.Status.IDLE;
    fightingChats.remove(winner.chatId);
    fightingChats.remove(loser.chatId);
    winner.timeoutWarningSent = false;
    loser.timeoutWarningSent = false;
    sendToActiveUsers(PhraseGenerator.getWonPhrase(winner, loser));
    Messenger.send(winner.chatId, "You gained " + expGained + " experience.");
    if (loser.chatId > 0) {
      winner.giveItem(Game.Item.HPOTION);
      Messenger.send(winner.chatId, "You found 1 healing potion!");
    } else {
      // logic for looting bots is here
      int rnd = Utils.rndInRange(1,6);
      if (rnd == 1) {
        winner.giveItem(Game.Item.HPOTION);
        Messenger.send(winner.chatId, "You found 1 healing potion!");
      } else if (rnd < 4) {
        Game.Item found = Game.ITEM_VALUES[Utils.getRndKeyWithWeight(
            loser.inventory)];
        winner.giveItem(found);
        Messenger.send(winner.chatId, "You found 1 " + found.singular +  "!");
      }
    }
    if (winner.hp < winner.getMaxHp() && winner.chatId > 0) {
      Messenger.send(winner.chatId, "Fight is finished. Your health will recover in "
        + 3*(winner.getMaxHp() - winner.hp) + " seconds.", mainButtons);
      injuredChats.add(winner.chatId);
    } else {
      Messenger.send(winner.chatId, "Fight is finished.", mainButtons);
    }
    if (loser.hp < loser.getMaxHp() && loser.chatId > 0) {
      Messenger.send(loser.chatId, "Fight is finished. Your health will recover in "
        + 3*(loser.getMaxHp() - loser.hp) + " seconds.", mainButtons);
      Messenger.flush(loser.chatId);
      injuredChats.add(loser.chatId);
    } else {
      Messenger.send(loser.chatId, "Fight is finished.", mainButtons);
    }
    levelUpIfNeeded(winner);
    levelUpIfNeeded(loser);
  }

  private static int getExperience(Client loser) {
    return 10 * loser.level;
  }

  private static String getClientStats(Client client) {
    String result = "*" + client.username + "*\n"
      + "Level: " + client.level + "\n"
      + "Health: " + client.hp + " (out of " + client.getMaxHp() + ")\n"
      + "Damage: 1 - " + client.getMaxDamage() + "\n"
      + "Strength: " + client.strength  + "\n"
      + "Vitality: " + client.vitality + "\n"
      + "Luck: " + client.luck;
    if (client.chatId > 0) {
      result += "\n"
        + "Experience: " + client.exp + " "
        + "(" + nextExp(client) + " needed to level up)\n"
        + "Fights won: " + client.fightsWon + " "
        + "(out of " + client.totalFights + ")\n";
    }
    return result;
  }

  private static int getDamage(Client client) {
    int result = 0;
    for (int i = 0; i <= client.challenge[1]; i++) {
      result += Utils.rndInRange(1, client.getMaxDamage());
    }
    return result;
  }

  private static void levelUpIfNeeded(Client client) {
    if (client.exp >= nextExp(client)) {
      client.level++;
      client.levelPoints++;
      Messenger.send(client.chatId, "You have achieved level " + client.level + "!\n",
        levelPointsButtons);
    }
  }

  private static void prepareToFight(Client client, Client opponent, int first) {
    client.status = Client.Status.FIGHTING;
    client.fightingChatId = opponent.chatId;
    client.lastFightActivitySince = curTime;
    client.timeoutWarningSent = false;
    readyToFightChats.remove(client.chatId);
    fightingChats.add(client.chatId);
    if (first == 0) {
      prepareToFight(opponent, client, 1);
    }
    pickQuestionsForFight(client);
  }

  private static void pickQuestionsForFight(Client client) {
    int questionsNum = client.level + 1;
    Set<Integer> questions = new HashSet<>();
    while (questions.size() < 5) {
      questions.add(Utils.rndInRange(0, dict.size() - 1));
    }
    client.fightQuestions.clear();
    for (int questionId : questions) {
      client.fightQuestions.put(questionId, 0); 
    }
  }

  private static void sendChallenge(Client client) {
    int questionId = client.challenge[0];
    while (questionId == client.challenge[0]) {
      questionId = Utils.getRnd(client.fightQuestions
                                      .keySet()
                                      .toArray(new Integer[] {}));
    }
    String[] question = dict.get(questionId);
    int difficulty = client.fightQuestions.get(questionId);
    client.challenge[0] = questionId;
    client.challenge[1] = difficulty;
    List<String> options = new ArrayList<>();

    if (difficulty == 1 && hasArticle(question[0])) {
      options = generateArticleOptions(question[0]);
    } else if (difficulty > 2) {
      addPotions(client, options);
      Messenger.send(client.chatId,
          "Please translate to German the word: " + question[1],
          options.toArray(new String[] {})); 
      return;
    } else if (difficulty > 0) {
      addPotions(client, options);
      Messenger.send(client.chatId,
          "Please translate to German the word: " + question[1] + ". Hint: `" +
          new String(Utils.shuffleCharArray(
              question[0].toLowerCase().toCharArray()))+ "`",
          options.toArray(new String[] {})); 
      return;
    } else {
      options = generateSimpleOptions(questionId);
    }

    addPotions(client, options);
    Messenger.send(client.chatId,
        "Please translate to German the word: " + question[1],
        options.toArray(new String[0])); 
  }

  private static ArrayList<String> generateSimpleOptions(int questionId) {
    ArrayList<String> options = new ArrayList<>();
    int index = 1;
    int answerIndex = Utils.rndInRange(1, 3);
    if (index == answerIndex) {
      options.add(dict.get(questionId)[0]);
    }
    while (index < 3) {
      int optionId = Utils.rndInRange(0, dict.size() - 1);
      if (optionId != questionId) {
        options.add(dict.get(optionId)[0]);
        index++;
        if (index == answerIndex) {
          options.add(dict.get(questionId)[0]);
        }
      }
    }
    return options;
  }

  private static ArrayList<String> generateArticleOptions(String word) {
    ArrayList<String> options = new ArrayList<>();
    options.add("das " + word.substring(4));
    options.add("der " + word.substring(4));
    options.add("die " + word.substring(4));
    return options;
  }

  static void prepareToFight(Client client, Client opponent) {
    prepareToFight(client, opponent, 0);
  }

  static int nextExp(Client client) {
    int levelDelta = 30;
    int result = 0;
    for (int i = 0; i < client.level; i++) {
      result = result + levelDelta * (int)Math.pow(2, i);
    }
    return result;
  }
}

