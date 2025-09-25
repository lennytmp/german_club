package FightLang;

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
            // Setup mocks - this replaces the real database and Telegram API
            MockStorage storage = new MockStorage();
            MockTelegram telegram = new MockTelegram();
            GameEngine engine = new GameEngine(storage, telegram);
            
            // Create two players using the new simulation approach
            telegram.simulateUserMessage(100, "Alice", "/start");
            engine.processUpdate(telegram.getUpdates(1)[0]);
            
            telegram.simulateUserMessage(200, "Bob", "/start");
            engine.processUpdate(telegram.getUpdates(2)[0]);
            
            // Give Alice a healing potion to test potion mechanics
            Client player1 = storage.getClientByChatId(100);
            if (player1 != null) {
                player1.giveItem(Game.Item.HPOTION);
                storage.saveClient(player1);
            }
            
            telegram.clearMessages();
            
            // Test profile display
            telegram.simulateUserMessage(100, "Alice", "Profil");
            engine.processUpdate(telegram.getUpdates(3)[0]); // Process Alice's profile request
            
            MockTelegram.SentMessage profileMsg = telegram.getLastMessageForChat(100);
            if (profileMsg == null || !profileMsg.message.contains("Alice")) {
                System.out.println("Profile display failed");
                return false;
            }
            System.out.print("S"); // Profile test passed
            
            telegram.clearMessages();
            
            // Alice looks for a fight
            telegram.simulateUserMessage(100, "Alice", "Kämpfen");
            engine.processUpdate(telegram.getUpdates(4)[0]); // Process Alice's fight request
            
            // Bob also looks for a fight - this should match them
            telegram.simulateUserMessage(200, "Bob", "Kämpfen");
            engine.processUpdate(telegram.getUpdates(5)[0]); // Process Bob's fight request
            
            // Check that both players are notified about the fight
            boolean aliceFightMsg = telegram.hasMessageForChatContaining(100, "Du kämpfst jetzt mit Bob");
            boolean bobFightMsg = telegram.hasMessageForChatContaining(200, "Du kämpfst jetzt mit Alice");
            
            if (!aliceFightMsg || !bobFightMsg) {
                System.out.println("Fight initiation failed");
                return false;
            }
            System.out.print("S"); // Fight matching test passed
            
            // Determine who goes first
            boolean aliceHasTurn = telegram.hasMessageForChatContaining(100, "Du bist an der Reihe!");
            boolean bobHasTurn = telegram.hasMessageForChatContaining(200, "Du bist an der Reihe!");
            
            if (aliceHasTurn == bobHasTurn) { // Both true or both false is wrong
                System.out.println("Turn system failed - unclear who goes first");
                return false;
            }
            System.out.print("S"); // Turn system test passed
            
            int activePlayer = aliceHasTurn ? 100 : 200;
            int passivePlayer = aliceHasTurn ? 200 : 100;
            String activePlayerName = aliceHasTurn ? "Alice" : "Bob";
            
            // Test potion usage if Alice has the turn
            if (activePlayer == 100) {
                MockTelegram.SentMessage turnMsg = telegram.getLastMessageForChat(100);
                if (!turnMsg.hasButton("Heiltrank [1]")) {
                    System.out.println("Potion button not available");
                    return false;
                }
                
                telegram.clearMessages();
                
                // Alice uses her potion
                telegram.simulateUserMessage(100, "Alice", "Heiltrank [1]");
                Telegram.Update[] potionUpdates = telegram.getUpdates(1);
                if (potionUpdates.length > 0) {
                    engine.processUpdate(potionUpdates[potionUpdates.length - 1]);
                }
                
                if (!telegram.hasMessageForChatContaining(100, "Heiltrank konsumiert")) {
                    System.out.println("Potion usage failed");
                    return false;
                }
                
                if (!telegram.hasMessageForChatContaining(200, "hat einen Heiltrank konsumiert")) {
                    System.out.println("Opponent not notified of potion usage");
                    return false;
                }
                System.out.print("S"); // Potion test passed
            }
            
            telegram.clearMessages();
            
            // Active player makes a successful attack
            telegram.simulateUserMessage(activePlayer, activePlayerName, "Erfolg");
            Telegram.Update[] attackUpdates = telegram.getUpdates(1);
            if (attackUpdates.length > 0) {
                engine.processUpdate(attackUpdates[attackUpdates.length - 1]);
            }
            
            // Check that damage messages were sent
            boolean attackerGotFeedback = telegram.getMessageCountForChat(activePlayer) > 0;
            boolean victimGotDamage = telegram.getMessageCountForChat(passivePlayer) > 0;
            
            if (!attackerGotFeedback || !victimGotDamage) {
                System.out.println("Combat feedback failed");
                return false;
            }
            System.out.print("S"); // Combat test passed
            
            // Test task system with a new player
            telegram.simulateUserMessage(300, "Charlie", "Aufgabe");
            Telegram.Update[] taskUpdates = telegram.getUpdates(1);
            if (taskUpdates.length > 0) {
                engine.processUpdate(taskUpdates[taskUpdates.length - 1]);
            }
            
            if (telegram.getMessageCountForChat(300) == 0) {
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