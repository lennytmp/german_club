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

  private static final String[] mainButtons = {"fight", "profile", "wiseman"};
  private static final String[] langButtons = {"English", "Русский"};
  private static final String[] levelPointsButtons = {
    "improve strength", "improve vitality", "improve luck"
  };
  private static final int CHAT_TIMEOUT = 600;

  private static Set<Integer> activeChats = new HashSet<>();
  private static Set<Integer> injuredChats = new HashSet<>();
  private static Set<Integer> readyToFightChats = new HashSet<>();
  private static Set<Integer> fightingChats = new HashSet<>();

  private static int curTime;

  public static void main(String[] args)
      throws InterruptedException, Exception {
    initialize(args);
    System.out.println("Fight Club Server started...");
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
      setFightingStatus(client, bot);
      generateRandomHitBlock(bot);
      Storage.saveClients(bot, client);

      msg(client,
          "You're now fighting with " + bot.username + ".",
          getFightingButtons(client));
      msg(client, getClientStats(bot));
      sendFightInstruction(client);
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
        msg(client, "You are now fully recovered.");
        injuredChats.remove(client.chatId);
      }
      Storage.saveClient(client);
    }
  }

  private static void sendTimoutWarningsIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.FIGHTING
          || (client.hit != null && client.block != null)
          || client.timeoutWarningSent
          || client.lastFightActivitySince > curTime - 30) {
          continue;
      }
      client.timeoutWarningSent = true;
      Storage.saveClient(client);

      msg(client, "You have 5 seconds to make a decision.");
    }
  }

  private static void stopFightsTimeoutIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.FIGHTING
          || !client.timeoutWarningSent
          || client.lastFightActivitySince > curTime - 50) {
        continue;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      msg(client, "Timeout!");
      msg(opponent, "Timeout!");
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
      msg(client, "Welcome to the Fight Club!", mainButtons);
      TelegramApi.sendHelp(client.chatId);
      msg(client, "Which language do you prefer?", langButtons);
      sendToActiveUsers(PhraseGenerator.getJoinedTheFightClub(
          client.username));
    }

    String txt = upd.message.text;

    if (txt.equals("/start")) {
      return;
    }

    if (txt.equals("English")) {
      setLanguage(client, "en");
      msg(client, "English language is set.", mainButtons);
      return;
    }

    if (txt.equals("Русский")) {
      setLanguage(client, "ru");
      msg(client, "Включен русский язык.", mainButtons);
      return;
    }

    if (txt.equals("wiseman") || txt.equals("/wiseman")) {
      msg(client, PhraseGenerator.getWisdom(client).get(client.lang));
      return;
    }

    if (txt.equals("profile") || txt.equals("/profile")) {
      showProfile(client);
      return;
    }

    if (txt.startsWith("/username ")) {
      if (client.status != Client.Status.IDLE) {
        msg(client, "You can change your name only when you're not fighting.");
        return;
      }
      String newName = txt.substring(10, txt.length());
      if (!newName.matches("[A-z0-9]*")) {
        msg(client, "Incorrect name, please make sure it has " +
          "english characters and numbers only.");
        return;
      }
      changeUserName(client, newName);
      return;
    }

    if (txt.startsWith("improve ")) {
      String what = txt.substring(8, txt.length());
      if (client.levelPoints < 1) {
        msg(client, "You have no level points available. You will have some "
          + "when you level up.", mainButtons);
        return;
      }
      improveSkill(client, what);
      return;
    }

    if (txt.equals("fight") || txt.equals("/fight")) {
      if (client.status == Client.Status.FIGHTING) {
        msg(client, "You're already fighiting with somebody.");
        return;
      }
      if (client.status == Client.Status.READY_TO_FIGHT) {
        msg(client, "You're already searching for a victim.");
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
    if (txt.startsWith("hit ")) {
      String where = txt.substring(4, txt.length());
      Client.BodyPart target = getBodyPartFromString(where);
      if (client.status != Client.Status.FIGHTING) {
        msg(client, "You need to start a fight first.", mainButtons);
        return;
      }
      if (target == null) {
        msg(client, "Don't know how to hit `" + where + "`.");
        return;
      }
      setHit(client, target);
      return;
    }

    if (txt.startsWith("block ")) {
      String where = txt.substring(6, txt.length());
      Client.BodyPart target = getBodyPartFromString(where);
      if (client.status != Client.Status.FIGHTING) {
        msg(client, "You need to start a fight first.");
        return;
      }
      if (target == null) {
        msg(client, "Don't know how to block `" + where + "`.");
        return;
      }
      setBlock(client, target);
      return;
    }

    if (txt.equals("/healing potion") || txt.startsWith("healing potion [")) {
      if (!client.hasItem(Game.Item.HPOTION)) {
        msg(client, "You don't have any potions.");
        return;
      }
      consumePotion(client);
      return;
    }

    if (txt.equals("/gp42")) {
      client.giveItem(Game.Item.HPOTION);
      Storage.saveClient(client);
      msg(client, "Now you have " + client.getItemNum(Game.Item.HPOTION) + " potions.");
      return;
    }

    if (txt.equals("/retreat42")) {
      if (client.status != Client.Status.FIGHTING) {
        return;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      msg(client, "Retreat42!");
      msg(opponent, "Retreat42!");
      finishFight(opponent, client);
      Storage.saveClients(opponent, client);
      return;
    }

    if (txt.equals("/kill42")) {
      if (client.status != Client.Status.FIGHTING) {
        return;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      msg(client, "Kill42 activated!");
      msg(opponent, "Kill42 activated!");
      finishFight(client, opponent);
      Storage.saveClients(opponent, client);
      return;
    }

    if (txt.equals("/reset42")) {
      Client cleanClient = new Client(client.chatId, client.username);
      Storage.saveClient(cleanClient);
      msg(cleanClient, "Reset42");
      return;
    }

    if (client.status == Client.Status.FIGHTING &&
        !txt.startsWith("/")) {
      String message = txt;
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      Map<String, String> sayingPhrase = PhraseGenerator.getSayingPhrase(client,
          message, opponent);
      msg(client, sayingPhrase.get(client.lang));
      msg(opponent, sayingPhrase.get(client.lang));
      return;
    }

    if (!txt.startsWith("/")) {
      String message = "\uD83D\uDCE2 " + client.username + ": " + txt;
      int numListeners = sendToActiveUsers(
        PhraseGenerator.getLangMap(message)) - 1;
      if (numListeners == 0) {
        msg(client, "You were not heard by anyone :(");
      }
      return;
    }

    // TODO: Add help page link here
    msg(client, "Use buttons below to make valid actions.");
  }

  // returns number of people who heard you
  private static int sendToActiveUsers(Map<String, String> message) {
    // If changed - also change the other function with the same name.
    int numListeners = 0;
    List<Integer> passive = new LinkedList<>();
    for (int recepientChatId : activeChats) {
      Client recepient = Storage.getClientByChatId(recepientChatId);
      if (recepient.lastActivity > curTime - CHAT_TIMEOUT) {
        msg(recepient, message.get(recepient.lang));
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
    msg(client, getClientStats(client));
    if (client.levelPoints > 0) {
      msg(client, "You have " + client.levelPoints + " unassigned "
        + "level points.", levelPointsButtons);
    }
    msg(client, getInventoryDescription(client));
    if (!client.nameChangeHintSent) {
      msg(client, "You can change your name with the following command \n"
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
    msg(client, "Your name is now " + newName + ".");
    Storage.saveClient(client);
  }

  private static void generateRandomHitBlock(Client client) {
    Client.BodyPart[] values = Client.BodyPart.values();
    client.hit = values[Utils.rndInRange(0, values.length - 1)];
    client.block = values[Utils.rndInRange(0, values.length - 1)];
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
      msg(client, "Don't know how to improve " + skill + ".");
      return;
    }
    client.levelPoints--;
    msg(client, "You have increased your " + skill + ", it is now "
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
    setFightingStatus(client, opponent);
    Storage.saveClients(client, opponent);
    msg(client,
        "You're now fighting with " + opponent.username + ".",
        getFightingButtons(client));
    msg(opponent,
        "You're now fighting with " + client.username + ".",
        getFightingButtons(opponent));
    msg(client, getClientStats(opponent));
    msg(opponent, getClientStats(client));
    sendFightInstruction(client);
    sendFightInstruction(opponent);
  }

  private static void sendFightInstruction(Client client) {
    if (client.fightsWon == 0) {
      msg(client, "You need to choose which part of your body to block and where to hit.");
    }
  }

  private static String[] getFightingButtons(Client client) {
    ArrayList<String> buttons = new ArrayList<>(
      Arrays.asList(new String[] {
        "hit head", "hit torso", "hit legs",
        "block head", "block torso", "block legs"
      })
    );
    int numPotions = client.getItemNum(Game.Item.HPOTION);
    if (numPotions > 0) {
      buttons.add("healing potion [" + numPotions + "]");
    }
    return buttons.toArray(new String[0]);
  }

  private static void setHit(Client client, Client.BodyPart target) {
    client.hit = target;
    client.lastFightActivitySince = curTime;
    client.timeoutWarningSent = false;
    Client opponent = Storage.getClientByChatId(client.fightingChatId);
    assert opponent != null;
    if (readyToHitBlock(client, opponent)) {
      handleHit(client, opponent);
    }
    Storage.saveClients(opponent, client);
  }

  private static void setBlock(Client client, Client.BodyPart target) {
    client.block = target;
    client.lastFightActivitySince = curTime;
    client.timeoutWarningSent = false;
    Client opponent = Storage.getClientByChatId(client.fightingChatId);
    assert opponent != null;
    if (readyToHitBlock(client, opponent)) {
      handleHit(client, opponent);
    }
    Storage.saveClients(opponent, client);
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
      msg(client, clientMsg, getFightingButtons(client));
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      msg(opponent, "\uD83C\uDF76 " + client.username + " have consumed a healing potion " +
      "[" + client.hp + "/" + client.getMaxHp() + "]");
    } else {
      msg(client, clientMsg);
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

  private static boolean readyToHitBlock(Client client, Client opponent) {
    if (client.hit == null) {
      msg(client, "Where do you want to hit?");
      return false;
    }
    if (client.block == null) {
      msg(client, "Where do you want to block?");
      return false;
    }
    if (opponent.hit == null || opponent.block == null) {
      msg(client, "Waiting for " + opponent.username + "...");
      return false;
    }
    return true;
  }

  private static void msg(Client client, String message) {
    msg(client, message, new String[] {});
  }

  private static void msg(int chatId, String message) {
    TelegramApi.say(chatId, message, new String[] {});
  }

  private static void msg(Client client, String message, String[] replies) {
    if (client.chatId < 0) {
      return; //no message for bots
    }
    TelegramApi.say(client.chatId, message, replies);
  }

  private static void makeAHit(Client client, Client victim) {
    Map<String, String> hitPhrase;
    String clientPrefix = "\uD83D\uDDE1 ";
    String victimPrefix = "\uD83D\uDEE1 ";
    if (victim.block == client.hit) {
      hitPhrase =
        PhraseGenerator.getBlockPhrase(client, victim, client.hit);
      msg(victim, victimPrefix + hitPhrase.get(victim.lang));
      msg(client, clientPrefix + hitPhrase.get(client.lang));
      return;
    }
    int clientHits = getDamage(client);
    victim.hp = Math.max(victim.hp - clientHits, 0);
    if (clientHits == 0) {
      hitPhrase =
        PhraseGenerator.getMissPhrase(client, victim, client.hit);
      msg(victim, victimPrefix + hitPhrase.get(victim.lang));
      msg(client, clientPrefix + hitPhrase.get(client.lang));
      return;
    }
    hitPhrase = PhraseGenerator.getHitPhrase(
      client,
      victim,
      client.hit,
      clientHits > client.getMaxDamage(),
      clientHits
    );
    msg(victim, victimPrefix + hitPhrase.get(victim.lang));
    msg(client, clientPrefix + hitPhrase.get(client.lang));
  }

  private static void handleHit(Client client, Client opponent) {
    boolean isBot = opponent.chatId < 0;
    // Who goes first
    Client first = client;
    Client second = opponent;
    int clientValue = client.luck;
    int opponentValue = opponent.luck;
    if (clientValue == opponentValue) {
      clientValue = Utils.rndInRange(0, 100);
      opponentValue = Utils.rndInRange(0, 100);
    }
    if (clientValue > opponentValue) {
      first = client;
      second = opponent;
    } else {
      first = opponent;
      second = client;
    }
    // Making hits
    makeAHit(first, second);
    if (second.hp == 0) {
      msg(first, "Lucky you! You didn't get any damage because "
        + second.username + " is defeated.");
      msg(second, "Oops, you didn't have much time to attack.");
    } else {
      makeAHit(second, first);
    }
    client.hit = null;
    client.block = null;
    if (!isBot) {
      opponent.hit = null;
      opponent.block = null;
    } else {
      generateRandomHitBlock(opponent);
    }
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
    msg(winner, "You gained " + expGained + " experience.");
    if (loser.chatId > 0) {
      winner.giveItem(Game.Item.HPOTION);
      msg(winner, "You found 1 healing potion!");
    } else {
      // logic for looting bots is here
      int rnd = Utils.rndInRange(1,6);
      if (rnd == 1) {
        winner.giveItem(Game.Item.HPOTION);
        msg(winner, "You found 1 healing potion!");
      } else if (rnd < 4) {
        Game.Item found = Game.ITEM_VALUES[Utils.getRndKeyWithWeight(
            loser.inventory)];
        winner.giveItem(found);
        msg(winner, "You found 1 " + found.singular +  "!");
      }
    }
    if (winner.hp < winner.getMaxHp() && winner.chatId > 0) {
      msg(winner, "Fight is finished. Your health will recover in "
        + 3*(winner.getMaxHp() - winner.hp) + " seconds.", mainButtons);
      injuredChats.add(winner.chatId);
    } else {
      msg(winner, "Fight is finished.", mainButtons);
    }
    if (loser.hp < loser.getMaxHp() && loser.chatId > 0) {
      msg(loser, "Fight is finished. Your health will recover in "
        + 3*(loser.getMaxHp() - loser.hp) + " seconds.", mainButtons);
      injuredChats.add(loser.chatId);
    } else {
      msg(loser, "Fight is finished.", mainButtons);
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
    // TODO(lenny): lvl30 has 100% chance to give crit?!
    int critRnd = Utils.rndInRange(0, 30);
    if (critRnd < client.luck) {
      return client.getMaxDamage() + Utils.rndInRange(0, client.getMaxDamage());
    }
    return Utils.rndInRange(0, client.getMaxDamage());
  }

  private static void levelUpIfNeeded(Client client) {
    if (client.exp >= nextExp(client)) {
      client.level++;
      client.levelPoints++;
      msg(client, "You have achieved level " + client.level + "!\n",
        levelPointsButtons);
    }
  }

  private static void setFightingStatus(Client client, Client opponent, int first) {
    client.status = Client.Status.FIGHTING;
    client.fightingChatId = opponent.chatId;
    client.lastFightActivitySince = curTime;
    client.timeoutWarningSent = false;
    readyToFightChats.remove(client.chatId);
    fightingChats.add(client.chatId);
    if (first == 0) {
      setFightingStatus(opponent, client, 1);
    }
  }

  static void setFightingStatus(Client client, Client opponent) {
    setFightingStatus(client, opponent, 0);
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

