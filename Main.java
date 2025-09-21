package FightLang;

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
  public static boolean isProd = false;

  private static final String[] MAIN_BUTTONS = { "fight", "profile", "task" };
  private static final String[] LEVEL_POINT_BUTTONS = {
      "improve strength", "improve vitality", "improve luck"
  };
  private static final int CHAT_TIMEOUT = 600;
  private static final int FIGHT_TIMEOUT = 60;
  private static final String TASK_FAIL = "Fail";
  private static final String TASK_SUCCESS = "Success";
  private static final String GAME_DESCRIPTION_PROMPT = "Du bist ein Rollenspiel. In diesem Spiel gibt es die folgenden Monster: " +
    "Bettler, Betrunkener, Crack-Süchtiger, Skelett, Zombie, Geist, Mumie, Lich, Vampir, Ghul, Nekromant, Teufel und Dämon. " +
    "Die Gegenstände, die Helden finden können, sind Gold, Silber, Asche, Papier, Klaue, Fangzahn, Wachs, Bandage, Seelenstein, " +
    "Fleisch, Knochen, Crack, Flasche, Münze.";
  private static final String NOT_FOUND_PROMPT = "In diesem Spiel suchte der Held nach etwas Nützlichem. "
      +
      "Du musst etwas sagen wie: „Du hast versucht, etwas Nützliches zu finden, aber du hast nichts gefunden.“ Du kannst dir einen "
      +
      "Grund ausdenken, wo ich gesucht habe oder warum ich nichts gefunden habe. Halte dich kurz und erwähne nicht, wonach ich ƒgesucht habe.";
  private static final String SOMETHING_FOUND_PROMPT = "Der Held hat gerade %sgefunden. Du musst es kurz beschreiben. " +
    "Zum Beispiel: „Du bist durch den Wald spaziert und hast eine alte Feuerstelle gefunden, in der ein riesiges Skelett lag. " + 
    "Nach genauer Untersuchung hast du die Krallen herausgezogen und in deinen Rucksack gesteckt.“" + 
    "Gib am Ende keine Zusammenfassung, der Spieler sollte den Text sorgfältig lesen, um zu verstehen, was er gefunden hat. Stelle keine Fragen. " +
    "Gehe davon aus, dass der Held diesen Gegenstand am Ende in seinen Rucksack steckt.";
  private static Set<Integer> activeChats = new HashSet<>();
  private static Set<Integer> injuredChats = new HashSet<>();
  private static Set<Integer> readyToFightChats = new HashSet<>();
  private static Set<Integer> fightingChats = new HashSet<>();

  private static int curTimeSeconds;

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
        cleanupDailySuccess();
        restoreHpIfNeeded(Storage.getClientsByChatIds(injuredChats));
        assignBotsIfTimeout(Storage.getClientsByChatIds(readyToFightChats));
        Client[] fightingClients = Storage.getClientsByChatIds(fightingChats);
        handleFightTimeouts(fightingClients);
      } catch (Exception e) {
        if (isProd) {
          Logger.logException(e);
        } else {
          throw e;
        }
      }
      Thread.sleep(500);
    }
  }

  private static void cleanupDailySuccess() {
    // Only reset stats at 4am in GMT+0 (roughly)
    if (curTimeSeconds / 60 / 60 % 24 != 4) {
      return;
    }
    Storage.forEachClient(new ClientDo() {
      public void run(Client client) {
        if (client.getLastDailyCleanup() + 24 * 60 * 60 < curTimeSeconds) {
          client.setSuccessToday(0);
          client.setLastDailyCleanup(curTimeSeconds);
        }
      }
    });
  }

  private static void initialize(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: FightLang.jar path/to/db");
      System.exit(0);
    }
    Logger.setDbPath(args[0]);
    Logger.initialize();
    TelegramApi.initialize();
    Logger.log("test");
    Gemini.initialize();
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
        if (client.lastActivity > curTimeSeconds - CHAT_TIMEOUT) {
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
          || client.readyToFightSince > curTimeSeconds - 10) {
        return;
      }
      Client bot = new Client(-client.chatId, client);
      prepareToFight(client, bot);
      Storage.saveClients(bot, client);

      Messenger.send(client.chatId, "You're now fighting with " + bot.username + ".");
      Messenger.send(client.chatId, getClientStats(bot));
      askTaskStatus(client);
    }
  }

  private static void restoreHpIfNeeded(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.IDLE
          || client.hp == client.getMaxHp()
          || client.lastRestore > curTimeSeconds - 3) {
        continue;
      }
      client.hp++;
      client.lastRestore = curTimeSeconds;
      if (client.hp == client.getMaxHp()) {
        Messenger.send(client.chatId, "You are now fully recovered.");
        injuredChats.remove(client.chatId);
      }
      Storage.saveClient(client);
    }
  }


  private static void handleFightTimeouts(Client[] clients) {
    for (Client client : clients) {
      if (client.status != Client.Status.FIGHTING
          || client.chatId < 0
          || client.lastFightActivitySince > curTimeSeconds - (FIGHT_TIMEOUT + 5)) {
        continue;
      }
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      // Reset activity since we're handling the timeout
      client.lastFightActivitySince = curTimeSeconds;
      // Timeout acts the same as pressing "Fail" - handle as failed task
      handleHitTask(client, opponent, false);
      Storage.saveClients(opponent, client);
      if (opponent.chatId < 0 && opponent.status == Client.Status.FIGHTING) {
        activateBotTask(opponent);
      }
    }
  }

  private static void updateCurTime() {
    curTimeSeconds = (int) (System.currentTimeMillis() / 1000L);
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
    client.lastActivity = curTimeSeconds;
    activeChats.add(chatId);
    Storage.saveClient(client);

    if (newClient) {
      Messenger.send(client.chatId, "Welcome to the German Club!", MAIN_BUTTONS);
      sendToActiveUsers(PhraseGenerator.getJoinedTheFightClub(
          client.username));
    }

    String txt = upd.message.text;

    if (txt.equals("/start")) {
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
            + "when you level up.", MAIN_BUTTONS);
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

    if (txt.equals("task") && client.status != Client.Status.FIGHTING) {
      client.incSuccessToday();
      Storage.saveClient(client);
      if (!Utils.roll(30)) {
        // Nothing found: with 50% chance send wiseman message, otherwise send the generated message
        boolean sendWisdom = Utils.roll(50);
        if (sendWisdom) {
          Messenger.send(client.chatId, PhraseGenerator.getWisdom(client));
        } else {
          String notFound = Gemini.AskGemini(GAME_DESCRIPTION_PROMPT + " " + NOT_FOUND_PROMPT);
          if (notFound == "") {
            notFound = "Du hast einen Spaziergang im Wald gemacht, aber nichts Nützliches gefunden.";
          }
          Messenger.send(client.chatId, notFound);
        }
        return;
      }
      Game.Item found = Utils.getRnd(new Game.Item[] { Game.Item.ASH, Game.Item.BANDAGE, Game.Item.BOTTLE });
      client.giveItem(found);
      Storage.saveClient(client);
      String foundMsg = Gemini.AskGemini(GAME_DESCRIPTION_PROMPT + " " + String.format(SOMETHING_FOUND_PROMPT, found.singular) ); 
      if (foundMsg == "") {
        foundMsg = String.format("Du hast 1 %s gefunden!", found.singular);
      }
      Messenger.send(client.chatId, foundMsg);
      return;
    }

    if (txt.equals("brew") && client.status != Client.Status.FIGHTING) {
      if (Game.canBrewPotion(client.inventory)) {
        client.inventory = Game.brewPotion(client.inventory);
        client.incSuccessToday();
        Storage.saveClient(client);
        Messenger.send(client.chatId, "After lot's of work, you have a new healing potion.");
        sendInventoryDescription(client);
      }
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

    if (txt.equals("/version")) {
      Messenger.send(client.chatId, "Version: 0.02");
      return;
    }

    if (client.status == Client.Status.FIGHTING &&
        (txt.equals(TASK_FAIL) || txt.equals(TASK_SUCCESS))) {
      boolean isSuccess = txt.equals(TASK_SUCCESS);
      if (isSuccess) {
        client.incSuccessToday();
      }
      client.lastFightActivitySince = curTimeSeconds;
      Client opponent = Storage.getClientByChatId(client.fightingChatId);
      handleHitTask(client, opponent, isSuccess);
      Storage.saveClients(opponent, client);
      if (opponent.chatId < 0 && opponent.status == Client.Status.FIGHTING) {
        activateBotTask(opponent);
      }
      return;
    }

    if (!txt.startsWith("/")) {
      String message = "\uD83D\uDCE2 " + client.username + ": " + txt;
      int numListeners = sendToActiveUsers(message) - 1;
      if (numListeners == 0) {
        Messenger.send(client.chatId, "You were not heard by anyone :(");
      }
      return;
    }

    // TODO: Add help page link here
    Messenger.send(client.chatId, "Use buttons below to make valid actions.");
  }

  // returns number of people who heard you
  private static int sendToActiveUsers(String message) {
    // If changed - also change the other function with the same name.
    int numListeners = 0;
    List<Integer> passive = new LinkedList<>();
    for (int recepientChatId : activeChats) {
      Client recepient = Storage.getClientByChatId(recepientChatId);
      if (recepient.lastActivity > curTimeSeconds - CHAT_TIMEOUT) {
        Messenger.send(recepient.chatId, message);
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
    Messenger.send(client.chatId, getClientStats(client), MAIN_BUTTONS);
    if (!client.nameChangeHintSent) {
      Messenger.send(client.chatId, "You can change your name with the following command \n"
          + "`/username newname`.", MAIN_BUTTONS);
      client.nameChangeHintSent = true;
      Storage.saveClient(client);
    }
    if (client.levelPoints > 0) {
      Messenger.send(client.chatId, "You have " + client.levelPoints + " unassigned "
          + "level points.", LEVEL_POINT_BUTTONS);
    }
    sendInventoryDescription(client);
  }

  private static void sendInventoryDescription(Client client) {
    String inventoryDesc = client.getInventoryDescription("\n");
    if (!inventoryDesc.isEmpty()) {
      inventoryDesc = "Du hast:\n" + inventoryDesc + "\n";
    } else {
      inventoryDesc = "Du hast nichts.";
    }
    Messenger.send(client.chatId, inventoryDesc, MAIN_BUTTONS);
    if (Game.canBrewPotion(client.inventory)) {
      String[] buttons = new String[MAIN_BUTTONS.length + 1];
      System.arraycopy(MAIN_BUTTONS, 0, buttons, 0, MAIN_BUTTONS.length);
      buttons[MAIN_BUTTONS.length] = "brew";
      Messenger.send(client.chatId, "You have all the ingredients to brew a healing potion", buttons);
    }
  }

  private static void changeUserName(Client client, String newName) {
    client.username = newName;
    Messenger.send(client.chatId, "Your name is now " + newName + ".");
    Storage.saveClient(client);
  }

  private static void activateBotTask(Client bot) {
    bot.lastFightActivitySince = curTimeSeconds;
    Client opponent = Storage.getClientByChatId(bot.fightingChatId);
    boolean isSuccess = Utils.roll(50);
    handleHitTask(bot, opponent, isSuccess);
    Storage.saveClients(opponent, bot);
  }

  private static void improveSkill(Client client, String skill) {
    int newValue = 0;
    if (skill.equals("strength")) {
      newValue = ++client.strength;
    } else if (skill.equals("vitality")) {
      newValue = ++client.vitality;
      client.hp = client.getMaxHp();
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
        + " more level points.", MAIN_BUTTONS);
    Storage.saveClient(client);
  }

  private static void setReadyToFight(Client client) {
    // TODO: set ready to fight and save to index
    client.status = Client.Status.READY_TO_FIGHT;
    client.readyToFightSince = curTimeSeconds;
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
    askTaskStatus(opponent);
    askTaskStatus(client);
  }

  private static void askTaskStatus(Client client) {
    Messenger.send(client.chatId, "Attempt at solving an exercise and report feedback",
        addPotions(client, new String[] { TASK_SUCCESS }));
  }

  private static String[] addPotions(Client client, String[] options) {
    Storage.saveClient(client);
    int numPotions = client.getItemNum(Game.Item.HPOTION);
    List<String> optionsList = new ArrayList<>(Arrays.asList(options));
    if (numPotions > 0) {
      optionsList.add("healing potion [" + numPotions + "]");
    }
    return optionsList.toArray(new String[0]);
  }

  private static void consumePotion(Client client) {
    client.hp += 5;
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

  private static void makeHitTask(Client client, Client victim, boolean isSuccess) {
    String clientPrefix = "\uD83D\uDDE1 ";
    String victimPrefix = "\uD83D\uDEE1 ";
    int clientHits = getDamageTask(client, isSuccess);
    victim.hp = Math.max(victim.hp - clientHits, 0);
    Messenger.send(victim.chatId, victimPrefix +
        PhraseGenerator.attackToVictim(client,
            victim,
            clientHits));
    Messenger.send(client.chatId,
        clientPrefix +
            PhraseGenerator.attackToOffender(client,
                victim,
                clientHits),
        addPotions(client, new String[] { TASK_SUCCESS }),
        true);
  }

  private static void handleHitTask(Client client, Client opponent, boolean isSuccess) {
    makeHitTask(client, opponent, isSuccess);
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

  private static void updateFightStats(Client winner, Client loser) {
    winner.fightsWon++;
    winner.totalFights++;
    loser.totalFights++;
    winner.status = Client.Status.IDLE;
    loser.status = Client.Status.IDLE;
    fightingChats.remove(winner.chatId);
    fightingChats.remove(loser.chatId);
  }

  private static void finishFight(Client winner, Client loser) {
    updateFightStats(winner, loser);
    int expGained = loser.expForKillingMe();
    winner.exp += expGained;
    sendToActiveUsers(PhraseGenerator.getWonPhrase(winner, loser));
    int winnerExpUntilPromo = winner.nextExp() - winner.exp;
    Messenger.send(winner.chatId, "You gained " + expGained + " experience, " +
        winnerExpUntilPromo + " experience left until level up.");
    String lost = "";
    if (loser.chatId > 0) {
      winner.giveItem(Game.Item.HPOTION);
      lost = loser.loseRandomItems();
      Messenger.send(winner.chatId, "You found 1 healing potion!");
    } else {
      // logic for looting bots is here
      int rnd = Utils.rndInRange(1, 6);
      if (rnd == 1) {
        winner.giveItem(Game.Item.HPOTION);
        Messenger.send(winner.chatId, "You found 1 healing potion!");
      } else if (rnd < 4) {
        Game.Item found = Game.ITEM_VALUES[Utils.getRndKeyWithWeight(
            loser.inventory)];
        winner.giveItem(found);
        Messenger.send(winner.chatId, "You found 1 " + found.singular + "!");
      }
    }
    if (winner.hp < winner.getMaxHp() && winner.chatId > 0) {
      Messenger.send(winner.chatId, "Du hast gewonnen! Deine Gesundheit wird sich in "
          + 3 * (winner.getMaxHp() - winner.hp) + " Sekunden regenerieren.", MAIN_BUTTONS);
      injuredChats.add(winner.chatId);
    } else {
      Messenger.send(winner.chatId, "Du hast gewonnen!", MAIN_BUTTONS);
    }
    levelUpIfNeeded(winner);
    if (loser.chatId < 0) {
      return;
    }
    handleLoserDefeat(loser, lost);
  }

  private static void handleLoserDefeat(Client loser, String lost) {
    String message;
    if (loser.hp < loser.getMaxHp()) {
      message = "Du wurdest im Kampf besiegt" + (lost.isEmpty() ? "." : ", und " + lost + " wurden gestohlen. ") +
          "Deine Gesundheit wird sich in " + 3 * (loser.getMaxHp() - loser.hp) +
          " Sekunden regenerieren.";
      Messenger.flush(loser.chatId);
      injuredChats.add(loser.chatId);
    } else {
      message = "Du wurdest im Kampf besiegt" + (lost.isEmpty() ? "." : ", und " + lost + " wurden gestohlen.");
    }

    Messenger.send(loser.chatId, message, MAIN_BUTTONS);
  }

  private static String getClientStats(Client client) {
    String result = "*" + client.username + "*\n"
        + "Level: " + client.level + "\n"
        + "Health: " + client.hp + " (out of " + client.getMaxHp() + ")\n"
        + "Damage: 1 - " + client.getMaxDamage() + "\n"
        + "Strength: " + client.strength + "\n"
        + "Vitality: " + client.vitality + "\n"
        + "Luck: " + client.luck;
    if (client.chatId > 0) {
      result += "\n"
          + "Experience: " + client.exp + " "
          + "(" + client.nextExp() + " needed to level up)\n"
          + "Fights won: " + client.fightsWon + " "
          + "(out of " + client.totalFights + ")\n";
      result += "Success today: " + client.getSuccessToday() + "\n";
    }
    return result;
  }

  private static int getDamageTask(Client client, boolean isSuccess) {
    int result = 1;
    int maxDamage = client.getMaxDamage();
    if (isSuccess) {
      result = Utils.rndInRange((maxDamage + 1) / 2, maxDamage);
    }
    if (Utils.rndInRange(1, 100) < client.luck * client.luck) {
      result *= 2;
    }
    return result;
  }

  private static void levelUpIfNeeded(Client client) {
    if (client.levelUpIfNeeded()) {
      Messenger.send(client.chatId, "You have achieved level " + client.level + "!\n",
          LEVEL_POINT_BUTTONS);
    }
  }

  private static void prepareToFight(Client client, Client opponent, int first) {
    client.status = Client.Status.FIGHTING;
    client.fightingChatId = opponent.chatId;
    client.lastFightActivitySince = curTimeSeconds;
    readyToFightChats.remove(client.chatId);
    fightingChats.add(client.chatId);
    if (first == 0) {
      prepareToFight(opponent, client, 1);
    }
  }

  static void prepareToFight(Client client, Client opponent) {
    prepareToFight(client, opponent, 0);
  }
}
