package FightLang;

public class TestLowerLevelMobChance {
    public static void main(String[] args) {
        System.out.println("Testing lower level mob encounter chance...");
        
        // Print available mob types and their levels
        System.out.println("\nAvailable mob types:");
        for (BotConfig bc : Game.BOT_TYPES) {
            System.out.println("  " + bc.name + " (levels " + bc.minLevel + "-" + bc.maxLevel + ")");
        }
        
        // Test for a level 10 player
        System.out.println("\nTesting for level 10 player:");
        testMobEncounters(10);
        
        // Test for a level 5 player
        System.out.println("\nTesting for level 5 player:");
        testMobEncounters(5);
        
        // Test for a level 1 player
        System.out.println("\nTesting for level 1 player:");
        testMobEncounters(1);
    }
    
    private static void testMobEncounters(int playerLevel) {
        // Create a mock storage and player
        MockStorage storage = new MockStorage();
        Client player = new Client(1, "TestPlayer");
        player.setStorage(storage);
        player.level = playerLevel;
        player.totalFights = 10;
        player.fightsWon = 5;
        
        // Count lower level mobs available
        int lowerLevelMobTypes = 0;
        for (BotConfig bc : Game.BOT_TYPES) {
            if (bc.maxLevel < playerLevel) {
                lowerLevelMobTypes++;
            }
        }
        
        System.out.println("  Player level: " + playerLevel);
        System.out.println("  Lower level mob types available: " + lowerLevelMobTypes);
        
        if (lowerLevelMobTypes == 0) {
            System.out.println("  ✓ No lower level mobs available - chance system won't trigger");
            return;
        }
        
        // Test the mob selection multiple times
        int totalTests = 1000;
        int lowerLevelMobCount = 0;
        
        for (int i = 0; i < totalTests; i++) {
            // Create a bot based on the player
            Client bot = new Client(-i, player);
            bot.setStorage(storage);
            
            // Check if the bot's max level is lower than player's level
            BotConfig botType = null;
            for (BotConfig bc : Game.BOT_TYPES) {
                if (bc.name.equals(bot.username)) {
                    botType = bc;
                    break;
                }
            }
            
            if (botType != null && botType.maxLevel < playerLevel) {
                lowerLevelMobCount++;
            }
        }
        
        double percentage = (double) lowerLevelMobCount / totalTests * 100;
        System.out.println("  Lower level mobs encountered: " + lowerLevelMobCount + " out of " + totalTests);
        System.out.println("  Percentage: " + String.format("%.2f", percentage) + "%");
        
        // Expected range should be around 10% (±5% for statistical variation)
        if (percentage >= 5.0 && percentage <= 15.0) {
            System.out.println("  ✓ Test PASSED: Percentage is within expected range (5-15%)");
        } else {
            System.out.println("  ✗ Test FAILED: Percentage " + percentage + "% is outside expected range");
        }
    }
}