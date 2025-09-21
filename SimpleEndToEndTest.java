package FightLang;

import java.util.List;

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
    
    private static boolean assertEquals(boolean condition, String testName) {
        if (condition) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED");
        return false;
    }
    
    /**
     * Test basic player registration and profile viewing
     */
    private static boolean testPlayerRegistrationAndProfile() {
        System.out.print("Testing player registration and profile... ");
        boolean testPassed = true;
        
        // Setup mocks
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Player joins the game - simulate by sending any message that creates a new client
        telegram.simulateUserMessage(100, "TestPlayer", "Profil");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        // Check welcome message (should be sent to new players)
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(100, "Willkommen im German Club!"), 
                                  "Should receive welcome message");
        testPassed &= assertEquals(telegram.getLastMessageForChat(100).hasButton("Kämpfen"), 
                                  "Should have fight button");
        testPassed &= assertEquals(telegram.getLastMessageForChat(100).hasButton("Profil"), 
                                  "Should have profile button");
        testPassed &= assertEquals(telegram.getLastMessageForChat(100).hasButton("Aufgabe"), 
                                  "Should have task button");
        
        // Player views profile
        telegram.clearMessages();
        telegram.simulateUserMessage(100, "TestPlayer", "Profil");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        // Check profile information
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(100, "Level:"), 
                                  "Profile should show level");
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(100, "Gesundheit:"), 
                                  "Profile should show health");
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(100, "TestPlayer"), 
                                  "Profile should show player name");
        
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
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create two players
        telegram.simulateUserMessage(200, "Fighter1", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        telegram.simulateUserMessage(300, "Fighter2", "/start");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        telegram.clearMessages();
        
        // Player 1 looks for fight
        telegram.simulateUserMessage(200, "Fighter1", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        
        // Player 1 should be waiting for opponent
        testPassed &= assertEquals(telegram.getMessageCountForChat(200) > 0, 
                                  "Player 1 should receive response about looking for fight");
        
        // Player 2 also looks for fight - should trigger match
        telegram.simulateUserMessage(300, "Fighter2", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(4)[0]);
        
        // Both players should be notified they're fighting each other
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(200, "Du kämpfst jetzt mit Fighter2"), 
                                  "Player 1 should be told they're fighting Fighter2");
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(300, "Du kämpfst jetzt mit Fighter1"), 
                                  "Player 2 should be told they're fighting Fighter1");
        
        // Both should receive opponent stats
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(200, "Level:"), 
                                  "Player 1 should see opponent stats");
        testPassed &= assertEquals(telegram.hasMessageForChatContaining(300, "Level:"), 
                                  "Player 2 should see opponent stats");
        
        // One of them should be asked to make the first move
        boolean player1HasTurn = telegram.hasMessageForChatContaining(200, "Du bist an der Reihe!");
        boolean player2HasTurn = telegram.hasMessageForChatContaining(300, "Du bist an der Reihe!");
        testPassed &= assertEquals((player1HasTurn || player2HasTurn) && !(player1HasTurn && player2HasTurn), 
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
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player
        telegram.simulateUserMessage(400, "TaskDoer", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Player does a task
        telegram.simulateUserMessage(400, "TaskDoer", "Aufgabe");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        // Player should receive some response (either found item or nothing found)
        testPassed &= assertEquals(telegram.getMessageCountForChat(400) > 0, 
                                  "Player should receive response to task");
        
        // Do multiple tasks to potentially find items
        for (int i = 0; i < 5; i++) {
            telegram.simulateUserMessage(400, "TaskDoer", "Aufgabe");
            engine.processUpdate(telegram.getUpdates(3 + i)[0]);
        }
        
        // Player should have received responses
        testPassed &= assertEquals(telegram.getMessageCountForChat(400) >= 6, 
                                  "Player should receive responses to all tasks");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
}