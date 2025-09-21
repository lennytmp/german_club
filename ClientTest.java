package FightLang;

import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testNextExpLevel();
        allTestsPassed &= testPotionBrewing();
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
}
