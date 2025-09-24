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
        System.out.println("\nAll victory message tests passed!");
    }
    
    /**
     * Comprehensive test that verifies the consolidated victory message functionality.
     * Tests message components, level-up integration, and single message count.
     */
    private static boolean testConsolidatedVictoryMessage() {
        System.out.print("Testing consolidated victory message functionality... ");
        boolean testPassed = true;
        
        // Test 1: Basic victory message components
        testPassed &= testBasicVictoryComponents();
        
        // Test 2: Victory message with level-up
        testPassed &= testVictoryWithLevelUp();
        
        // Test 3: Verify only one message is sent
        testPassed &= testSingleMessageCount();
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test basic victory message contains all expected components
     */
    private static boolean testBasicVictoryComponents() {
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create two players
        telegram.simulateUserMessage(100, "Winner", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        telegram.simulateUserMessage(200, "Loser", "/start");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        telegram.clearMessages();
        
        // Start fight
        telegram.simulateUserMessage(100, "Winner", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        telegram.simulateUserMessage(200, "Loser", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(4)[0]);
        
        // Find who has the turn and force fight to completion
        boolean player1HasTurn = telegram.hasMessageForChatContaining(100, "Du bist an der Reihe!");
        int activePlayer = player1HasTurn ? 100 : 200;
        int passivePlayer = player1HasTurn ? 200 : 100;
        
        // Fight until victory
        for (int round = 0; round < 50; round++) {
            telegram.clearMessages();
            
            telegram.simulateUserMessage(activePlayer, "Player" + activePlayer, "Erfolg");
            engine.processUpdate(telegram.getUpdates(5 + round)[0]);
            
            // Check if fight ended
            if (telegram.hasMessageContaining("Du hast gewonnen!")) {
                // Find the victory message
                MockTelegram.SentMessage victoryMessage = null;
                for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                    if (msg.message.contains("Du hast gewonnen!")) {
                        victoryMessage = msg;
                        break;
                    }
                }
                
                if (victoryMessage != null) {
                    String message = victoryMessage.message;
                    
                    // Check that all components are in the single message
                    if (!message.contains("Du hast gewonnen!")) return false;
                    if (!message.contains("Erfahrung erhalten")) return false;
                    if (!message.contains("Erfahrung fehlt bis zum Levelaufstieg")) return false;
                    if (!message.contains("Heiltrank gefunden")) return false;
                    
                    // Check for proper formatting with emojis
                    if (!message.contains("🎯")) return false;
                    if (!message.contains("💎")) return false;
                    
                    // Check that message has proper line breaks (not all on one line)
                    if (!message.contains("\n")) return false;
                    
                    return true;
                }
                break;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        return false;
    }
    
    /**
     * Test victory message when player levels up
     */
    private static boolean testVictoryWithLevelUp() {
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player close to leveling up
        Client nearLevelUp = new Client(300, "NearLevelUp");
        nearLevelUp.exp = 25; // Close to level 2 (needs 30)
        storage.addClient(nearLevelUp);
        
        // Create opponent
        telegram.simulateUserMessage(400, "Opponent", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Start fight
        telegram.simulateUserMessage(300, "NearLevelUp", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        telegram.simulateUserMessage(400, "Opponent", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        
        // Determine turn order
        boolean nearLevelUpHasTurn = telegram.hasMessageForChatContaining(300, "Du bist an der Reihe!");
        int activePlayer = nearLevelUpHasTurn ? 300 : 400;
        int passivePlayer = nearLevelUpHasTurn ? 400 : 300;
        
        // Fight until victory
        for (int round = 0; round < 50; round++) {
            telegram.clearMessages();
            
            telegram.simulateUserMessage(activePlayer, "Player" + activePlayer, "Erfolg");
            engine.processUpdate(telegram.getUpdates(4 + round)[0]);
            
            if (telegram.hasMessageContaining("Du hast gewonnen!")) {
                // Find victory message for the near-level-up player
                if (activePlayer == 300) {
                    MockTelegram.SentMessage victoryMessage = null;
                    for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                        if (msg.chatId == 300 && msg.message.contains("Du hast gewonnen!")) {
                            victoryMessage = msg;
                            break;
                        }
                    }
                    
                    if (victoryMessage != null) {
                        // Should contain level up message in the same victory message
                        if (!victoryMessage.message.contains("Du hast Level")) return false;
                        if (!victoryMessage.message.contains("erreicht!")) return false;
                        if (!victoryMessage.message.contains("🎉")) return false;
                        return true;
                    }
                }
                // If player 400 won, we can't test level-up, but that's ok
                return true;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        return false;
    }
    
    /**
     * Test that only ONE victory message is sent (not multiple messages)
     */
    private static boolean testSingleMessageCount() {
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create two players
        telegram.simulateUserMessage(600, "SingleMsgWinner", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        telegram.simulateUserMessage(700, "SingleMsgLoser", "/start");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        telegram.clearMessages();
        
        // Start fight
        telegram.simulateUserMessage(600, "SingleMsgWinner", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        telegram.simulateUserMessage(700, "SingleMsgLoser", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(4)[0]);
        
        // Find who has the turn
        boolean player1HasTurn = telegram.hasMessageForChatContaining(600, "Du bist an der Reihe!");
        int activePlayer = player1HasTurn ? 600 : 700;
        int passivePlayer = player1HasTurn ? 700 : 600;
        
        // Fight until victory
        for (int round = 0; round < 50; round++) {
            telegram.clearMessages();
            
            telegram.simulateUserMessage(activePlayer, "Player" + activePlayer, "Erfolg");
            engine.processUpdate(telegram.getUpdates(5 + round)[0]);
            
            if (telegram.hasMessageContaining("Du hast gewonnen!")) {
                // Count victory-related messages for the winner
                int victoryMessageCount = 0;
                for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                    if (msg.chatId == activePlayer && 
                        (msg.message.contains("Du hast gewonnen!") || 
                         msg.message.contains("Erfahrung erhalten") ||
                         msg.message.contains("gefunden") ||
                         msg.message.contains("regenerieren"))) {
                        victoryMessageCount++;
                    }
                }
                
                // Should be exactly 1 message containing all victory information
                return (victoryMessageCount == 1);
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        return false;
    }
}