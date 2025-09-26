package FightLang;

import static FightLang.TestHelper.*;

/**
 * Core game mechanics test that focuses on the essential game flow
 * without relying on external dependencies like Phrases or Gemini.
 * 
 * REFACTORED: Now uses TestHelper utilities to reduce code duplication.
 */
public class CoreGameTest {
    
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        
        allTestsPassed &= testGameEngineCreation();
        allTestsPassed &= testProfileDisplay();
        allTestsPassed &= testFightPreparation();
        
        if (!allTestsPassed) {
            System.out.println("\nSome core game tests failed!");
            System.exit(1);
        }
    }
    
    /**
     * Test that GameEngine can be created with mock dependencies
     */
    private static boolean testGameEngineCreation() {
        boolean testPassed = true;
        
        try {
            TestEnvironment env = createTestEnvironment();
            testPassed &= assertTrue(env.engine != null, "GameEngine should be created successfully");
            
        } catch (Exception e) {
            System.out.println("Testing GameEngine creation... FAILED");
            System.out.println("Exception during GameEngine creation: " + e.getMessage());
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test profile display functionality through end-to-end simulation
     */
    private static boolean testProfileDisplay() {
        boolean testPassed = true;
        
        try {
            TestEnvironment env = createTestEnvironment();
            
            // Create a player with healing potion ingredients
            Client client = createPlayerWithItems(env, 100, "TestPlayer", 
                Game.Item.ASH, Game.Item.BANDAGE, Game.Item.BOTTLE);
            
            // Get player's profile
            MockTelegram.SentMessage profileMsg = getPlayerProfile(env, 100, "TestPlayer");
            
            // Validate basic profile info
            testPassed &= assertTrue(validateBasicProfile(profileMsg, "TestPlayer"), "Profile should contain basic player info");
            
            // Check brewing options are available
            testPassed &= assertTrue(profileMsg.hasButton("Heiltrank brauen"), "Should have healing potion brewing button when ingredients available");
            
        } catch (Exception e) {
            System.out.println("Testing profile display... FAILED");
            System.out.println("Exception: " + e.getMessage());
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test fight initiation through end-to-end simulation
     */
    private static boolean testFightPreparation() {
        boolean testPassed = true;
        
        try {
            TestEnvironment env = createTestEnvironment();
            
            // Create two players
            Client player1 = createPlayer(env, 200, "Fighter1");
            Client player2 = createPlayer(env, 300, "Fighter2");
            
            testPassed &= assertTrue(player1 != null, "Player1 should be created");
            testPassed &= assertTrue(player2 != null, "Player2 should be created");
            
            if (player1 != null && player2 != null) {
                testPassed &= assertTrue(player1.status == Client.Status.IDLE, "Player1 should start IDLE");
                testPassed &= assertTrue(player2.status == Client.Status.IDLE, "Player2 should start IDLE");
            }
            
            env.clearMessages();
            
            // Setup fight between the two players
            int firstPlayer = setupFight(env, 200, "Fighter1", 300, "Fighter2");
            
            // Verify fight initiation
            testPassed &= assertTrue(validateFightInitiation(env, 200, "Fighter1", 300, "Fighter2"), 
                                   "Fight should be properly initiated between both players");
            
            // Verify turn system
            testPassed &= assertTrue(validateTurnSystem(env, 200, 300), 
                                   "Exactly one player should have the first turn");
            
            // Verify both players are now in FIGHTING status
            player1 = env.storage.getClientByChatId(200);
            player2 = env.storage.getClientByChatId(300);
            if (player1 != null && player2 != null) {
                testPassed &= assertTrue(player1.status == Client.Status.FIGHTING, "Player1 should be FIGHTING after match");
                testPassed &= assertTrue(player2.status == Client.Status.FIGHTING, "Player2 should be FIGHTING after match");
                
                // They should be fighting each other
                testPassed &= assertTrue(player1.fightingChatId == player2.chatId, "Player1 should be fighting Player2");
                testPassed &= assertTrue(player2.fightingChatId == player1.chatId, "Player2 should be fighting Player1");
            }
            
        } catch (Exception e) {
            System.out.println("Testing fight preparation... FAILED");
            System.out.println("Exception: " + e.getMessage());
            testPassed = false;
        }
        
        return testPassed;
    }
}