package FightLang;

import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testNextExpLevel();
        allTestsPassed &= testPotionBrewing();
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
        
        // Create client with empty inventory
        Client client = new Client(1, "TestUser");
        client.nameChangeHintSent = true; // Avoid name change hint in message
        
        // Test profile display
        Main.ProfileDisplay display = Main.buildProfileDisplay(client);
        
        // Should have standard buttons only
        allTestsPassed &= assertEquals(3, display.buttons.length, 
            "Should have 3 main buttons when no brewing options available");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Kämpfen") ? 1 : 0,
            "Should include 'Kämpfen' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Profil") ? 1 : 0,
            "Should include 'Profil' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Aufgabe") ? 1 : 0,
            "Should include 'Aufgabe' button");
        
        // Should not contain brewing buttons
        allTestsPassed &= assertEquals(0, arrayContains(display.buttons, "Heiltrank brauen") ? 1 : 0,
            "Should not include brewing buttons");
        
        // Message should not mention brewing
        allTestsPassed &= assertEquals(0, messageContains(display.message, "Du kannst brauen") ? 1 : 0,
            "Message should not mention brewing when no ingredients available");
        
        return allTestsPassed;
    }

    public static boolean testProfileDisplayWithSingleBrewingOption() {
        boolean allTestsPassed = true;
        
        // Create client with healing potion ingredients
        Client client = new Client(2, "TestUser");
        client.nameChangeHintSent = true;
        client.giveItem(Game.Item.ASH);
        client.giveItem(Game.Item.BANDAGE);
        client.giveItem(Game.Item.BOTTLE);
        
        // Test profile display
        Main.ProfileDisplay display = Main.buildProfileDisplay(client);
        
        // Should have main buttons + 1 brewing button
        allTestsPassed &= assertEquals(4, display.buttons.length, 
            "Should have 4 buttons when 1 brewing option available");
        
        // Should contain main buttons
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Kämpfen") ? 1 : 0,
            "Should include 'Kämpfen' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Profil") ? 1 : 0,
            "Should include 'Profil' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Aufgabe") ? 1 : 0,
            "Should include 'Aufgabe' button");
        
        // Should contain healing potion brewing button
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Heiltrank brauen") ? 1 : 0,
            "Should include 'Heiltrank brauen' button");
        
        // Should not contain other brewing buttons
        allTestsPassed &= assertEquals(0, arrayContains(display.buttons, "Stärketrank brauen") ? 1 : 0,
            "Should not include 'Stärketrank brauen' button");
        allTestsPassed &= assertEquals(0, arrayContains(display.buttons, "Glückstrank brauen") ? 1 : 0,
            "Should not include 'Glückstrank brauen' button");
        
        // Message should mention brewing healing potion
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Du kannst brauen") ? 1 : 0,
            "Message should mention brewing when ingredients available");
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Heiltrank") ? 1 : 0,
            "Message should mention Heiltrank specifically");
        
        return allTestsPassed;
    }

    public static boolean testProfileDisplayWithMultipleBrewingOptions() {
        boolean allTestsPassed = true;
        
        // Create client with ingredients for all three potions
        Client client = new Client(3, "TestUser");
        client.nameChangeHintSent = true;
        
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
        
        // Test profile display
        Main.ProfileDisplay display = Main.buildProfileDisplay(client);
        
        // Should have main buttons + 3 brewing buttons
        allTestsPassed &= assertEquals(6, display.buttons.length, 
            "Should have 6 buttons when 3 brewing options available");
        
        // Should contain all main buttons
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Kämpfen") ? 1 : 0,
            "Should include 'Kämpfen' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Profil") ? 1 : 0,
            "Should include 'Profil' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Aufgabe") ? 1 : 0,
            "Should include 'Aufgabe' button");
        
        // Should contain all brewing buttons
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Heiltrank brauen") ? 1 : 0,
            "Should include 'Heiltrank brauen' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Stärketrank brauen") ? 1 : 0,
            "Should include 'Stärketrank brauen' button");
        allTestsPassed &= assertEquals(1, arrayContains(display.buttons, "Glückstrank brauen") ? 1 : 0,
            "Should include 'Glückstrank brauen' button");
        
        // Message should mention all brewable potions
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Du kannst brauen") ? 1 : 0,
            "Message should mention brewing when ingredients available");
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Heiltrank") ? 1 : 0,
            "Message should mention Heiltrank");
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Stärketrank") ? 1 : 0,
            "Message should mention Stärketrank");
        allTestsPassed &= assertEquals(1, messageContains(display.message, "Glückstrank") ? 1 : 0,
            "Message should mention Glückstrank");
        
        return allTestsPassed;
    }

    public static boolean testTradingSystem() {
        boolean allTestsPassed = true;
        
        // Test 1: Client with no items should have empty inventory
        Client clientNoItems = new Client(123, "testuser");
        allTestsPassed &= assertEquals(0, clientNoItems.hasAnyItems() ? 1 : 0,
            "New client should have no items");
        
        // Test 2: Client with items should be able to enter trading state
        Client clientWithItems = new Client(124, "testuser2");
        clientWithItems.giveItem(Game.Item.COIN);
        clientWithItems.giveItem(Game.Item.BOTTLE);
        
        allTestsPassed &= assertEquals(1, clientWithItems.hasItem(Game.Item.COIN) ? 1 : 0,
            "Client should have coin");
        allTestsPassed &= assertEquals(1, clientWithItems.hasItem(Game.Item.BOTTLE) ? 1 : 0,
            "Client should have bottle");
        
        // Test 3: Generate trade offer
        boolean offerGenerated = clientWithItems.generateTradeOffer(Game.Item.GOLD);
        allTestsPassed &= assertEquals(1, offerGenerated ? 1 : 0,
            "Trade offer should be generated successfully");
        allTestsPassed &= assertEquals(1, clientWithItems.status == Client.Status.TRADING ? 1 : 0,
            "Client should be in trading status");
        allTestsPassed &= assertEquals(1, clientWithItems.offeredItem != null ? 1 : 0,
            "Offered item should be set");
        allTestsPassed &= assertEquals(1, clientWithItems.hasItem(clientWithItems.offeredItem) ? 1 : 0,
            "Offered item should be one that client actually has");
        allTestsPassed &= assertEquals(1, clientWithItems.requestedItem == Game.Item.GOLD ? 1 : 0,
            "Requested item should be gold");
        
        // Test 4: Simulate trade execution
        boolean tradeSuccessful = clientWithItems.executeTrade();
        allTestsPassed &= assertEquals(1, tradeSuccessful ? 1 : 0,
            "Trade execution should be successful");
        
        allTestsPassed &= assertEquals(1, clientWithItems.hasItem(Game.Item.GOLD) ? 1 : 0,
            "Client should have gold after trade");
        // Verify that inventory count changed (one item was traded away)
        int totalItemsAfterTrade = 0;
        for (Map.Entry<Integer, Integer> entry : clientWithItems.inventory.entrySet()) {
            totalItemsAfterTrade += entry.getValue();
        }
        allTestsPassed &= assertEquals(2, totalItemsAfterTrade,
            "Client should have 2 items after trade (started with 2, traded 1 for 1)");
        
        return allTestsPassed;
    }
}
