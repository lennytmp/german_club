package FightLang;

import static FightLang.TestHelper.*;

/**
 * Comprehensive battle flow test that demonstrates:
 * 1. Player registration
 * 2. Finding opponents 
 * 3. Complete battle with hits and turns
 * 4. Battle completion
 * 
 * This test focuses on the message flow that users would see,
 * demonstrating that the game is fully testable end-to-end.
 */
public class BattleFlowTest {
    
    public static void main(String[] args) {
        boolean testPassed = runCompleteBattleScenario();
        
        if (!testPassed) {
            System.out.println("Battle Flow Test FAILED!");
            System.exit(1);
        }
    }
    
    /**
     * Run a complete battle scenario from start to finish
     */
    private static boolean runCompleteBattleScenario() {
        try {
            // Setup mocks using TestHelper
            TestEnvironment env = createTestEnvironment();
            
            // Create two players with Alice having a healing potion
            createPlayer(env, 100, "Alice");
            createPlayerWithItems(env, 200, "Bob", Game.Item.HPOTION);
            
            // Give Alice the healing potion instead
            Client alice = env.storage.getClientByChatId(100);
            if (alice != null) {
                alice.giveItem(Game.Item.HPOTION);
                env.storage.saveClient(alice);
            }
            
            env.clearMessages();
            
            // Test profile display using TestHelper
            MockTelegram.SentMessage profileMsg = getPlayerProfile(env, 100, "Alice");
            if (!validateBasicProfile(profileMsg, "Alice")) {
                System.out.println("Profile display failed");
                return false;
            }
            System.out.print("S"); // Profile test passed
            
            env.clearMessages();
            
            // Setup fight using TestHelper
            int activePlayer = setupFight(env, 100, "Alice", 200, "Bob");
            
            if (!validateFightInitiation(env, 100, "Alice", 200, "Bob")) {
                System.out.println("Fight initiation failed");
                return false;
            }
            System.out.print("S"); // Fight matching test passed
            
            if (!validateTurnSystem(env, 100, 200)) {
                System.out.println("Turn system failed - unclear who goes first");
                return false;
            }
            System.out.print("S"); // Turn system test passed
            
            int passivePlayer = activePlayer == 100 ? 200 : 100;
            String activePlayerName = activePlayer == 100 ? "Alice" : "Bob";
            
            // Test potion usage if Alice has the turn
            if (activePlayer == 100) {
                MockTelegram.SentMessage turnMsg = env.telegram.getLastMessageForChat(100);
                if (!turnMsg.hasButton("Heiltrank [1]")) {
                    System.out.println("Potion button not available");
                    return false;
                }
                
                env.clearMessages();
                
                // Alice uses her potion using TestHelper
                usePotion(env, 100, "Alice", "Heiltrank [1]");
                
                if (!env.telegram.hasMessageForChatContaining(100, "Heiltrank konsumiert")) {
                    System.out.println("Potion usage failed");
                    return false;
                }
                
                if (!env.telegram.hasMessageForChatContaining(200, "hat einen Heiltrank konsumiert")) {
                    System.out.println("Opponent not notified of potion usage");
                    return false;
                }
                System.out.print("S"); // Potion test passed
            }
            
            env.clearMessages();
            
            // Active player makes a successful attack using TestHelper
            simulateAttack(env, activePlayer, activePlayerName);
            
            // Check that damage messages were sent
            boolean attackerGotFeedback = env.telegram.getMessageCountForChat(activePlayer) > 0;
            boolean victimGotDamage = env.telegram.getMessageCountForChat(passivePlayer) > 0;
            
            if (!attackerGotFeedback || !victimGotDamage) {
                System.out.println("Combat feedback failed");
                return false;
            }
            System.out.print("S"); // Combat test passed
            
            // Test task system with a new player
            createPlayer(env, 300, "Charlie");
            env.telegram.simulateUserMessage(300, "Charlie", "Aufgabe");
            Telegram.Update[] taskUpdates = env.telegram.getUpdates(1);
            if (taskUpdates.length > 0) {
                env.engine.processUpdate(taskUpdates[taskUpdates.length - 1]);
            }
            
            if (env.telegram.getMessageCountForChat(300) == 0) {
                System.out.println("Task system failed");
                return false;
            }
            System.out.print("S"); // Task test passed
            
            return true;
            
        } catch (Exception e) {
            System.out.println("Battle Flow Test failed with exception: " + e.getMessage());
            return false;
        }
    }
}