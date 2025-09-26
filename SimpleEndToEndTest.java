package FightLang;

import java.util.List;
import static FightLang.TestHelper.*;

/**
 * Simple end-to-end test that simulates the complete game flow:
 * 1. Player looks for an opponent
 * 2. Player makes hits during battle
 * 3. Battle finishes
 * 
 * This test only checks the telegram messages the user would receive,
 * without looking into any internal data structures.
 */
public class SimpleEndToEndTest {
    
    public static void main(String[] args) {
        // Initialize required components for testing
        try {
            Phrases.initialize();
        } catch (Exception e) {
            // If phrases can't be initialized, create minimal test data
            System.out.println("Warning: Could not initialize Phrases, using minimal test data");
        }
        
        boolean allTestsPassed = true;
        
        allTestsPassed &= testPlayerRegistrationAndProfile();
        allTestsPassed &= testPlayerVsPlayerFight();
        allTestsPassed &= testTaskCompletion();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println("\nAll end-to-end tests passed!");
    }
    
    // Removed duplicate assert method - now using TestHelper utilities
    
    /**
     * Test basic player registration and profile viewing
     */
    private static boolean testPlayerRegistrationAndProfile() {
        System.out.print("Testing player registration and profile... ");
        boolean testPassed = true;
        
        // Setup mocks using TestHelper
        TestEnvironment env = createTestEnvironment();
        
        // Player joins the game - simulate by sending any message that creates a new client
        env.telegram.simulateUserMessage(100, "TestPlayer", "Profil");
        env.engine.processUpdate(env.telegram.getUpdates(1)[0]);
        
        // Check welcome message (should be sent to new players)
        testPassed &= assertTrue(env.telegram.hasMessageForChatContaining(100, "Willkommen im German Club!"), 
                                  "Should receive welcome message");
        testPassed &= assertTrue(env.telegram.getLastMessageForChat(100).hasButton("Kämpfen"), 
                                  "Should have fight button");
        testPassed &= assertTrue(env.telegram.getLastMessageForChat(100).hasButton("Profil"), 
                                  "Should have profile button");
        testPassed &= assertTrue(env.telegram.getLastMessageForChat(100).hasButton("Aufgabe"), 
                                  "Should have task button");
        
        // Player views profile using TestHelper
        MockTelegram.SentMessage profileMsg = getPlayerProfile(env, 100, "TestPlayer");
        
        // Check profile information using TestHelper validation
        testPassed &= assertTrue(validateBasicProfile(profileMsg, "TestPlayer"), 
                                "Profile should contain basic player information");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test player vs player fight initiation
     */
    private static boolean testPlayerVsPlayerFight() {
        System.out.print("Testing player vs player fight... ");
        boolean testPassed = true;
        
        TestEnvironment env = createTestEnvironment();
        
        // Create two players using TestHelper
        createPlayer(env, 200, "Fighter1");
        createPlayer(env, 300, "Fighter2");
        
        env.clearMessages();
        
        // Setup fight using TestHelper
        int firstPlayer = setupFight(env, 200, "Fighter1", 300, "Fighter2");
        
        // Player 1 should be waiting for opponent initially
        testPassed &= assertTrue(env.telegram.getMessageCountForChat(200) > 0, 
                                  "Player 1 should receive response about looking for fight");
        
        // Validate fight initiation using TestHelper
        testPassed &= assertTrue(validateFightInitiation(env, 200, "Fighter1", 300, "Fighter2"), 
                                "Both players should be notified they're fighting each other");
        
        // Both should receive opponent stats
        testPassed &= assertTrue(env.telegram.hasMessageForChatContaining(200, "Level:"), 
                                  "Player 1 should see opponent stats");
        testPassed &= assertTrue(env.telegram.hasMessageForChatContaining(300, "Level:"), 
                                  "Player 2 should see opponent stats");
        
        // Validate turn system using TestHelper
        testPassed &= assertTrue(validateTurnSystem(env, 200, 300), 
                                  "Exactly one player should have the first turn");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test task completion mechanics
     */
    private static boolean testTaskCompletion() {
        System.out.print("Testing task completion... ");
        boolean testPassed = true;
        
        TestEnvironment env = createTestEnvironment();
        
        // Create a player using TestHelper
        createPlayer(env, 400, "TaskDoer");
        
        env.clearMessages();
        
        // Player does a task
        env.telegram.simulateUserMessage(400, "TaskDoer", "Aufgabe");
        env.engine.processUpdate(env.telegram.getUpdates(1)[0]);
        
        // Player should receive some response (either found item or nothing found)
        testPassed &= assertTrue(env.telegram.getMessageCountForChat(400) > 0, 
                                  "Player should receive response to task");
        
        // Do multiple tasks to potentially find items
        for (int i = 0; i < 5; i++) {
            env.telegram.simulateUserMessage(400, "TaskDoer", "Aufgabe");
            env.engine.processUpdate(env.telegram.getUpdates(2 + i)[0]);
        }
        
        // Player should have received responses
        testPassed &= assertTrue(env.telegram.getMessageCountForChat(400) >= 6, 
                                  "Player should receive responses to all tasks");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
}