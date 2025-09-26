package FightLang;

import java.util.List;
import static FightLang.TestHelper.*;

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
        
        TestEnvironment env = createTestEnvironment();
        
        // Create a player close to leveling up to test level-up integration
        Client nearLevelUp = createPlayerWithStats(env, 100, "LevelUpWinner", 1, 25); // Close to level 2 (needs 30)
        
        // Create opponent using TestHelper
        createPlayer(env, 200, "Opponent");
        
        env.clearMessages();
        
        // Start fight using TestHelper
        int activePlayer = setupFight(env, 100, "LevelUpWinner", 200, "Opponent");
        int passivePlayer = activePlayer == 100 ? 200 : 100;
        
        // Fight until victory
        for (int round = 0; round < 50; round++) {
            env.clearMessages();
            
            simulateAttack(env, activePlayer, "Player" + activePlayer);
            
            // Check if fight ended (look for victory phrases or experience gained)
            if (env.telegram.hasMessageContaining("Erfahrung erhalten") || env.telegram.hasMessageContaining("hat") && env.telegram.hasMessageContaining("besiegt")) {
                // Find the victory message
                MockTelegram.SentMessage victoryMessage = null;
                for (MockTelegram.SentMessage msg : env.telegram.getSentMessages()) {
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
                for (MockTelegram.SentMessage msg : env.telegram.getSentMessages()) {
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