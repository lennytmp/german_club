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
            System.exit(1);
        }
        System.out.println("\nAll core game tests passed!");
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
        System.out.print("Testing GameEngine creation... ");
        boolean testPassed = true;
        
        try {
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            testPassed &= assertEquals(engine != null, "GameEngine should be created successfully");
            
        } catch (Exception e) {
            System.out.println("Exception during GameEngine creation: " + e.getMessage());
            testPassed = false;
        }
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test profile display functionality
     */
    private static boolean testProfileDisplay() {
        System.out.print("Testing profile display... ");
        boolean testPassed = true;
        
        // Create a test client
        Client client = new Client(100, "TestPlayer");
        client.giveItem(Game.Item.ASH);
        client.giveItem(Game.Item.BANDAGE);
        client.giveItem(Game.Item.BOTTLE);
        
        // Test profile display
        GameEngine.ProfileDisplay display = GameEngine.buildProfileDisplay(client);
        
        testPassed &= assertEquals(display != null, "Profile display should be created");
        testPassed &= assertEquals(display.message != null, "Profile message should not be null");
        testPassed &= assertEquals(display.buttons != null, "Profile buttons should not be null");
        testPassed &= assertEquals(display.message.contains("TestPlayer"), "Profile should contain player name");
        testPassed &= assertEquals(display.message.contains("Level:"), "Profile should contain level info");
        testPassed &= assertEquals(display.message.contains("Gesundheit:"), "Profile should contain health info");
        
        // Check that brewing options are available
        boolean hasBrewButton = false;
        for (String button : display.buttons) {
            if (button.contains("brauen")) {
                hasBrewButton = true;
                break;
            }
        }
        testPassed &= assertEquals(hasBrewButton, "Should have brewing button when ingredients available");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test fight preparation mechanics
     */
    private static boolean testFightPreparation() {
        System.out.print("Testing fight preparation... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create two test clients
        Client player1 = new Client(200, "Fighter1");
        Client player2 = new Client(300, "Fighter2");
        
        // Initially both should be IDLE
        testPassed &= assertEquals(player1.status == Client.Status.IDLE, "Player1 should start IDLE");
        testPassed &= assertEquals(player2.status == Client.Status.IDLE, "Player2 should start IDLE");
        
        // Prepare them to fight
        engine.prepareToFight(player1, player2);
        
        // Both should now be FIGHTING
        testPassed &= assertEquals(player1.status == Client.Status.FIGHTING, "Player1 should be FIGHTING after preparation");
        testPassed &= assertEquals(player2.status == Client.Status.FIGHTING, "Player2 should be FIGHTING after preparation");
        
        // They should be fighting each other
        testPassed &= assertEquals(player1.fightingChatId == player2.chatId, "Player1 should be fighting Player2");
        testPassed &= assertEquals(player2.fightingChatId == player1.chatId, "Player2 should be fighting Player1");
        
        if (testPassed) {
            System.out.println(" PASSED");
        } else {
            System.out.println(" FAILED");
        }
        
        return testPassed;
    }
}