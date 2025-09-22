package FightLang;

import java.lang.InterruptedException;
import java.lang.Thread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Main {
  public static boolean isProd = false;
  private static GameEngine gameEngine;

  public static void main(String[] args)
      throws InterruptedException, Exception {
    initialize(args);
    System.out.println("German Club Server started...");
    while (true) {
      try {
        StorageInterface storage = new Storage();
        TelegramInterface telegram = new TelegramService();
        int maxUpdateId = storage.getMaxUpdateId();
        // TODO: download updates async and put to queue
        Telegram.Update[] updates = telegram.getUpdates(maxUpdateId + 1);
        Arrays.sort(updates, new Comparator<Telegram.Update>() {
          public int compare(Telegram.Update u1, Telegram.Update u2) {
            return u1.update_id - u2.update_id;
          }
        });
        // Handle user commands
        for (Telegram.Update upd : updates) {
          storage.saveMaxUpdateId(upd.update_id);
          if (upd.message != null && upd.message.text != null) {
            gameEngine.processUpdate(upd);
          }
        }
        // Background/async operations for each client
        Messenger.sendSheduledMessages();
        gameEngine.runBackgroundTasks();
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

    // Initialize the game engine
    StorageInterface storage = new Storage();
    TelegramInterface telegram = new TelegramService();
    gameEngine = new GameEngine(storage, telegram);
  }

  // Testable class to hold profile display data - kept for backward compatibility with tests
  static class ProfileDisplay {
    String message;
    String[] buttons;
    
    ProfileDisplay(String message, String[] buttons) {
      this.message = message;
      this.buttons = buttons;
    }
  }
  
  // Testable method that builds profile display without sending messages - kept for backward compatibility with tests
  static ProfileDisplay buildProfileDisplay(Client client) {
    GameEngine.ProfileDisplay engineDisplay = GameEngine.buildProfileDisplay(client);
    return new ProfileDisplay(engineDisplay.message, engineDisplay.buttons);
  }

  // Kept for backward compatibility with tests
  static void prepareToFight(Client client, Client opponent) {
    // This method is now handled by GameEngine, but kept for test compatibility
    // We'll create a temporary GameEngine just for this test method
    StorageInterface storage = new MockStorage();
    TelegramInterface telegram = new MockTelegram();
    
    // Ensure clients have storage dependency set
    client.setStorage(storage);
    opponent.setStorage(storage);
    
    GameEngine tempEngine = new GameEngine(storage, telegram);
    tempEngine.prepareToFight(client, opponent);
  }
}