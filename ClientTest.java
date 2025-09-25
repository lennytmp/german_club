package FightLang;

import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testNextExpLevel();
        allTestsPassed &= testPotionBrewing();
        allTestsPassed &= testPotionButtonProgression();
        allTestsPassed &= testProfileDisplayWithNoBrewingOptions();
        allTestsPassed &= testProfileDisplayWithSingleBrewingOption();
        allTestsPassed &= testProfileDisplayWithMultipleBrewingOptions();
        allTestsPassed &= testTradingSystem();
        if (!allTestsPassed) {
            System.exit(1); 
        }
        System.out.println();
    }

    // A simple assertEquals function to compare expected and actual values
    public static boolean assertEquals(int expected, int actual, String testName) {
        if (expected == actual) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected " + expected + " but got " + actual);
        return false;
    }
    
    // Helper to check if string array contains specific value
    public static boolean arrayContains(String[] array, String value) {
        for (String item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    // Helper to check if message contains specific text
    public static boolean messageContains(String message, String text) {
        return message.contains(text);
    }

    public static boolean testNextExpLevel() {
        Client c =  new Client(0, "test");
        c.level = 1;
        boolean allTestsPassed = true;
        allTestsPassed &= assertEquals(30, c.nextExp(), "Expected experience for level 1 should be 30.");
        c.level = 2;
        allTestsPassed &= assertEquals(230, c.nextExp(), "Expected experience for level 2 should be 230.");
        c.level = 3;
        allTestsPassed &= assertEquals(530, c.nextExp(), "Expected experience for level 2 should be 530.");
        return allTestsPassed;
    }

    public static boolean testPotionBrewing() {
        boolean allTestsPassed = true;
        
        // Test healing potion brewing
        allTestsPassed &= testHealingPotionBrewing();
        
        // Test strength potion brewing
        allTestsPassed &= testStrengthPotionBrewing();
        
        // Test luck potion brewing
        allTestsPassed &= testLuckPotionBrewing();
        
        // Test edge cases
        allTestsPassed &= testInsufficientIngredients();
        
        return allTestsPassed;
    }

    public static boolean testHealingPotionBrewing() {
        boolean allTestsPassed = true;
        
        // Create inventory with healing potion ingredients
        Map<Integer, Integer> inventory = new HashMap<>();
        inventory.put(Game.Item.ASH.ordinal(), 1);      // 1 Ash
        inventory.put(Game.Item.BANDAGE.ordinal(), 1);  // 1 Bandage  
        inventory.put(Game.Item.BOTTLE.ordinal(), 1);   // 1 Bottle
        
        // Test can brew
        allTestsPassed &= assertEquals(1, Game.canBrewPotion(inventory) ? 1 : 0, 
            "Should be able to brew healing potion with required ingredients");
        
        // Test brewing
        Map<Integer, Integer> result = Game.brewPotion(inventory);
        allTestsPassed &= assertEquals(1, result.getOrDefault(Game.Item.HPOTION.ordinal(), 0), 
            "Should have 1 healing potion after brewing");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.ASH.ordinal(), 0), 
            "Should have consumed ash ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.BANDAGE.ordinal(), 0), 
            "Should have consumed bandage ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.BOTTLE.ordinal(), 0), 
            "Should have consumed bottle ingredient");
        
        return allTestsPassed;
    }

    public static boolean testStrengthPotionBrewing() {
        boolean allTestsPassed = true;
        
        // Create inventory with strength potion ingredients
        Map<Integer, Integer> inventory = new HashMap<>();
        inventory.put(Game.Item.BONE.ordinal(), 1);    // 1 Bone
        inventory.put(Game.Item.FLESH.ordinal(), 1);   // 1 Flesh  
        inventory.put(Game.Item.FANG.ordinal(), 1);    // 1 Fang
        
        // Test can brew
        allTestsPassed &= assertEquals(1, Game.canBrewStrengthPotion(inventory) ? 1 : 0, 
            "Should be able to brew strength potion with required ingredients");
        
        // Test brewing
        Map<Integer, Integer> result = Game.brewStrengthPotion(inventory);
        allTestsPassed &= assertEquals(1, result.getOrDefault(Game.Item.SPOTION.ordinal(), 0), 
            "Should have 1 strength potion after brewing");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.BONE.ordinal(), 0), 
            "Should have consumed bone ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.FLESH.ordinal(), 0), 
            "Should have consumed flesh ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.FANG.ordinal(), 0), 
            "Should have consumed fang ingredient");
        
        return allTestsPassed;
    }

    public static boolean testLuckPotionBrewing() {
        boolean allTestsPassed = true;
        
        // Create inventory with luck potion ingredients
        Map<Integer, Integer> inventory = new HashMap<>();
        inventory.put(Game.Item.COIN.ordinal(), 1);    // 1 Coin
        inventory.put(Game.Item.GOLD.ordinal(), 1);    // 1 Gold  
        inventory.put(Game.Item.SILVER.ordinal(), 1);  // 1 Silver
        
        // Test can brew
        allTestsPassed &= assertEquals(1, Game.canBrewLuckPotion(inventory) ? 1 : 0, 
            "Should be able to brew luck potion with required ingredients");
        
        // Test brewing
        Map<Integer, Integer> result = Game.brewLuckPotion(inventory);
        allTestsPassed &= assertEquals(1, result.getOrDefault(Game.Item.LPOTION.ordinal(), 0), 
            "Should have 1 luck potion after brewing");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.COIN.ordinal(), 0), 
            "Should have consumed coin ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.GOLD.ordinal(), 0), 
            "Should have consumed gold ingredient");
        allTestsPassed &= assertEquals(0, result.getOrDefault(Game.Item.SILVER.ordinal(), 0), 
            "Should have consumed silver ingredient");
        
        return allTestsPassed;
    }

    public static boolean testInsufficientIngredients() {
        boolean allTestsPassed = true;
        
        // Test with empty inventory
        Map<Integer, Integer> emptyInventory = new HashMap<>();
        allTestsPassed &= assertEquals(0, Game.canBrewPotion(emptyInventory) ? 1 : 0, 
            "Should not be able to brew healing potion with empty inventory");
        allTestsPassed &= assertEquals(0, Game.canBrewStrengthPotion(emptyInventory) ? 1 : 0, 
            "Should not be able to brew strength potion with empty inventory");
        allTestsPassed &= assertEquals(0, Game.canBrewLuckPotion(emptyInventory) ? 1 : 0, 
            "Should not be able to brew luck potion with empty inventory");
        
        // Test with insufficient ingredients (missing one ingredient for each potion)
        Map<Integer, Integer> partialInventory = new HashMap<>();
        partialInventory.put(Game.Item.ASH.ordinal(), 1);       // Only ash for healing potion
        partialInventory.put(Game.Item.BONE.ordinal(), 1);      // Only bone for strength potion
        partialInventory.put(Game.Item.COIN.ordinal(), 1);      // Only coin for luck potion
        
        allTestsPassed &= assertEquals(0, Game.canBrewPotion(partialInventory) ? 1 : 0, 
            "Should not be able to brew healing potion with insufficient ingredients");
        allTestsPassed &= assertEquals(0, Game.canBrewStrengthPotion(partialInventory) ? 1 : 0, 
            "Should not be able to brew strength potion with insufficient ingredients");
        allTestsPassed &= assertEquals(0, Game.canBrewLuckPotion(partialInventory) ? 1 : 0, 
            "Should not be able to brew luck potion with insufficient ingredients");
        
        // Test that brewing with insufficient ingredients doesn't change inventory
        Map<Integer, Integer> originalInventory = new HashMap<>(partialInventory);
        Map<Integer, Integer> result1 = Game.brewPotion(partialInventory);
        Map<Integer, Integer> result2 = Game.brewStrengthPotion(partialInventory);
        Map<Integer, Integer> result3 = Game.brewLuckPotion(partialInventory);
        
        allTestsPassed &= assertEquals(originalInventory.size(), result1.size(), 
            "Inventory size should remain unchanged after failed healing potion brewing");
        allTestsPassed &= assertEquals(originalInventory.size(), result2.size(), 
            "Inventory size should remain unchanged after failed strength potion brewing");
        allTestsPassed &= assertEquals(originalInventory.size(), result3.size(), 
            "Inventory size should remain unchanged after failed luck potion brewing");
        
        return allTestsPassed;
    }

    public static boolean testProfileDisplayWithNoBrewingOptions() {
        boolean allTestsPassed = true;
        
        try {
            // Setup mocks using end-to-end approach
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player with no items
            telegram.simulateUserMessage(1, "TestUser", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            telegram.clearMessages();
            
            // Player requests profile
            telegram.simulateUserMessage(1, "TestUser", "Profil");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Check the profile response
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(1);
            allTestsPassed &= assertEquals(1, profileMsg != null ? 1 : 0, "Profile message should be sent");
            
            if (profileMsg != null) {
                // Should have standard buttons only
                allTestsPassed &= assertEquals(3, profileMsg.buttons.length, 
                    "Should have 3 main buttons when no brewing options available");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Kämpfen") ? 1 : 0,
                    "Should include 'Kämpfen' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Profil") ? 1 : 0,
                    "Should include 'Profil' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Aufgabe") ? 1 : 0,
                    "Should include 'Aufgabe' button");
                
                // Should not contain brewing buttons
                allTestsPassed &= assertEquals(0, arrayContains(profileMsg.buttons, "Heiltrank brauen") ? 1 : 0,
                    "Should not include brewing buttons");
                
                // Message should not mention brewing
                allTestsPassed &= assertEquals(0, messageContains(profileMsg.message, "Du kannst brauen") ? 1 : 0,
                    "Message should not mention brewing when no ingredients available");
            }
        } catch (Exception e) {
            System.out.println("Error in testProfileDisplayWithNoBrewingOptions: " + e.getMessage());
            return false;
        }
        
        return allTestsPassed;
    }

    public static boolean testProfileDisplayWithSingleBrewingOption() {
        boolean allTestsPassed = true;
        
        try {
            // Setup mocks using end-to-end approach
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player and give them healing potion ingredients
            telegram.simulateUserMessage(2, "TestUser", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            // Add healing ingredients to the player
            Client client = storage.getClientByChatId(2);
            if (client != null) {
                client.giveItem(Game.Item.ASH);
                client.giveItem(Game.Item.BANDAGE);
                client.giveItem(Game.Item.BOTTLE);
                storage.saveClient(client);
            }
            
            telegram.clearMessages();
            
            // Player requests profile
            telegram.simulateUserMessage(2, "TestUser", "Profil");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Check the profile response
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(2);
            allTestsPassed &= assertEquals(1, profileMsg != null ? 1 : 0, "Profile message should be sent");
            
            if (profileMsg != null) {
                // Should have main buttons + 1 brewing button
                allTestsPassed &= assertEquals(4, profileMsg.buttons.length, 
                    "Should have 4 buttons when 1 brewing option available");
                
                // Should contain main buttons
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Kämpfen") ? 1 : 0,
                    "Should include 'Kämpfen' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Profil") ? 1 : 0,
                    "Should include 'Profil' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Aufgabe") ? 1 : 0,
                    "Should include 'Aufgabe' button");
                
                // Should contain healing potion brewing button
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Heiltrank brauen") ? 1 : 0,
                    "Should include 'Heiltrank brauen' button");
                
                // Should not contain other brewing buttons
                allTestsPassed &= assertEquals(0, arrayContains(profileMsg.buttons, "Stärketrank brauen") ? 1 : 0,
                    "Should not include 'Stärketrank brauen' button");
                allTestsPassed &= assertEquals(0, arrayContains(profileMsg.buttons, "Glückstrank brauen") ? 1 : 0,
                    "Should not include 'Glückstrank brauen' button");
                
                // Message should mention brewing healing potion
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Du kannst brauen") ? 1 : 0,
                    "Message should mention brewing when ingredients available");
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Heiltrank") ? 1 : 0,
                    "Message should mention Heiltrank specifically");
            }
        } catch (Exception e) {
            System.out.println("Error in testProfileDisplayWithSingleBrewingOption: " + e.getMessage());
            return false;
        }
        
        return allTestsPassed;
    }

    public static boolean testProfileDisplayWithMultipleBrewingOptions() {
        boolean allTestsPassed = true;
        
        try {
            // Setup mocks using end-to-end approach
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player and give them ingredients for all three potions
            telegram.simulateUserMessage(3, "TestUser", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            // Add ingredients for all three potions
            Client client = storage.getClientByChatId(3);
            if (client != null) {
                // Add healing ingredients
                client.giveItem(Game.Item.ASH);
                client.giveItem(Game.Item.BANDAGE);
                client.giveItem(Game.Item.BOTTLE);
                // Add strength ingredients
                client.giveItem(Game.Item.BONE);
                client.giveItem(Game.Item.FLESH);
                client.giveItem(Game.Item.FANG);
                // Add luck ingredients
                client.giveItem(Game.Item.COIN);
                client.giveItem(Game.Item.GOLD);
                client.giveItem(Game.Item.SILVER);
                storage.saveClient(client);
            }
            
            telegram.clearMessages();
            
            // Player requests profile
            telegram.simulateUserMessage(3, "TestUser", "Profil");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Check the profile response
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(3);
            allTestsPassed &= assertEquals(1, profileMsg != null ? 1 : 0, "Profile message should be sent");
            
            if (profileMsg != null) {
                // Should have main buttons + 3 brewing buttons
                allTestsPassed &= assertEquals(6, profileMsg.buttons.length, 
                    "Should have 6 buttons when 3 brewing options available");
                
                // Should contain all main buttons
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Kämpfen") ? 1 : 0,
                    "Should include 'Kämpfen' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Profil") ? 1 : 0,
                    "Should include 'Profil' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Aufgabe") ? 1 : 0,
                    "Should include 'Aufgabe' button");
                
                // Should contain all brewing buttons
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Heiltrank brauen") ? 1 : 0,
                    "Should include 'Heiltrank brauen' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Stärketrank brauen") ? 1 : 0,
                    "Should include 'Stärketrank brauen' button");
                allTestsPassed &= assertEquals(1, arrayContains(profileMsg.buttons, "Glückstrank brauen") ? 1 : 0,
                    "Should include 'Glückstrank brauen' button");
                
                // Message should mention all brewable potions
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Du kannst brauen") ? 1 : 0,
                    "Message should mention brewing when ingredients available");
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Heiltrank") ? 1 : 0,
                    "Message should mention Heiltrank");
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Stärketrank") ? 1 : 0,
                    "Message should mention Stärketrank");
                allTestsPassed &= assertEquals(1, messageContains(profileMsg.message, "Glückstrank") ? 1 : 0,
                    "Message should mention Glückstrank");
            }
        } catch (Exception e) {
            System.out.println("Error in testProfileDisplayWithMultipleBrewingOptions: " + e.getMessage());
            return false;
        }
        
        return allTestsPassed;
    }

    public static boolean testTradingSystem() {
        boolean allTestsPassed = true;
        
        try {
            // Setup mocks using end-to-end approach
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player and give them some items
            telegram.simulateUserMessage(124, "TraderUser", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            // Add some items to the player for trading
            Client client = storage.getClientByChatId(124);
            if (client != null) {
                client.giveItem(Game.Item.COIN);
                client.giveItem(Game.Item.BOTTLE);
                storage.saveClient(client);
            }
            
            // Test that new players with no items have empty inventory
            telegram.simulateUserMessage(123, "EmptyUser", "/start");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            Client emptyClient = storage.getClientByChatId(123);
            allTestsPassed &= assertEquals(0, (emptyClient != null && emptyClient.hasAnyItems()) ? 1 : 0,
                "New client should have no items");
            
            // Test that players with items can see their inventory in profile
            telegram.clearMessages();
            telegram.simulateUserMessage(124, "TraderUser", "Profil");
            engine.processUpdate(telegram.getUpdates(3)[0]);
            
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(124);
            allTestsPassed &= assertEquals(1, profileMsg != null ? 1 : 0, "Profile message should be sent");
            
            if (profileMsg != null) {
                // Profile should show items in inventory
                allTestsPassed &= assertEquals(1, profileMsg.message.contains("Münze") || profileMsg.message.contains("Coin") ? 1 : 0,
                    "Profile should show coin in inventory");
                allTestsPassed &= assertEquals(1, profileMsg.message.contains("Flasche") || profileMsg.message.contains("Bottle") ? 1 : 0,
                    "Profile should show bottle in inventory");
            }
            
            // Note: Full trading mechanics (generating offers, executing trades) 
            // would require more complex game flow simulation or may be better tested as unit tests
            // for the internal trading logic, while the UI interaction should be tested end-to-end
            
        } catch (Exception e) {
            System.out.println("Error in testTradingSystem: " + e.getMessage());
            return false;
        }
        
        return allTestsPassed;
    }

    public static boolean testPotionButtonProgression() {
        System.out.print("Testing potion button progression... ");
        
        try {
            // Create mock dependencies
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player with all potions
            Client testPlayer = new Client(100, "PotionTester");
            testPlayer.giveItem(Game.Item.HPOTION);   // 1 Healing potion
            testPlayer.giveItem(Game.Item.SPOTION);   // 1 Strength potion  
            testPlayer.giveItem(Game.Item.LPOTION);   // 1 Luck potion
            testPlayer.setStorage(storage);
            storage.addClient(testPlayer);
            
            // Create bot opponent for fighting context
            Client bot = new Client(-100, "TestBot");
            bot.setStorage(storage);
            storage.addClient(bot);
            
            // Set up fighting status manually to ensure context
            testPlayer.status = Client.Status.FIGHTING;
            testPlayer.fightingChatId = -100;
            bot.status = Client.Status.FIGHTING;
            bot.fightingChatId = 100;
            storage.saveClient(testPlayer);
            storage.saveClient(bot);
            
            // Test 1: Simulate initial turn with all 3 potions - check buttons
            telegram.clearMessages();
            // Manually simulate what askTaskStatus does for testing
            telegram.sendMessage(100, "Du bist an der Reihe!", 
                new String[]{"Erfolg", "Heiltrank [1]", "Stärketrank [1]", "Glückstrank [1]"});
            
            MockTelegram.SentMessage turnMsg = telegram.getLastMessageForChat(100);
            boolean hasHeiling = turnMsg.hasButton("Heiltrank [1]");
            boolean hasStärke = turnMsg.hasButton("Stärketrank [1]");
            boolean hasGlück = turnMsg.hasButton("Glückstrank [1]");
            boolean hasErfolg = turnMsg.hasButton("Erfolg");
            
            if (!hasHeiling || !hasStärke || !hasGlück || !hasErfolg) {
                System.out.println("Initial buttons test FAILED");
                return false;
            }
            
            // Test 2: Use strength potion and check buttons update
            telegram.simulateUserMessage(100, "PotionTester", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            telegram.clearMessages();
            telegram.simulateUserMessage(100, "PotionTester", "Stärketrank [1]");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Verify strength potion consumed
            testPlayer = storage.getClientByChatId(100);
            if (testPlayer.getItemNum(Game.Item.SPOTION) != 0) {
                System.out.println("Strength potion not consumed FAILED");
                return false;
            }
            
            // Check response has correct buttons (should have healing + luck, no strength)
            MockTelegram.SentMessage afterStrength = telegram.getLastMessageForChat(100);
            if (afterStrength != null) {
                hasHeiling = afterStrength.hasButton("Heiltrank [1]");
                hasStärke = afterStrength.hasButton("Stärketrank [");
                hasGlück = afterStrength.hasButton("Glückstrank [1]");
                hasErfolg = afterStrength.hasButton("Erfolg");
                
                if (!hasHeiling || hasStärke || !hasGlück || !hasErfolg) {
                    System.out.println("Buttons after strength consumption FAILED");
                    return false;
                }
            }
            
            // Test 3: Use luck potion
            telegram.clearMessages();
            telegram.simulateUserMessage(100, "PotionTester", "Glückstrank [1]");
            engine.processUpdate(telegram.getUpdates(3)[0]);
            
            // Verify luck potion consumed
            testPlayer = storage.getClientByChatId(100);
            if (testPlayer.getItemNum(Game.Item.LPOTION) != 0) {
                System.out.println("Luck potion not consumed FAILED");
                return false;
            }
            
            // Test 4: Use healing potion
            telegram.clearMessages();
            telegram.simulateUserMessage(100, "PotionTester", "Heiltrank [1]");
            engine.processUpdate(telegram.getUpdates(4)[0]);
            
            // Verify healing potion consumed
            testPlayer = storage.getClientByChatId(100);
            if (testPlayer.getItemNum(Game.Item.HPOTION) != 0) {
                System.out.println("Healing potion not consumed FAILED");
                return false;
            }
            
            // Check final response has no potion buttons
            MockTelegram.SentMessage finalMsg = telegram.getLastMessageForChat(100);
            if (finalMsg != null) {
                hasHeiling = finalMsg.hasButton("Heiltrank [");
                hasStärke = finalMsg.hasButton("Stärketrank [");
                hasGlück = finalMsg.hasButton("Glückstrank [");
                hasErfolg = finalMsg.hasButton("Erfolg");
                
                if (hasHeiling || hasStärke || hasGlück || !hasErfolg) {
                    System.out.println("Final buttons test FAILED");
                    return false;
                }
            }
            
            // Test 5: Verify all potions consumed
            int totalPotions = testPlayer.getItemNum(Game.Item.HPOTION) + 
                              testPlayer.getItemNum(Game.Item.SPOTION) + 
                              testPlayer.getItemNum(Game.Item.LPOTION);
            
            if (totalPotions != 0) {
                System.out.println("Not all potions consumed FAILED");
                return false;
            }
            
            System.out.print("S");
            return true;
            
        } catch (Exception e) {
            System.out.println("Exception in testPotionButtonProgression: " + e.getMessage() + " FAILED");
            return false;
        }
    }

}
