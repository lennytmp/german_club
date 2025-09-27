package FightLang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GameEngine {
    private final StorageInterface storage;
    private final TelegramInterface telegram;
    
    private static final String[] MAIN_BUTTONS = { "Kämpfen", "Profil", "Aufgabe" };
    private static final String[] LEVEL_POINT_BUTTONS = {
        "Stärke verbessern", "Vitalität verbessern", "Glück verbessern"
    };
    private static final int CHAT_TIMEOUT = 600;
    private static final int FIGHT_TIMEOUT = 60;
    private static final int REGEN_INTERVAL_SECONDS = 9;
    private static final String TASK_FAIL = "Fehler";
    private static final String TASK_SUCCESS = "Erfolg";
    private static final String GAME_DESCRIPTION_PROMPT = "Du bist ein Rollenspiel. In diesem Spiel gibt es die folgenden Monster: " +
      "Bettler, Betrunkener, Crack-Süchtiger, Skelett, Zombie, Geist, Mumie, Lich, Vampir, Ghul, Nekromant, Teufel und Dämon. " +
      "Die Gegenstände, die Helden finden können, sind Gold, Silber, Asche, Papier, Klaue, Fangzahn, Wachs, Bandage, Seelenstein, " +
      "Fleisch, Knochen, Crack, Flasche, Münze.";
    private static final String NOT_FOUND_PROMPT = "In diesem Spiel suchte der Held nach etwas Nützlichem. " +
        "Du musst etwas sagen wie: \"Du hast versucht, etwas Nützliches zu finden, aber du hast nichts gefunden.\" Du kannst dir einen " +
        "Grund ausdenken, wo ich gesucht habe oder warum ich nichts gefunden habe. Halte dich kurz und erwähne nicht, wonach ich gesucht habe.";
    private static final String SOMETHING_FOUND_PROMPT = "Der Held hat gerade %s gefunden. Du musst es kurz beschreiben. " +
      "Zum Beispiel: \"Du bist durch den Wald spaziert und hast eine alte Feuerstelle gefunden, in der ein riesiges Skelett lag. " + 
      "Nach genauer Untersuchung hast du die Krallen herausgezogen und in deinen Rucksack gesteckt.\" " + 
      "Gib am Ende keine Zusammenfassung, der Spieler sollte den Text sorgfältig lesen, um zu verstehen, was er gefunden hat. Stelle keine Fragen. " +
      "Gehe davon aus, dass der Held diesen Gegenstand am Ende in seinen Rucksack steckt.";
    
    private Set<Integer> activeChats = new HashSet<>();
    private Set<Integer> injuredChats = new HashSet<>();
    private Set<Integer> readyToFightChats = new HashSet<>();
    private Set<Integer> fightingChats = new HashSet<>();
    
    private int curTimeSeconds;
    
    public GameEngine(StorageInterface storage, TelegramInterface telegram) {
        this.storage = storage;
        this.telegram = telegram;
        updateCurTime();
        initializeClientSets();
    }
    
    // Helper method to get client and ensure storage dependency is set
    private Client getClientWithStorage(int chatId) {
        Client client = storage.getClientByChatId(chatId);
        if (client != null) {
            client.setStorage(storage);
        }
        return client;
    }
    
    private void initializeClientSets() {
        storage.forEachClient(new ClientDo() {
            public void run(Client client) {
                if (client == null) {
                    return; // this shouldn't happen
                }
                // Ensure client has storage dependency set
                client.setStorage(storage);
                
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
    
    public void processUpdate(Telegram.Update upd) {
        updateCurTime();
        int chatId = upd.message.chat.id;
        Client client = getClientWithStorage(chatId);
        boolean newClient = client == null;
        if (newClient) {
            String username;
            if (upd.message.from.username != null) {
                username = upd.message.from.username;
            } else {
                username = upd.message.from.first_name;
            }
            client = new Client(chatId, username);
            client.setStorage(storage);
        } else {
            // Ensure existing client has storage dependency set
            client.setStorage(storage);
        }
        client.lastActivity = curTimeSeconds;
        activeChats.add(chatId);
        storage.saveClient(client);

        if (newClient) {
            telegram.sendMessage(client.chatId, "Willkommen im German Club!", MAIN_BUTTONS);
            try {
                sendToActiveUsers(PhraseGenerator.getJoinedTheFightClub(client.username));
            } catch (Exception e) {
                // Ignore phrase generation errors in test environment
            }
        }

        String txt = upd.message.text;

        if (txt.equals("/start")) {
            sendStartMessage(client);
            return;
        }

        // Handle trade responses first - must come before other commands
        if (client.status == Client.Status.TRADING) {
            if (txt.equals("Angebot annehmen")) {
                handleTradeAccept(client);
                return;
            }
            if (txt.equals("Angebot ablehnen")) {
                handleTradeReject(client);
                return;
            }
            // If player tries to use other commands while trading, resend the trade offer
            String tradeMessage = String.format(
                "\uD83D\uDCBC Der Händler wartet auf deine Antwort!\n\n" +
                "\"Ich biete dir 1 %s für deine %s. Was sagst du?\"\n\n" +
                "Du musst zuerst auf das Handelsangebot antworten, bevor du etwas anderes tun kannst.",
                client.requestedItem.singular,
                client.offeredItem.singular
            );
            telegram.sendMessage(client.chatId, tradeMessage, new String[] { "Angebot annehmen", "Angebot ablehnen" });
            return;
        }

        if (txt.equals("Profil") || txt.equals("/profil")) {
            showProfile(client);
            return;
        }

        if (txt.startsWith("/username ")) {
            if (client.status != Client.Status.IDLE) {
                telegram.sendMessage(client.chatId, "Du kannst deinen Namen nur ändern, wenn du nicht kämpfst.");
                return;
            }
            String newName = txt.substring(10, txt.length());
            if (!newName.matches("[A-z0-9]*")) {
                telegram.sendMessage(client.chatId, "Falscher Name, bitte stelle sicher, dass er nur " +
                    "englische Buchstaben und Zahlen enthält.");
                return;
            }
            changeUserName(client, newName);
            return;
        }

        if (txt.startsWith("Stärke verbessern") || txt.startsWith("Vitalität verbessern") || txt.startsWith("Glück verbessern")) {
            String what = "";
            if (txt.startsWith("Stärke verbessern")) {
                what = "strength";
            } else if (txt.startsWith("Vitalität verbessern")) {
                what = "vitality";
            } else if (txt.startsWith("Glück verbessern")) {
                what = "luck";
            }
            if (client.levelPoints < 1) {
                telegram.sendMessage(client.chatId, "Du hast keine Stufenpunkte verfügbar. Du wirst welche haben, "
                    + "wenn du ein Level aufsteigst.", MAIN_BUTTONS);
                return;
            }
            improveSkill(client, what);
            return;
        }

        if (txt.equals("Kämpfen") || txt.equals("/kämpfen")) {
            if (client.status == Client.Status.FIGHTING) {
                telegram.sendMessage(client.chatId, "Du kämpfst bereits mit jemandem.");
                return;
            }
            if (client.status == Client.Status.READY_TO_FIGHT) {
                telegram.sendMessage(client.chatId, "Du suchst bereits nach einem Opfer.");
                return;
            }
            if (readyToFightChats.size() == 0) {
                setReadyToFight(client);
            } else {
                int opponentChatId = readyToFightChats.iterator().next();
                startFightReal(client, getClientWithStorage(opponentChatId));
            }
            return;
        }

        if (txt.equals("/heiltrank") || txt.startsWith("Heiltrank [")) {
            if (!client.hasItem(Game.Item.HPOTION)) {
                telegram.sendMessage(client.chatId, "Du hast keine Tränke.");
                return;
            }
            consumePotion(client);
            return;
        }

        if (txt.equals("/stärketrank") || txt.startsWith("Stärketrank [")) {
            if (!client.hasItem(Game.Item.SPOTION)) {
                telegram.sendMessage(client.chatId, "Du hast keine Stärketränke.");
                return;
            }
            consumeStrengthPotion(client);
            return;
        }

        if (txt.equals("/glückstrank") || txt.startsWith("Glückstrank [")) {
            if (!client.hasItem(Game.Item.LPOTION)) {
                telegram.sendMessage(client.chatId, "Du hast keine Glückstränke.");
                return;
            }
            consumeLuckPotion(client);
            return;
        }

        if (txt.equals("Aufgabe") && client.status != Client.Status.FIGHTING && client.status != Client.Status.TRADING) {
            client.incSuccessToday();
            storage.saveClient(client);
            if (!Utils.roll(30)) {
                // Nothing found: 25% chance to meet trader only if player has items
                if (Utils.roll(25) && client.hasAnyItems()) {
                    initiateTradeOffer(client);
                } else {
                    handleNothingFound(client);
                }
                return;
            }
            Game.Item found = Utils.getRnd(Game.ITEM_VALUES);
            client.giveItem(found);
            storage.saveClient(client);
            String foundMsg = "";
            try {
                foundMsg = Gemini.AskGemini(GAME_DESCRIPTION_PROMPT + " " + String.format(SOMETHING_FOUND_PROMPT, found.singular));
            } catch (Exception e) {
                // Ignore Gemini errors in test environment
            }
            if (foundMsg == "") {
                foundMsg = String.format("Du hast 1 %s gefunden!", found.singular);
            }
            telegram.sendMessage(client.chatId, foundMsg);
            return;
        }

        if (txt.equals("Heiltrank brauen") && client.status != Client.Status.FIGHTING) {
            handleBrewingCommand(client, "Heiltrank");
            return;
        }

        if (txt.equals("Stärketrank brauen") && client.status != Client.Status.FIGHTING) {
            handleBrewingCommand(client, "Stärketrank");
            return;
        }

        if (txt.equals("Glückstrank brauen") && client.status != Client.Status.FIGHTING) {
            handleBrewingCommand(client, "Glückstrank");
            return;
        }

        if (txt.equals("/retreat42")) {
            if (client.status != Client.Status.FIGHTING) {
                return;
            }
            Client opponent = getClientWithStorage(client.fightingChatId);
            telegram.sendMessage(client.chatId, "Rückzug42!");
            telegram.sendMessage(opponent.chatId, "Rückzug42!");
            finishFight(opponent, client);
            storage.saveClients(opponent, client);
            return;
        }

        if (txt.equals("/kill42")) {
            if (client.status != Client.Status.FIGHTING) {
                return;
            }
            Client opponent = getClientWithStorage(client.fightingChatId);
            telegram.sendMessage(client.chatId, "Töten42 aktiviert!");
            telegram.sendMessage(opponent.chatId, "Töten42 aktiviert!");
            finishFight(client, opponent);
            storage.saveClients(opponent, client);
            return;
        }

        if (txt.equals("/reset42")) {
            Client cleanClient = new Client(client.chatId, client.username);
            storage.saveClient(cleanClient);
            telegram.sendMessage(cleanClient.chatId, "Zurücksetzen42");
            return;
        }

        if (txt.equals("/version")) {
            telegram.sendMessage(client.chatId, "Version: 0.02");
            return;
        }

        if (client.status == Client.Status.FIGHTING &&
            (txt.equals(TASK_FAIL) || txt.equals(TASK_SUCCESS))) {
            boolean isSuccess = txt.equals(TASK_SUCCESS);
            if (isSuccess) {
                client.incSuccessToday();
            }
            client.lastFightActivitySince = curTimeSeconds;
            Client opponent = getClientWithStorage(client.fightingChatId);
            handleHitTask(client, opponent, isSuccess);
            storage.saveClients(opponent, client);
            if (opponent.chatId < 0 && opponent.status == Client.Status.FIGHTING) {
                activateBotTask(opponent, client);
            }
            return;
        }

        if (!txt.startsWith("/")) {
            String message = "\uD83D\uDCE2 " + client.username + ": " + txt;
            int numListeners = sendToActiveUsers(message) - 1;
            if (numListeners == 0) {
                telegram.sendMessage(client.chatId, "Du wurdest von niemandem gehört :(");
            }
            return;
        }

        // TODO: Add help page link here
        telegram.sendMessage(client.chatId, "Verwende die Schaltflächen unten, um gültige Aktionen auszuführen.");
    }
    
    public void runBackgroundTasks() {
        updateCurTime();
        cleanupDailySuccess();
        cleanupExpiredPotionEffects();
        restoreHpIfNeeded(storage.getClientsByChatIds(injuredChats));
        assignBotsIfTimeout(storage.getClientsByChatIds(readyToFightChats));
        Client[] fightingClients = storage.getClientsByChatIds(fightingChats);
        handleFightTimeouts(fightingClients);
    }
    
    private void cleanupDailySuccess() {
        // Only reset stats at 4am in GMT+0 (roughly)
        if (curTimeSeconds / 60 / 60 % 24 != 4) {
            return;
        }
        storage.forEachClient(new ClientDo() {
            public void run(Client client) {
                if (client == null) return;
                client.setStorage(storage); // Ensure storage dependency is set
                
                if (client.getLastDailyCleanup() + 24 * 60 * 60 < curTimeSeconds) {
                    client.setSuccessToday(0);
                    client.setLastDailyCleanup(curTimeSeconds);
                }
            }
        });
    }
    
    private void cleanupExpiredPotionEffects() {
        storage.forEachClient(new ClientDo() {
            public void run(Client client) {
                if (client == null) return;
                client.setStorage(storage); // Ensure storage dependency is set
                
                // Check which effects were active before cleanup
                boolean hadStrengthEffect = client.strengthPotionExpiry > 0 && client.strengthPotionExpiry > curTimeSeconds;
                boolean hadLuckEffect = client.luckPotionExpiry > 0 && client.luckPotionExpiry > curTimeSeconds;
                
                // Remove expired potion effects
                client.removeExpiredPotionEffects(curTimeSeconds);
                
                // Check which effects expired and send notifications
                boolean strengthExpired = hadStrengthEffect && (client.strengthPotionExpiry <= 0 || client.strengthPotionExpiry <= curTimeSeconds);
                boolean luckExpired = hadLuckEffect && (client.luckPotionExpiry <= 0 || client.luckPotionExpiry <= curTimeSeconds);
                
                if (strengthExpired || luckExpired) {
                    // Only send notification to real players (not bots)
                    if (client.chatId > 0) {
                        String message = "⏰ ";
                        if (strengthExpired && luckExpired) {
                            message += "Deine Stärke- und Glückstrank-Effekte sind abgelaufen. Deine Stärke ist jetzt " + 
                                client.strength + " und dein Glück ist " + client.luck + ".";
                        } else if (strengthExpired) {
                            message += "Dein Stärketrank-Effekt ist abgelaufen. Deine Stärke ist wieder " + client.strength + ".";
                        } else if (luckExpired) {
                            message += "Dein Glückstrank-Effekt ist abgelaufen. Dein Glück ist wieder " + client.luck + ".";
                        }
                        telegram.sendMessage(client.chatId, message);
                    }
                    storage.saveClient(client);
                }
            }
        });
    }
    
    private void assignBotsIfTimeout(Client[] clients) {
        for (Client client : clients) {
            if (client == null) continue;
            client.setStorage(storage); // Ensure storage dependency is set
            
            if (client.status != Client.Status.READY_TO_FIGHT
                || client.readyToFightSince > curTimeSeconds - 10) {
                return;
            }
            Client bot = new Client(-client.chatId, client);
            bot.setStorage(storage); // Set storage for bot too
            prepareToFight(client, bot);
            storage.saveClients(bot, client);

            telegram.sendMessage(client.chatId, "Du kämpfst jetzt mit " + bot.username + ".");
            telegram.sendMessage(client.chatId, getClientStats(bot));
            
            // Determine turn order and ask the first player to act
            boolean clientGoesFirst = determineTurnOrder(client, bot);
            if (clientGoesFirst) {
                askTaskStatus(client);
            } else {
                // Bot goes first - activate bot immediately
                activateBotTask(bot, client);
            }
        }
    }
    
    private void restoreHpIfNeeded(Client[] clients) {
        for (Client client : clients) {
            if (client == null) continue;
            client.setStorage(storage); // Ensure storage dependency is set
            
            if (client.status != Client.Status.IDLE
                || client.hp == client.getMaxHp()
                || client.lastRestore > curTimeSeconds - REGEN_INTERVAL_SECONDS) {
                continue;
            }
            client.hp++;
            client.lastRestore = curTimeSeconds;
            if (client.hp == client.getMaxHp()) {
                telegram.sendMessage(client.chatId, "Du bist jetzt vollständig erholt.");
                injuredChats.remove(client.chatId);
            }
            storage.saveClient(client);
        }
    }
    
    private void handleFightTimeouts(Client[] clients) {
        for (Client client : clients) {
            if (client == null) continue;
            client.setStorage(storage); // Ensure storage dependency is set
            
            if (client.status != Client.Status.FIGHTING
                || client.chatId < 0
                || client.lastFightActivitySince > curTimeSeconds - (FIGHT_TIMEOUT + 5)) {
                continue;
            }
            Client opponent = getClientWithStorage(client.fightingChatId);
            // Reset activity since we're handling the timeout
            client.lastFightActivitySince = curTimeSeconds;
            // Timeout acts the same as pressing "Fail" - handle as failed task
            handleHitTask(client, opponent, false);
            storage.saveClients(opponent, client);
            if (opponent.chatId < 0 && opponent.status == Client.Status.FIGHTING) {
                activateBotTask(opponent, client);
            }
        }
    }
    
    private void updateCurTime() {
        curTimeSeconds = (int) (System.currentTimeMillis() / 1000L);
    }
    
    // returns number of people who heard you
    private int sendToActiveUsers(String message) {
        return sendToActiveUsers(message, null);
    }
    
    // returns number of people who heard you, optionally excluding a specific chat ID
    private int sendToActiveUsers(String message, Integer excludeChatId) {
        // If changed - also change the other function with the same name.
        int numListeners = 0;
        List<Integer> passive = new LinkedList<>();
        for (int recepientChatId : activeChats) {
            // Skip the excluded chat ID if specified
            if (excludeChatId != null && recepientChatId == excludeChatId.intValue()) {
                continue;
            }
            
            Client recepient = getClientWithStorage(recepientChatId);
            if (recepient != null && recepient.lastActivity > curTimeSeconds - CHAT_TIMEOUT) {
                telegram.sendMessage(recepient.chatId, message);
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
    
    private void sendStartMessage(Client client) {
        StringBuilder startMessage = new StringBuilder();
        startMessage.append("🗡️ **Willkommen beim German Club!** ⚔️\n\n");
        startMessage.append("Du bist ein mutiger Held in einer magischen Welt voller Gefahren und Abenteuer!\n\n");
        startMessage.append("🎯 **Deine Mission:**\n");
        startMessage.append("• Kämpfe gegen andere Spieler und Monster\n");
        startMessage.append("• Sammle wertvolle Gegenstände und braue Tränke\n");
        startMessage.append("• Steige im Level auf und verbessere deine Fähigkeiten\n");
        startMessage.append("• Werde der mächtigste Kämpfer im German Club!\n\n");
        startMessage.append("⚡ **Erste Schritte:**\n");
        startMessage.append("• Nutze *Kämpfen* um gegen andere zu kämpfen\n");
        startMessage.append("• Verwende *Aufgabe* um Gegenstände zu finden\n");
        startMessage.append("• Schau dir dein *Profil* an um deine Statistiken zu sehen\n\n");
        startMessage.append("🎮 Nutze die Buttons unten, um zu beginnen!");
        
        telegram.sendMessage(client.chatId, startMessage.toString(), MAIN_BUTTONS);
    }
    
    private void showProfile(Client client) {
        StringBuilder profileMessage = new StringBuilder();
        
        // Add client stats
        profileMessage.append(getClientStats(client));
        
        // Add name change hint if not sent yet
        if (!client.nameChangeHintSent) {
            profileMessage.append("\n\nDu kannst deinen Namen mit folgendem Befehl ändern \n")
                       .append("`/username neuername`.");
            client.nameChangeHintSent = true;
            storage.saveClient(client);
        }
        
        // Add level points message if applicable
        if (client.levelPoints > 0) {
            profileMessage.append("\n\nDu hast ").append(client.levelPoints)
                       .append(" nicht zugewiesene Stufenpunkte.");
        }
        
        // Add inventory description
        String inventoryDesc = client.getInventoryDescription("\n");
        if (!inventoryDesc.isEmpty()) {
            profileMessage.append("\n\nDu hast:\n").append(inventoryDesc);
        } else {
            profileMessage.append("\n\nDu hast nichts.");
        }
        
        // Add brewing message if possible
        String[] brewableOptions = Game.getBrewableOptions(client.inventory);
        if (brewableOptions.length > 0) {
            profileMessage.append("\n\nDu kannst brauen:");
            for (String option : brewableOptions) {
                String potionName = option.replace(" brauen", "");
                profileMessage.append("\n- ").append(potionName);
            }
        }
        
        // Determine which buttons to show
        String[] buttons = MAIN_BUTTONS;
        
        if (client.levelPoints > 0 && brewableOptions.length > 0) {
            // Both level points and brewing available
            buttons = new String[LEVEL_POINT_BUTTONS.length + brewableOptions.length];
            System.arraycopy(LEVEL_POINT_BUTTONS, 0, buttons, 0, LEVEL_POINT_BUTTONS.length);
            System.arraycopy(brewableOptions, 0, buttons, LEVEL_POINT_BUTTONS.length, brewableOptions.length);
        } else if (client.levelPoints > 0) {
            // Only level points available
            buttons = LEVEL_POINT_BUTTONS;
        } else if (brewableOptions.length > 0) {
            // Only brewing available
            buttons = new String[MAIN_BUTTONS.length + brewableOptions.length];
            System.arraycopy(MAIN_BUTTONS, 0, buttons, 0, MAIN_BUTTONS.length);
            System.arraycopy(brewableOptions, 0, buttons, MAIN_BUTTONS.length, brewableOptions.length);
        }
        
        // Send the message
        telegram.sendMessage(client.chatId, profileMessage.toString(), buttons);
    }
    
    private void handleBrewingCommand(Client client, String potionType) {
        boolean canBrew = false;
        String successMessage = "";
        
        switch (potionType) {
            case "Heiltrank":
                canBrew = Game.canBrewPotion(client.inventory);
                if (canBrew) {
                    client.inventory = Game.brewPotion(client.inventory);
                    successMessage = "Nach viel Arbeit hast du einen neuen Heiltrank.";
                }
                break;
            case "Stärketrank":
                canBrew = Game.canBrewStrengthPotion(client.inventory);
                if (canBrew) {
                    client.inventory = Game.brewStrengthPotion(client.inventory);
                    successMessage = "Nach viel Arbeit hast du einen neuen Stärketrank.";
                }
                break;
            case "Glückstrank":
                canBrew = Game.canBrewLuckPotion(client.inventory);
                if (canBrew) {
                    client.inventory = Game.brewLuckPotion(client.inventory);
                    successMessage = "Nach viel Arbeit hast du einen neuen Glückstrank.";
                }
                break;
        }
        
        if (canBrew) {
            client.incSuccessToday();
            storage.saveClient(client);
            sendBrewingSuccessWithInventory(client, successMessage);
        }
    }

    private void sendBrewingSuccessWithInventory(Client client, String successMessage) {
        String inventoryDesc = client.getInventoryDescription("\n");
        StringBuilder combinedMessage = new StringBuilder(successMessage);
        
        if (!inventoryDesc.isEmpty()) {
            combinedMessage.append("\n\nDu hast jetzt:\n").append(inventoryDesc);
        } else {
            combinedMessage.append("\n\nDu hast nichts mehr.");
        }
        
        String[] brewableOptions = Game.getBrewableOptions(client.inventory);
        if (brewableOptions.length > 0) {
            String[] buttons = new String[MAIN_BUTTONS.length + brewableOptions.length];
            System.arraycopy(MAIN_BUTTONS, 0, buttons, 0, MAIN_BUTTONS.length);
            System.arraycopy(brewableOptions, 0, buttons, MAIN_BUTTONS.length, brewableOptions.length);
            
            combinedMessage.append("\n\nDu kannst brauen:");
            for (String option : brewableOptions) {
                String potionName = option.replace(" brauen", "");
                combinedMessage.append("\n- ").append(potionName);
            }
            
            telegram.sendMessage(client.chatId, combinedMessage.toString(), buttons);
        } else {
            telegram.sendMessage(client.chatId, combinedMessage.toString(), MAIN_BUTTONS);
        }
    }

    private void changeUserName(Client client, String newName) {
        client.username = newName;
        telegram.sendMessage(client.chatId, "Dein Name ist jetzt " + newName + ".");
        storage.saveClient(client);
    }

    private void activateBotTask(Client bot, Client opponent) {
        bot.lastFightActivitySince = curTimeSeconds;
        boolean isSuccess = Utils.roll(50);
        handleHitTask(bot, opponent, isSuccess);
        storage.saveClients(opponent, bot);
    }

    private void improveSkill(Client client, String skill) {
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
            telegram.sendMessage(client.chatId, "Weiß nicht, wie man " + skill + " verbessert.");
            return;
        }
        client.levelPoints--;
        telegram.sendMessage(client.chatId, "Du hast deine " + skill + " erhöht, sie ist jetzt "
            + newValue + ". Du hast " + client.levelPoints
            + " weitere Stufenpunkte.", MAIN_BUTTONS);
        storage.saveClient(client);
    }

    private void setReadyToFight(Client client) {
        // TODO: set ready to fight and save to index
        client.status = Client.Status.READY_TO_FIGHT;
        client.readyToFightSince = curTimeSeconds;
        storage.saveClient(client);
        readyToFightChats.add(client.chatId);
        try {
            sendToActiveUsers(PhraseGenerator.getReadyToFightPhrase(client));
        } catch (Exception e) {
            // Ignore phrase generation errors in test environment
        }
    }

    private void startFightReal(Client client, Client opponent) {
        prepareToFight(client, opponent);
        storage.saveClients(client, opponent);
        telegram.sendMessage(client.chatId, "Du kämpfst jetzt mit " + opponent.username + ".");
        telegram.sendMessage(opponent.chatId, "Du kämpfst jetzt mit " + client.username + ".");
        telegram.sendMessage(client.chatId, getClientStats(opponent));
        telegram.sendMessage(opponent.chatId, getClientStats(client));
        
        // Determine turn order and ask the first player to act
        boolean clientGoesFirst = determineTurnOrder(client, opponent);
        if (clientGoesFirst) {
            askTaskStatus(client);
        } else {
            askTaskStatus(opponent);
        }
    }

    private void askTaskStatus(Client client) {
        telegram.sendMessage(client.chatId, "Du bist an der Reihe!",
            addPotions(client, new String[] { TASK_SUCCESS }));
    }

    private String[] addPotions(Client client, String[] options) {
        storage.saveClient(client);
        List<String> optionsList = new ArrayList<>(Arrays.asList(options));
        
        int numHealingPotions = client.getItemNum(Game.Item.HPOTION);
        if (numHealingPotions > 0) {
            optionsList.add("Heiltrank [" + numHealingPotions + "]");
        }
        
        int numStrengthPotions = client.getItemNum(Game.Item.SPOTION);
        if (numStrengthPotions > 0) {
            optionsList.add("Stärketrank [" + numStrengthPotions + "]");
        }
        
        int numLuckPotions = client.getItemNum(Game.Item.LPOTION);
        if (numLuckPotions > 0) {
            optionsList.add("Glückstrank [" + numLuckPotions + "]");
        }
        
        return optionsList.toArray(new String[0]);
    }

    // Generic potion consumption method to avoid code duplication
    private void consumePotionGeneric(Client client, Game.Item potionType, String potionName, PotionEffect effect) {
        // Apply the specific effect of the potion
        effect.apply(client);
        
        // Remove the potion from inventory
        client.takeItem(potionType);
        storage.saveClient(client);

        // All potions use the same emoji
        String emoji = "\uD83C\uDF76"; // 🍶

        // Build client message - consistent format for all potions
        String clientMsg = emoji + " " + potionName + " konsumiert, du hast " +
            client.getItemNum(potionType) + " übrig.";
        
        // Add health info for healing potions, no effect note for others
        if (potionType == Game.Item.HPOTION) {
            clientMsg += " [" + client.hp + "/" + client.getMaxHp() + "]";
        } else {
            clientMsg += " (Noch keine Wirkung)";
        }

        // Send messages based on fighting status
        if (client.status == Client.Status.FIGHTING) {
            telegram.sendMessage(client.chatId, clientMsg, addPotions(client, new String[] { TASK_SUCCESS }));
            Client opponent = getClientWithStorage(client.fightingChatId);
            
            // Consistent format for opponent notification
            String opponentMsg = emoji + " " + client.username + " hat einen " + potionName + " konsumiert";
            if (potionType == Game.Item.HPOTION) {
                opponentMsg += " [" + client.hp + "/" + client.getMaxHp() + "]";
            } else {
                opponentMsg += ".";
            }
            telegram.sendMessage(opponent.chatId, opponentMsg);
        } else {
            telegram.sendMessage(client.chatId, clientMsg);
        }
    }

    // Functional interface for potion effects
    private interface PotionEffect {
        void apply(Client client);
    }

    private void consumePotion(Client client) {
        consumePotionGeneric(client, Game.Item.HPOTION, "Heiltrank", (c) -> {
            c.hp += 5;
            if (c.hp > c.getMaxHp()) {
                c.hp = c.getMaxHp();
            }
        });
    }

    private void consumeStrengthPotion(Client client) {
        // Apply the effect first
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, curTimeSeconds);
        
        // Remove the potion from inventory
        client.takeItem(Game.Item.SPOTION);
        storage.saveClient(client);

        // Build enhanced message with effect information
        String emoji = "\uD83C\uDF76"; // 🍶
        int remainingTime = client.getStrengthPotionRemainingTime(curTimeSeconds);
        String timeStr = Client.formatTimeRemaining(remainingTime);
        int remainingPotions = client.getItemNum(Game.Item.SPOTION);
        
        String clientMsg = emoji + " Stärketrank konsumiert! Deine Stärke ist jetzt " + 
            client.getEffectiveStrength() + " (läuft in " + timeStr + " ab). " +
            "Du hast " + remainingPotions + " übrig.";

        // Send messages based on fighting status
        if (client.status == Client.Status.FIGHTING) {
            telegram.sendMessage(client.chatId, clientMsg, addPotions(client, new String[] { TASK_SUCCESS }));
            Client opponent = getClientWithStorage(client.fightingChatId);
            
            String opponentMsg = emoji + " " + client.username + " hat einen Stärketrank konsumiert (Stärke: " + 
                client.getEffectiveStrength() + ").";
            telegram.sendMessage(opponent.chatId, opponentMsg);
        } else {
            telegram.sendMessage(client.chatId, clientMsg);
        }
    }

    private void consumeLuckPotion(Client client) {
        // Apply the effect first
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, curTimeSeconds);
        
        // Remove the potion from inventory
        client.takeItem(Game.Item.LPOTION);
        storage.saveClient(client);

        // Build enhanced message with effect information
        String emoji = "\uD83C\uDF76"; // 🍶
        int remainingTime = client.getLuckPotionRemainingTime(curTimeSeconds);
        String timeStr = Client.formatTimeRemaining(remainingTime);
        int remainingPotions = client.getItemNum(Game.Item.LPOTION);
        
        String clientMsg = emoji + " Glückstrank konsumiert! Dein Glück ist jetzt " + 
            client.getEffectiveLuck() + " (läuft in " + timeStr + " ab). " +
            "Du hast " + remainingPotions + " übrig.";

        // Send messages based on fighting status
        if (client.status == Client.Status.FIGHTING) {
            telegram.sendMessage(client.chatId, clientMsg, addPotions(client, new String[] { TASK_SUCCESS }));
            Client opponent = getClientWithStorage(client.fightingChatId);
            
            String opponentMsg = emoji + " " + client.username + " hat einen Glückstrank konsumiert (Glück: " + 
                client.getEffectiveLuck() + ").";
            telegram.sendMessage(opponent.chatId, opponentMsg);
        } else {
            telegram.sendMessage(client.chatId, clientMsg);
        }
    }

    private void makeHitTask(Client client, Client victim, boolean isSuccess) {
        String clientPrefix = "\uD83D\uDDE1 ";
        String victimPrefix = "\uD83D\uDEE1 ";
        int clientHits = getDamageTask(client, isSuccess);
        victim.hp = Math.max(victim.hp - clientHits, 0);
        
        // Send damage message to victim with response buttons (if still fighting)
        if (victim.status == Client.Status.FIGHTING && victim.hp > 0) {
            try {
                telegram.sendMessage(victim.chatId, victimPrefix +
                    PhraseGenerator.attackToVictim(client,
                        victim,
                        clientHits),
                    addPotions(victim, new String[] { TASK_SUCCESS }));
            } catch (Exception e) {
                telegram.sendMessage(victim.chatId, victimPrefix + "Du wurdest angegriffen! -" + clientHits + " [" + victim.hp + "/" + victim.getMaxHp() + "]",
                    addPotions(victim, new String[] { TASK_SUCCESS }));
            }
        } else {
            // If victim is dead or not fighting, just send the damage message
            try {
                telegram.sendMessage(victim.chatId, victimPrefix +
                    PhraseGenerator.attackToVictim(client,
                        victim,
                        clientHits));
            } catch (Exception e) {
                telegram.sendMessage(victim.chatId, victimPrefix + "Du wurdest angegriffen! -" + clientHits + " [" + victim.hp + "/" + victim.getMaxHp() + "]");
            }
        }
        
        // Send confirmation to attacker (no buttons needed - they just acted)
        try {
            telegram.sendMessage(client.chatId,
                clientPrefix +
                    PhraseGenerator.attackToOffender(client,
                        victim,
                        clientHits));
        } catch (Exception e) {
            telegram.sendMessage(client.chatId, clientPrefix + "Du hast angegriffen! -" + clientHits + " [" + victim.hp + "/" + victim.getMaxHp() + "]");
        }
    }

    private void handleHitTask(Client client, Client opponent, boolean isSuccess) {
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

    private void updateFightStats(Client winner, Client loser) {
        winner.fightsWon++;
        winner.totalFights++;
        loser.totalFights++;
        winner.status = Client.Status.IDLE;
        loser.status = Client.Status.IDLE;
        fightingChats.remove(winner.chatId);
        fightingChats.remove(loser.chatId);
    }

    private void finishFight(Client winner, Client loser) {
        updateFightStats(winner, loser);
        int expGained = loser.expForKillingMe();
        winner.exp += expGained;
        
        // Send victory announcement to all active users except the winner
        try {
            sendToActiveUsers(PhraseGenerator.getWonPhrase(winner, loser), winner.chatId);
        } catch (Exception e) {
            // Ignore phrase generation errors in test environment
        }
        
        // Build comprehensive victory message for the winner
        StringBuilder victoryMessage = new StringBuilder();
        try {
            victoryMessage.append(PhraseGenerator.getWonPhrase(winner, loser));
        } catch (Exception e) {
            // Fallback to simple message in case of phrase generation errors
            victoryMessage.append("Du hast gewonnen!");
        }
        
        // Add experience information
        int winnerExpUntilPromo = winner.nextExp() - winner.exp;
        victoryMessage.append("\n\n🎯 Du hast ").append(expGained).append(" Erfahrung erhalten, ")
                     .append(winnerExpUntilPromo).append(" Erfahrung fehlt bis zum Levelaufstieg.");
        
        // Handle item findings
        String lost = "";
        boolean foundItems = false;
        if (loser.chatId > 0) {
            winner.giveItem(Game.Item.HPOTION);
            lost = loser.loseRandomItems();
            victoryMessage.append("\n\n🎒 Du hast 1 Heiltrank gefunden!");
            foundItems = true;
        } else {
            // logic for looting bots is here
            int rnd = Utils.rndInRange(1, 6);
            if (rnd == 1) {
                winner.giveItem(Game.Item.HPOTION);
                victoryMessage.append("\n\n🎒 Du hast 1 Heiltrank gefunden!");
                foundItems = true;
            } else if (rnd < 4) {
                Game.Item found = Game.ITEM_VALUES[Utils.getRndKeyWithWeight(
                    loser.inventory)];
                winner.giveItem(found);
                victoryMessage.append("\n\n🎒 Du hast 1 ").append(found.singular).append(" gefunden!");
                foundItems = true;
            }
        }
        
        // Add health regeneration information if needed
        if (winner.hp < winner.getMaxHp() && winner.chatId > 0) {
            victoryMessage.append("\n\n❤️ Deine Gesundheit wird sich in ")
                         .append(REGEN_INTERVAL_SECONDS * (winner.getMaxHp() - winner.hp))
                         .append(" Sekunden regenerieren.");
            injuredChats.add(winner.chatId);
        }
        
        // Check for level up and add to message if applicable
        boolean leveledUp = winner.levelUpIfNeeded();
        if (leveledUp) {
            victoryMessage.append("\n\n🎉 Du hast Level ").append(winner.level).append(" erreicht!");
        }
        
        // Send the comprehensive victory message
        if (leveledUp) {
            telegram.sendMessage(winner.chatId, victoryMessage.toString(), LEVEL_POINT_BUTTONS);
        } else {
            telegram.sendMessage(winner.chatId, victoryMessage.toString(), MAIN_BUTTONS);
        }
        if (loser.chatId < 0) {
            return;
        }
        handleLoserDefeat(loser, lost);
    }

    private void handleLoserDefeat(Client loser, String lost) {
        String message;
        if (loser.hp < loser.getMaxHp()) {
            message = "Du wurdest im Kampf besiegt" + (lost.isEmpty() ? ". " : ", und " + lost + " wurden gestohlen. ") +
                "Deine Gesundheit wird sich in " + REGEN_INTERVAL_SECONDS * (loser.getMaxHp() - loser.hp) +
                " Sekunden regenerieren.";
            injuredChats.add(loser.chatId);
        } else {
            message = "Du wurdest im Kampf besiegt" + (lost.isEmpty() ? "." : ", und " + lost + " wurden gestohlen.");
        }

        telegram.sendMessage(loser.chatId, message, MAIN_BUTTONS);
    }

    private String getClientStats(Client client) {
        String result = "*" + client.username + "*\n"
            + "Level: " + client.level + "\n"
            + "Gesundheit: " + client.hp + " (von " + client.getMaxHp() + ")\n"
            + "Schaden: 1 - " + client.getMaxDamage() + "\n";
        
        // Show strength with potion effects if any
        if (client.getEffectiveStrength() != client.strength) {
            int remainingTime = client.getStrengthPotionRemainingTime(curTimeSeconds);
            String timeStr = Client.formatTimeRemaining(remainingTime);
            result += "Stärke: " + client.strength + " (+" + (client.getEffectiveStrength() - client.strength) + " = " + client.getEffectiveStrength() + " für " + timeStr + ")\n";
        } else {
            result += "Stärke: " + client.strength + "\n";
        }
        
        result += "Vitalität: " + client.vitality + "\n";
        
        // Show luck with potion effects if any
        if (client.getEffectiveLuck() != client.luck) {
            int remainingTime = client.getLuckPotionRemainingTime(curTimeSeconds);
            String timeStr = Client.formatTimeRemaining(remainingTime);
            result += "Glück: " + client.luck + " (+" + (client.getEffectiveLuck() - client.luck) + " = " + client.getEffectiveLuck() + " für " + timeStr + ")";
        } else {
            result += "Glück: " + client.luck;
        }
        
        if (client.chatId > 0) {
            result += "\n"
                + "Erfahrung: " + client.exp + " "
                + "(" + client.nextExp() + " benötigt für Levelaufstieg)\n"
                + "Kämpfe gewonnen: " + client.fightsWon + " "
                + "(von " + client.totalFights + ")\n";
            result += "Erfolg heute: " + client.getSuccessToday() + "\n";
        }
        return result;
    }

    private int getDamageTask(Client client, boolean isSuccess) {
        int result = 1;
        int maxDamage = client.getMaxDamage();
        if (isSuccess) {
            result = Utils.rndInRange((maxDamage + 1) / 2, maxDamage);
        }
        if (Utils.rndInRange(1, 100) < client.getEffectiveLuck() * client.getEffectiveLuck()) {
            result *= 2;
        }
        return result;
    }

    private void levelUpIfNeeded(Client client) {
        if (client.levelUpIfNeeded()) {
            telegram.sendMessage(client.chatId, "Du hast Level " + client.level + " erreicht!\n",
                LEVEL_POINT_BUTTONS);
        }
    }

    private void setupClientForFight(Client client, Client opponent) {
        client.status = Client.Status.FIGHTING;
        client.fightingChatId = opponent.chatId;
        client.lastFightActivitySince = curTimeSeconds;
        readyToFightChats.remove(client.chatId);
        fightingChats.add(client.chatId);
    }

    // Determines who goes first based on luck values
    // Returns true if client goes first, false if opponent goes first
    private boolean determineTurnOrder(Client client, Client opponent) {
        int totalLuck = client.getEffectiveLuck() + opponent.getEffectiveLuck();
        int randomValue = Utils.rndInRange(1, totalLuck);
        
        // If random value is <= opponent's luck, opponent goes first
        // Otherwise, client goes first
        return randomValue > opponent.getEffectiveLuck();
    }

    public void prepareToFight(Client client, Client opponent) {
        // Set both clients to FIGHTING status regardless of turn order
        setupClientForFight(client, opponent);
        setupClientForFight(opponent, client);
    }

    // Trading system methods
    private Game.Item getRandomTradeItem() {
        // Trader can offer any item in the game
        return Utils.getRnd(Game.ITEM_VALUES);
    }

    private void initiateTradeOffer(Client client) {
        // Generate trade offer
        Game.Item randomTradeItem = getRandomTradeItem();
        if (!client.generateTradeOffer(randomTradeItem)) {
            // This should never happen with the current logic, but fallback to nothing found
            handleNothingFound(client);
            return;
        }
        
        storage.saveClient(client);
        
        String tradeMessage = String.format(
            "\uD83D\uDCBC Ein geheimnisvoller Händler erscheint vor dir!\n\n" +
            "\"Ich biete dir 1 %s für deine %s. Was sagst du?\"\n\n" +
            "Du kannst das Angebot annehmen oder ablehnen.",
            client.requestedItem.singular,
            client.offeredItem.singular
        );
        
        telegram.sendMessage(client.chatId, tradeMessage, new String[] { "Angebot annehmen", "Angebot ablehnen" });
    }

    private void handleNothingFound(Client client) {
        // Nothing found: with 50% chance send wiseman message, otherwise send the generated message
        boolean sendWisdom = Utils.roll(50);
        if (sendWisdom) {
            try {
                telegram.sendMessage(client.chatId, PhraseGenerator.getWisdom(client));
            } catch (Exception e) {
                telegram.sendMessage(client.chatId, "Du hast einen Spaziergang im Wald gemacht, aber nichts Nützliches gefunden.");
            }
        } else {
            String notFound = "";
            try {
                notFound = Gemini.AskGemini(GAME_DESCRIPTION_PROMPT + " " + NOT_FOUND_PROMPT);
            } catch (Exception e) {
                // Ignore Gemini errors in test environment
            }
            if (notFound == "") {
                notFound = "Du hast einen Spaziergang im Wald gemacht, aber nichts Nützliches gefunden.";
            }
            telegram.sendMessage(client.chatId, notFound);
        }
    }

    private void handleTradeAccept(Client client) {
        if (client.status != Client.Status.TRADING || client.offeredItem == null || client.requestedItem == null) {
            return;
        }

        // Store items for message before executing trade
        Game.Item offeredItem = client.offeredItem;
        Game.Item requestedItem = client.requestedItem;

        if (!client.executeTrade()) {
            telegram.sendMessage(client.chatId, "Du hast das angebotene Item nicht mehr!", MAIN_BUTTONS);
            client.resetTradeState();
            return;
        }
        
        String successMessage = String.format(
            "\uD83E\uDD1D Handel erfolgreich!\n\n" +
            "Du hast deine %s gegen 1 %s getauscht.\n\n" +
            "\"Danke für das Geschäft!\" sagt der Händler und verschwindet.",
            offeredItem.singular,
            requestedItem.singular
        );
        
        telegram.sendMessage(client.chatId, successMessage, MAIN_BUTTONS);
        client.resetTradeState();
    }

    private void handleTradeReject(Client client) {
        if (client.status != Client.Status.TRADING) {
            return;
        }

        telegram.sendMessage(client.chatId, 
            "\uD83D\uDE45 Du lehnst das Angebot ab.\n\n" +
            "\"Schade...\" murmelt der Händler und verschwindet in den Schatten.",
            MAIN_BUTTONS);
        client.resetTradeState();
    }
}