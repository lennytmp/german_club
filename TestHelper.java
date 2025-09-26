package FightLang;

import java.util.HashMap;
import java.util.Map;

/**
 * Common test helper utilities to reduce code duplication across test files.
 * 
 * This class provides:
 * - Standardized assertion methods that follow the "S" output format rule
 * - Common mock setup patterns
 * - Player creation and fight setup utilities
 * - Test validation helpers
 * 
 * Usage: Import static methods to use in test files.
 */
public class TestHelper {
    
    // ========== ASSERTION METHODS ==========
    
    /**
     * Assert that two integers are equal. Prints "S" on success, descriptive error on failure.
     */
    public static boolean assertEquals(int expected, int actual, String testName) {
        if (expected == actual) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected " + expected + " but got " + actual);
        return false;
    }
    
    /**
     * Assert that two booleans are equal. Prints "S" on success, descriptive error on failure.
     */
    public static boolean assertEquals(boolean expected, boolean actual, String testName) {
        if (expected == actual) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected " + expected + " but got " + actual);
        return false;
    }
    
    /**
     * Assert that two strings are equal. Prints "S" on success, descriptive error on failure.
     */
    public static boolean assertEquals(String expected, String actual, String testName) {
        if (expected.equals(actual)) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected \"" + expected + "\" but got \"" + actual + "\"");
        return false;
    }
    
    /**
     * Assert that a condition is true. Prints "S" on success, descriptive error on failure.
     */
    public static boolean assertTrue(boolean condition, String testName) {
        if (condition) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED");
        return false;
    }
    
    /**
     * Assert that a condition is false. Prints "S" on success, descriptive error on failure.
     */
    public static boolean assertFalse(boolean condition, String testName) {
        if (!condition) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED");
        return false;
    }
    
    // ========== MOCK SETUP UTILITIES ==========
    
    /**
     * Test environment containing all necessary mock objects for testing.
     */
    public static class TestEnvironment {
        public final MockStorage storage;
        public final MockTelegram telegram;
        public final GameEngine engine;
        
        public TestEnvironment() {
            this.storage = new MockStorage();
            this.telegram = new MockTelegram();
            this.engine = new GameEngine(storage, telegram);
        }
        
        /**
         * Clear all telegram messages to start fresh for next test.
         */
        public void clearMessages() {
            telegram.clearMessages();
        }
    }
    
    /**
     * Create a standardized test environment with all mocks initialized.
     */
    public static TestEnvironment createTestEnvironment() {
        return new TestEnvironment();
    }
    
    // ========== PLAYER CREATION UTILITIES ==========
    
    /**
     * Create a new player by simulating the /start command.
     * Returns the created client for further manipulation.
     */
    public static Client createPlayer(TestEnvironment env, int chatId, String playerName) {
        env.telegram.simulateUserMessage(chatId, playerName, "/start");
        Telegram.Update[] updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }
        return env.storage.getClientByChatId(chatId);
    }
    
    /**
     * Create a player with specific items in their inventory.
     */
    public static Client createPlayerWithItems(TestEnvironment env, int chatId, String playerName, Game.Item... items) {
        Client player = createPlayer(env, chatId, playerName);
        if (player != null) {
            for (Game.Item item : items) {
                player.giveItem(item);
            }
            env.storage.saveClient(player);
        }
        return player;
    }
    
    /**
     * Create a player with specific stats (level, experience, etc.).
     */
    public static Client createPlayerWithStats(TestEnvironment env, int chatId, String playerName, int level, int exp) {
        Client player = createPlayer(env, chatId, playerName);
        if (player != null) {
            player.level = level;
            player.exp = exp;
            env.storage.saveClient(player);
        }
        return player;
    }
    
    // ========== FIGHT SETUP UTILITIES ==========
    
    /**
     * Setup a fight between two players and return who has the first turn.
     * Returns the chat ID of the player who goes first, or -1 if setup failed.
     */
    public static int setupFight(TestEnvironment env, int player1ChatId, String player1Name, int player2ChatId, String player2Name) {
        // Both players look for fight
        env.telegram.simulateUserMessage(player1ChatId, player1Name, "Kämpfen");
        Telegram.Update[] updates1 = env.telegram.getUpdates(1);
        if (updates1.length > 0) {
            env.engine.processUpdate(updates1[updates1.length - 1]);
        }
        
        env.telegram.simulateUserMessage(player2ChatId, player2Name, "Kämpfen");
        Telegram.Update[] updates2 = env.telegram.getUpdates(1);
        if (updates2.length > 0) {
            env.engine.processUpdate(updates2[updates2.length - 1]);
        }
        
        // Determine who has the turn
        boolean player1HasTurn = env.telegram.hasMessageForChatContaining(player1ChatId, "Du bist an der Reihe!");
        boolean player2HasTurn = env.telegram.hasMessageForChatContaining(player2ChatId, "Du bist an der Reihe!");
        
        if (player1HasTurn && !player2HasTurn) {
            return player1ChatId;
        } else if (player2HasTurn && !player1HasTurn) {
            return player2ChatId;
        } else {
            return -1; // Setup failed
        }
    }
    
    /**
     * Simulate a successful attack by a player.
     */
    public static void simulateAttack(TestEnvironment env, int attackerChatId, String attackerName) {
        env.telegram.simulateUserMessage(attackerChatId, attackerName, "Erfolg");
        Telegram.Update[] updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }
    }
    
    /**
     * Simulate using a potion during a fight.
     */
    public static void usePotion(TestEnvironment env, int playerChatId, String playerName, String potionButton) {
        env.telegram.simulateUserMessage(playerChatId, playerName, potionButton);
        Telegram.Update[] updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }
    }
    
    // ========== VALIDATION UTILITIES ==========
    
    /**
     * Check if string array contains specific value.
     */
    public static boolean arrayContains(String[] array, String value) {
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if message contains specific text.
     */
    public static boolean messageContains(String message, String text) {
        return message != null && message.contains(text);
    }
    
    /**
     * Validate that a fight was properly initiated between two players.
     */
    public static boolean validateFightInitiation(TestEnvironment env, int player1ChatId, String player1Name, int player2ChatId, String player2Name) {
        boolean player1Notified = env.telegram.hasMessageForChatContaining(player1ChatId, "Du kämpfst jetzt mit " + player2Name);
        boolean player2Notified = env.telegram.hasMessageForChatContaining(player2ChatId, "Du kämpfst jetzt mit " + player1Name);
        return player1Notified && player2Notified;
    }
    
    /**
     * Validate that exactly one of two players has the turn.
     */
    public static boolean validateTurnSystem(TestEnvironment env, int player1ChatId, int player2ChatId) {
        boolean player1HasTurn = env.telegram.hasMessageForChatContaining(player1ChatId, "Du bist an der Reihe!");
        boolean player2HasTurn = env.telegram.hasMessageForChatContaining(player2ChatId, "Du bist an der Reihe!");
        return (player1HasTurn || player2HasTurn) && !(player1HasTurn && player2HasTurn);
    }
    
    // ========== INVENTORY UTILITIES ==========
    
    /**
     * Create an inventory map with specific items for testing potion brewing.
     */
    public static Map<Integer, Integer> createInventoryWithItems(Game.Item... items) {
        Map<Integer, Integer> inventory = new HashMap<>();
        for (Game.Item item : items) {
            inventory.put(item.ordinal(), inventory.getOrDefault(item.ordinal(), 0) + 1);
        }
        return inventory;
    }
    
    /**
     * Create inventory with healing potion ingredients.
     */
    public static Map<Integer, Integer> createHealingPotionIngredients() {
        return createInventoryWithItems(Game.Item.ASH, Game.Item.BANDAGE, Game.Item.BOTTLE);
    }
    
    /**
     * Create inventory with strength potion ingredients.
     */
    public static Map<Integer, Integer> createStrengthPotionIngredients() {
        return createInventoryWithItems(Game.Item.BONE, Game.Item.FLESH, Game.Item.FANG);
    }
    
    /**
     * Create inventory with luck potion ingredients.
     */
    public static Map<Integer, Integer> createLuckPotionIngredients() {
        return createInventoryWithItems(Game.Item.COIN, Game.Item.GOLD, Game.Item.SILVER);
    }
    
    // ========== PROFILE TESTING UTILITIES ==========
    
    /**
     * Request and get a player's profile message.
     */
    public static MockTelegram.SentMessage getPlayerProfile(TestEnvironment env, int chatId, String playerName) {
        env.clearMessages();
        env.telegram.simulateUserMessage(chatId, playerName, "Profil");
        Telegram.Update[] updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }
        return env.telegram.getLastMessageForChat(chatId);
    }
    
    /**
     * Validate basic profile information is present.
     */
    public static boolean validateBasicProfile(MockTelegram.SentMessage profileMsg, String playerName) {
        if (profileMsg == null) return false;
        return profileMsg.message.contains(playerName) && 
               profileMsg.message.contains("Level:") && 
               profileMsg.message.contains("Gesundheit:");
    }
    
    // ========== JSON VALIDATION UTILITIES ==========
    
    /**
     * Validates that a JSON string can be parsed back without errors.
     * Useful for testing JSON generation in Gemini API calls.
     */
    public static boolean isValidJson(String jsonString) {
        try {
            com.google.gson.JsonParser.parseString(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}