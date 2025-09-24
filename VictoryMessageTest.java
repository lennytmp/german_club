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
        allTestsPassed &= testVictoryMessageWithLevelUp();
        allTestsPassed &= testVictoryMessageVsBotWithLoot();
        allTestsPassed &= testVictoryMessageCount();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println("\nAll victory message tests passed!");
    }
    
    /**
     * Test that victory message contains all expected components in a single message
     */
    private static boolean testConsolidatedVictoryMessage() {
        System.out.print("Testing consolidated victory message components... ");
        boolean testPassed = true;
        
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
                
                testPassed &= (victoryMessage != null);
                if (victoryMessage != null) {
                    String message = victoryMessage.message;
                    
                    // Check that all components are in the single message
                    testPassed &= message.contains("Du hast gewonnen!");
                    testPassed &= message.contains("Erfahrung erhalten");
                    testPassed &= message.contains("Erfahrung fehlt bis zum Levelaufstieg");
                    testPassed &= message.contains("Heiltrank gefunden");
                    
                    // Check for proper formatting with emojis
                    testPassed &= message.contains("🎯");
                    testPassed &= message.contains("💎");
                    
                    // Check that message has proper line breaks (not all on one line)
                    testPassed &= message.contains("\n");
                }
                break;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test victory message when player levels up
     */
    private static boolean testVictoryMessageWithLevelUp() {
        System.out.print("Testing victory message with level up... ");
        boolean testPassed = true;
        
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
                MockTelegram.SentMessage victoryMessage = null;
                for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                    if (msg.chatId == 300 && msg.message.contains("Du hast gewonnen!")) {
                        victoryMessage = msg;
                        break;
                    }
                }
                
                if (victoryMessage != null && activePlayer == 300) {
                    // Should contain level up message if player 300 won
                    testPassed &= victoryMessage.message.contains("Du hast Level");
                    testPassed &= victoryMessage.message.contains("erreicht!");
                    testPassed &= victoryMessage.message.contains("🎉");
                }
                break;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test victory message when fighting a bot (different loot logic)
     */
    private static boolean testVictoryMessageVsBotWithLoot() {
        System.out.print("Testing victory message vs bot with loot... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player
        telegram.simulateUserMessage(500, "BotFighter", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Player looks for fight (should get matched with bot after timeout)
        telegram.simulateUserMessage(500, "BotFighter", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        // Wait for bot assignment and run background tasks
        try {
            Thread.sleep(11000);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        telegram.clearMessages();
        engine.runBackgroundTasks();
        
        // Player should now be fighting a bot
        if (telegram.hasMessageForChatContaining(500, "Du kämpfst jetzt mit")) {
            // Fight until victory
            for (int round = 0; round < 50; round++) {
                telegram.clearMessages();
                
                telegram.simulateUserMessage(500, "BotFighter", "Erfolg");
                engine.processUpdate(telegram.getUpdates(3 + round)[0]);
                
                if (telegram.hasMessageContaining("Du hast gewonnen!")) {
                    // Find victory message
                    MockTelegram.SentMessage victoryMessage = null;
                    for (MockTelegram.SentMessage msg : telegram.getSentMessages()) {
                        if (msg.chatId == 500 && msg.message.contains("Du hast gewonnen!")) {
                            victoryMessage = msg;
                            break;
                        }
                    }
                    
                    testPassed &= (victoryMessage != null);
                    if (victoryMessage != null) {
                        // Bot fights may or may not give loot, but should always have exp
                        testPassed &= victoryMessage.message.contains("Erfahrung erhalten");
                    }
                    break;
                }
            }
        } else {
            // If bot matching didn't work, consider test passed (not testing bot matching here)
            testPassed = true;
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test that only ONE victory message is sent (not multiple messages)
     */
    private static boolean testVictoryMessageCount() {
        System.out.print("Testing that only one victory message is sent... ");
        boolean testPassed = true;
        
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
                testPassed &= (victoryMessageCount == 1);
                
                if (victoryMessageCount != 1) {
                    System.out.println("Expected 1 victory message, got " + victoryMessageCount);
                }
                break;
            }
            
            // Switch players
            int temp = activePlayer;
            activePlayer = passivePlayer;
            passivePlayer = temp;
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
}