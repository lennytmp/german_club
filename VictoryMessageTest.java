package FightLang;

import java.util.List;

/**
 * Test that specifically verifies the consolidated victory message functionality.
 * This test ensures that when a player wins a fight, they receive a single
 * comprehensive message containing all victory information instead of multiple
 * separate messages.
 */
public class VictoryMessageTest {
    
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        
        // Initialize required components for testing
        Phrases.initialize();
        
        allTestsPassed &= testConsolidatedVictoryMessage();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
    }
    
    /**
     * Comprehensive test that verifies the consolidated victory message functionality.
     * Tests message components, level-up integration, and single message count in one test.
     */
    private static boolean testConsolidatedVictoryMessage() {
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player close to leveling up to test level-up integration
        Client nearLevelUp = new Client(100, "LevelUpWinner");
        nearLevelUp.exp = 25; // Close to level 2 (needs 30)
        storage.addClient(nearLevelUp);
        
        // Create opponent
        telegram.simulateUserMessage(200, "Opponent", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Start fight
        telegram.simulateUserMessage(100, "LevelUpWinner", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        telegram.simulateUserMessage(200, "Opponent", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        
        // Find who has the turn and force fight to completion
        boolean player1HasTurn = telegram.hasMessageForChatContaining(100, "Du bist an der Reihe!");
        int activePlayer = player1HasTurn ? 100 : 200;
        int passivePlayer = player1HasTurn ? 200 : 100;
        
        // Fight until victory
        for (int round = 0; round < 50; round++) {
            telegram.clearMessages();
            
            telegram.simulateUserMessage(activePlayer, "Player" + activePlayer, "Erfolg");
            engine.processUpdate(telegram.getUpdates(4 + round)[0]);
            
            // Check if fight ended (look for victory phrases or experience gained)
            if (telegram.hasMessageContaining("Erfahrung erhalten") || telegram.hasMessageContaining("hat") && telegram.hasMessageContaining("besiegt")) {
                // Find the victory message
                MockTelegram.SentMessage victoryMessage = null;
                for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                    if (msg.message.contains("Erfahrung erhalten")) {
                        victoryMessage = msg;
                        break;
                    }
                }
                
                if (victoryMessage != null) {
                    String message = victoryMessage.message;
                    
                    // Test 1: Check that all basic components are in the single message
                    testPassed &= (message.contains("hat") && message.contains("besiegt")) || message.contains("☠");
                    testPassed &= message.contains("Erfahrung erhalten");
                    testPassed &= message.contains("Erfahrung fehlt bis zum Levelaufstieg");
                    testPassed &= message.contains("Heiltrank gefunden");
                    
                    // Test 2: Check for proper formatting with emojis
                    testPassed &= message.contains("🎯");
                    testPassed &= message.contains("🎒");
                    
                    // Test 3: Check that message has proper line breaks (not all on one line)
                    testPassed &= message.contains("\n");
                    
                    // Test 4: If player 100 won (the near-level-up player), check level-up integration
                    if (victoryMessage.chatId == 100) {
                        testPassed &= message.contains("Du hast Level");
                        testPassed &= message.contains("erreicht!");
                        testPassed &= message.contains("🎉");
                    }
                }
                
                // Test 5: Count victory-related messages - should be exactly 1
                int victoryMessageCount = 0;
                for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                    if (msg.chatId == activePlayer && 
                        (msg.message.contains("Erfahrung erhalten") ||
                         msg.message.contains("gefunden") ||
                         msg.message.contains("regenerieren"))) {
                        victoryMessageCount++;
                    }
                }
                testPassed &= (victoryMessageCount == 1);
                
                break;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        if (testPassed) {
            System.out.print("S");
        } else {
            System.out.println("Victory message test FAILED");
        }
        
        return testPassed;
    }
}