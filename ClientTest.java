package FightLang;

import java.util.HashMap;
import java.util.Map;

public class ClientTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testNextExpLevel();
        allTestsPassed &= testPotionBrewing();
        allTestsPassed &= testBrewableOptionsGeneration();
        allTestsPassed &= testCanBrewAnyPotion();
        allTestsPassed &= testMultiplePotionOptions();
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

    public static boolean testBrewableOptionsGeneration() {
        boolean allTestsPassed = true;
        
        // Test empty inventory - no brewing options
        Map<Integer, Integer> emptyInventory = new HashMap<>();
        String[] options = Game.getBrewableOptions(emptyInventory);
        allTestsPassed &= assertEquals(0, options.length, 
            "Empty inventory should have no brewing options");
        
        // Test healing potion ingredients only
        Map<Integer, Integer> healingInventory = new HashMap<>();
        healingInventory.put(Game.Item.ASH.ordinal(), 1);
        healingInventory.put(Game.Item.BANDAGE.ordinal(), 1);
        healingInventory.put(Game.Item.BOTTLE.ordinal(), 1);
        
        options = Game.getBrewableOptions(healingInventory);
        allTestsPassed &= assertEquals(1, options.length, 
            "Healing ingredients should provide 1 brewing option");
        allTestsPassed &= assertEquals(1, options[0].equals("Heiltrank brauen") ? 1 : 0,
            "Should offer 'Heiltrank brauen' option");
        
        // Test strength potion ingredients only
        Map<Integer, Integer> strengthInventory = new HashMap<>();
        strengthInventory.put(Game.Item.BONE.ordinal(), 1);
        strengthInventory.put(Game.Item.FLESH.ordinal(), 1);
        strengthInventory.put(Game.Item.FANG.ordinal(), 1);
        
        options = Game.getBrewableOptions(strengthInventory);
        allTestsPassed &= assertEquals(1, options.length, 
            "Strength ingredients should provide 1 brewing option");
        allTestsPassed &= assertEquals(1, options[0].equals("Stärketrank brauen") ? 1 : 0,
            "Should offer 'Stärketrank brauen' option");
        
        // Test luck potion ingredients only
        Map<Integer, Integer> luckInventory = new HashMap<>();
        luckInventory.put(Game.Item.COIN.ordinal(), 1);
        luckInventory.put(Game.Item.GOLD.ordinal(), 1);
        luckInventory.put(Game.Item.SILVER.ordinal(), 1);
        
        options = Game.getBrewableOptions(luckInventory);
        allTestsPassed &= assertEquals(1, options.length, 
            "Luck ingredients should provide 1 brewing option");
        allTestsPassed &= assertEquals(1, options[0].equals("Glückstrank brauen") ? 1 : 0,
            "Should offer 'Glückstrank brauen' option");
        
        return allTestsPassed;
    }

    public static boolean testCanBrewAnyPotion() {
        boolean allTestsPassed = true;
        
        // Test empty inventory
        Map<Integer, Integer> emptyInventory = new HashMap<>();
        allTestsPassed &= assertEquals(0, Game.canBrewAnyPotion(emptyInventory) ? 1 : 0,
            "Empty inventory should not allow brewing any potion");
        
        // Test with healing ingredients
        Map<Integer, Integer> healingInventory = new HashMap<>();
        healingInventory.put(Game.Item.ASH.ordinal(), 1);
        healingInventory.put(Game.Item.BANDAGE.ordinal(), 1);
        healingInventory.put(Game.Item.BOTTLE.ordinal(), 1);
        
        allTestsPassed &= assertEquals(1, Game.canBrewAnyPotion(healingInventory) ? 1 : 0,
            "Should be able to brew any potion with healing ingredients");
        
        // Test with strength ingredients
        Map<Integer, Integer> strengthInventory = new HashMap<>();
        strengthInventory.put(Game.Item.BONE.ordinal(), 1);
        strengthInventory.put(Game.Item.FLESH.ordinal(), 1);
        strengthInventory.put(Game.Item.FANG.ordinal(), 1);
        
        allTestsPassed &= assertEquals(1, Game.canBrewAnyPotion(strengthInventory) ? 1 : 0,
            "Should be able to brew any potion with strength ingredients");
        
        // Test with luck ingredients
        Map<Integer, Integer> luckInventory = new HashMap<>();
        luckInventory.put(Game.Item.COIN.ordinal(), 1);
        luckInventory.put(Game.Item.GOLD.ordinal(), 1);
        luckInventory.put(Game.Item.SILVER.ordinal(), 1);
        
        allTestsPassed &= assertEquals(1, Game.canBrewAnyPotion(luckInventory) ? 1 : 0,
            "Should be able to brew any potion with luck ingredients");
        
        return allTestsPassed;
    }

    public static boolean testMultiplePotionOptions() {
        boolean allTestsPassed = true;
        
        // Test inventory with ingredients for all three potions
        Map<Integer, Integer> multiInventory = new HashMap<>();
        // Healing ingredients
        multiInventory.put(Game.Item.ASH.ordinal(), 1);
        multiInventory.put(Game.Item.BANDAGE.ordinal(), 1);
        multiInventory.put(Game.Item.BOTTLE.ordinal(), 1);
        // Strength ingredients
        multiInventory.put(Game.Item.BONE.ordinal(), 1);
        multiInventory.put(Game.Item.FLESH.ordinal(), 1);
        multiInventory.put(Game.Item.FANG.ordinal(), 1);
        // Luck ingredients
        multiInventory.put(Game.Item.COIN.ordinal(), 1);
        multiInventory.put(Game.Item.GOLD.ordinal(), 1);
        multiInventory.put(Game.Item.SILVER.ordinal(), 1);
        
        String[] options = Game.getBrewableOptions(multiInventory);
        allTestsPassed &= assertEquals(3, options.length, 
            "Should have 3 brewing options when all ingredients are available");
        
        // Check that all expected options are present
        boolean hasHealing = false;
        boolean hasStrength = false;
        boolean hasLuck = false;
        
        for (String option : options) {
            if (option.equals("Heiltrank brauen")) hasHealing = true;
            else if (option.equals("Stärketrank brauen")) hasStrength = true;
            else if (option.equals("Glückstrank brauen")) hasLuck = true;
        }
        
        allTestsPassed &= assertEquals(1, hasHealing ? 1 : 0,
            "Should include 'Heiltrank brauen' option");
        allTestsPassed &= assertEquals(1, hasStrength ? 1 : 0,
            "Should include 'Stärketrank brauen' option");
        allTestsPassed &= assertEquals(1, hasLuck ? 1 : 0,
            "Should include 'Glückstrank brauen' option");
        
        // Test with ingredients for only two potions (healing + strength)
        Map<Integer, Integer> twoInventory = new HashMap<>();
        twoInventory.put(Game.Item.ASH.ordinal(), 1);
        twoInventory.put(Game.Item.BANDAGE.ordinal(), 1);
        twoInventory.put(Game.Item.BOTTLE.ordinal(), 1);
        twoInventory.put(Game.Item.BONE.ordinal(), 1);
        twoInventory.put(Game.Item.FLESH.ordinal(), 1);
        twoInventory.put(Game.Item.FANG.ordinal(), 1);
        
        options = Game.getBrewableOptions(twoInventory);
        allTestsPassed &= assertEquals(2, options.length, 
            "Should have 2 brewing options when healing and strength ingredients are available");
        
        return allTestsPassed;
    }
}
