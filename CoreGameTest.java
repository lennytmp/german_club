package FightLang;

/**
 * Core game mechanics test that focuses on the essential game flow
 * without relying on external dependencies like Phrases or Gemini.
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
    
    private static boolean assertEquals(boolean condition, String testName) {
        if (condition) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED");
        return false;
    }
    
    /**
     * Test that GameEngine can be created with mock dependencies
     */
    private static boolean testGameEngineCreation() {
        boolean testPassed = true;
        
        try {
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            testPassed &= assertEquals(engine != null, "GameEngine should be created successfully");
            
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
            // Setup mocks using the new approach
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create a player by simulating joining the game
            telegram.simulateUserMessage(100, "TestPlayer", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            // Give the player some items by manually adding them to storage
            Client client = storage.getClientByChatId(100);
            if (client != null) {
                client.giveItem(Game.Item.ASH);
                client.giveItem(Game.Item.BANDAGE);
                client.giveItem(Game.Item.BOTTLE);
                storage.saveClient(client);
            }
            
            telegram.clearMessages();
            
            // Simulate player requesting profile view
            telegram.simulateUserMessage(100, "TestPlayer", "Profil");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Check that a profile message was sent
            testPassed &= assertEquals(telegram.getMessageCountForChat(100) > 0, "Profile request should send response");
            
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(100);
            testPassed &= assertEquals(profileMsg != null, "Profile message should be sent");
            
            if (profileMsg != null) {
                testPassed &= assertEquals(profileMsg.message.contains("TestPlayer"), "Profile should contain player name");
                testPassed &= assertEquals(profileMsg.message.contains("Level:"), "Profile should contain level info");
                testPassed &= assertEquals(profileMsg.message.contains("Gesundheit:"), "Profile should contain health info");
                
                // Check that brewing options are available in buttons
                testPassed &= assertEquals(profileMsg.hasButton("Heiltrank brauen"), "Should have healing potion brewing button when ingredients available");
            }
            
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
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create two players by simulating them joining the game
            telegram.simulateUserMessage(200, "Fighter1", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            telegram.simulateUserMessage(300, "Fighter2", "/start");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Verify both players are created and initially IDLE
            Client player1 = storage.getClientByChatId(200);
            Client player2 = storage.getClientByChatId(300);
            testPassed &= assertEquals(player1 != null, "Player1 should be created");
            testPassed &= assertEquals(player2 != null, "Player2 should be created");
            
            if (player1 != null && player2 != null) {
                testPassed &= assertEquals(player1.status == Client.Status.IDLE, "Player1 should start IDLE");
                testPassed &= assertEquals(player2.status == Client.Status.IDLE, "Player2 should start IDLE");
            }
            
            telegram.clearMessages();
            
            // Both players look for fights - this should trigger fight matching
            telegram.simulateUserMessage(200, "Fighter1", "Kämpfen");
            engine.processUpdate(telegram.getUpdates(3)[0]);
            telegram.simulateUserMessage(300, "Fighter2", "Kämpfen");
            engine.processUpdate(telegram.getUpdates(4)[0]);
            
            // Verify fight messages were sent to both players
            testPassed &= assertEquals(telegram.hasMessageForChatContaining(200, "Du kämpfst jetzt mit Fighter2"), 
                                     "Fighter1 should be told they're fighting Fighter2");
            testPassed &= assertEquals(telegram.hasMessageForChatContaining(300, "Du kämpfst jetzt mit Fighter1"), 
                                     "Fighter2 should be told they're fighting Fighter1");
            
            // Verify both players are now in FIGHTING status
            player1 = storage.getClientByChatId(200);
            player2 = storage.getClientByChatId(300);
            if (player1 != null && player2 != null) {
                testPassed &= assertEquals(player1.status == Client.Status.FIGHTING, "Player1 should be FIGHTING after match");
                testPassed &= assertEquals(player2.status == Client.Status.FIGHTING, "Player2 should be FIGHTING after match");
                
                // They should be fighting each other
                testPassed &= assertEquals(player1.fightingChatId == player2.chatId, "Player1 should be fighting Player2");
                testPassed &= assertEquals(player2.fightingChatId == player1.chatId, "Player2 should be fighting Player1");
            }
            
        } catch (Exception e) {
            System.out.println("Testing fight preparation... FAILED");
            System.out.println("Exception: " + e.getMessage());
            testPassed = false;
        }
        
        return testPassed;
    }
}