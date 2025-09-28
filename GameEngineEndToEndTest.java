package FightLang;

import java.util.List;
import static FightLang.TestHelper.*;

/**
 * End-to-end test that simulates the complete game flow:
 * 1. Player looks for an opponent
 * 2. Player makes hits during battle
 * 3. Battle finishes
 * 
 * This test only checks the telegram messages the user would receive,
 * without looking into any internal data structures.
 */
public class GameEngineEndToEndTest {
    
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        
        // Initialize required components for testing
        Phrases.initialize();
        
        allTestsPassed &= testCompleteGameFlow();
        allTestsPassed &= testPlayerVsPlayerFight();
        allTestsPassed &= testPlayerVsBotFight();
        allTestsPassed &= testFightWithPotionUsage();
        allTestsPassed &= testTaskAndItemFinding();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println("\nAll end-to-end tests passed!");
    }
    
    /**
     * Test the complete game flow from player registration to fight completion
     */
    private static boolean testCompleteGameFlow() {
        System.out.print("Testing complete game flow... ");
        boolean testPassed = true;
        
        // Setup mocks using TestHelper
        TestEnvironment env = createTestEnvironment();
        
        // Player 1 joins the game using TestHelper
        createPlayer(env, 100, "TestPlayer1");
        
        // Check welcome message
        testPassed &= env.telegram.hasMessageForChatContaining(100, "Willkommen im German Club!");
        testPassed &= env.telegram.getLastMessageForChat(100).hasButton("Kämpfen");
        testPassed &= env.telegram.getLastMessageForChat(100).hasButton("Profil");
        testPassed &= env.telegram.getLastMessageForChat(100).hasButton("Aufgabe");
        
        // Player 1 looks for fight
        env.clearMessages();
        env.telegram.simulateUserMessage(100, "TestPlayer1", "Kämpfen");
        env.engine.processUpdate(env.telegram.getUpdates(1)[0]);
        
        // Player should be looking for opponent (no fight started yet)
        testPassed &= env.telegram.getMessageCountForChat(100) > 0;
        
        // Player 2 joins and also looks for fight
        createPlayer(env, 200, "TestPlayer2");
        
        env.clearMessages();
        env.telegram.simulateUserMessage(200, "TestPlayer2", "Kämpfen");
        env.engine.processUpdate(env.telegram.getUpdates(1)[0]);
        
        // Now both players should be matched and fighting
        testPassed &= env.telegram.hasMessageForChatContaining(100, "Du kämpfst jetzt mit TestPlayer2");
        testPassed &= env.telegram.hasMessageForChatContaining(200, "Du kämpfst jetzt mit TestPlayer1");
        
        // One of them should be asked to make the first move
        boolean player1HasTurn = env.telegram.hasMessageForChatContaining(100, "Du bist an der Reihe!");
        boolean player2HasTurn = env.telegram.hasMessageForChatContaining(200, "Du bist an der Reihe!");
        testPassed &= (player1HasTurn || player2HasTurn) && !(player1HasTurn && player2HasTurn);
        
        // Determine who goes first and simulate their action
        int activePlayer = player1HasTurn ? 100 : 200;
        int passivePlayer = player1HasTurn ? 200 : 100;
        
        env.clearMessages();
        
        // Active player makes a successful hit
        simulateAttack(env, activePlayer, "TestPlayer" + (activePlayer == 100 ? "1" : "2"));
        
        // Check that damage was dealt and passive player gets to respond
        testPassed &= env.telegram.hasMessageForChatContaining(passivePlayer, "Du bist an der Reihe!");
        testPassed &= env.telegram.getMessageCountForChat(activePlayer) > 0; // Active player gets feedback
        testPassed &= env.telegram.getMessageCountForChat(passivePlayer) > 0; // Passive player gets damage message
        
        // Continue the fight until someone wins
        int maxRounds = 50; // Prevent infinite loops
        int currentPlayer = passivePlayer;
        int opponent = activePlayer;
        
        for (int round = 0; round < maxRounds; round++) {
            env.clearMessages();
            
            // Current player makes a successful hit
            simulateAttack(env, currentPlayer, "TestPlayer" + (currentPlayer == 100 ? "1" : "2"));
            
            // Check if fight is over (someone won)
            if (env.telegram.hasMessageContaining("Erfahrung erhalten") || 
                env.telegram.hasMessageContaining("Du wurdest im Kampf besiegt")) {
                testPassed &= true; // Fight ended properly
                break;
            }
            
            // Switch players
            int temp = currentPlayer;
            currentPlayer = opponent;
            opponent = temp;
            
            if (round == maxRounds - 1) {
                System.out.println("Fight didn't end in " + maxRounds + " rounds");
                testPassed &= false; // Fight didn't end in reasonable time
            }
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test player vs player fight mechanics
     */
    private static boolean testPlayerVsPlayerFight() {
        System.out.print("Testing player vs player fight... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create two players and start them fighting
        telegram.simulateUserMessage(300, "Fighter1", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        telegram.simulateUserMessage(400, "Fighter2", "/start");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        telegram.clearMessages();
        
        // Both players look for fight
        telegram.simulateUserMessage(300, "Fighter1", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        telegram.simulateUserMessage(400, "Fighter2", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(4)[0]);
        
        // Verify fight started
        testPassed &= telegram.hasMessageForChatContaining(300, "Du kämpfst jetzt mit Fighter2");
        testPassed &= telegram.hasMessageForChatContaining(400, "Du kämpfst jetzt mit Fighter1");
        
        // Verify stats are shown
        testPassed &= telegram.hasMessageForChatContaining(300, "Level:");
        testPassed &= telegram.hasMessageForChatContaining(400, "Level:");
        
        // Verify turn system works
        boolean fighter1HasTurn = telegram.hasMessageForChatContaining(300, "Du bist an der Reihe!");
        boolean fighter2HasTurn = telegram.hasMessageForChatContaining(400, "Du bist an der Reihe!");
        testPassed &= (fighter1HasTurn || fighter2HasTurn) && !(fighter1HasTurn && fighter2HasTurn);
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test player vs bot fight (when no other players available)
     */
    private static boolean testPlayerVsBotFight() {
        System.out.print("Testing player vs bot fight... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create one player
        telegram.simulateUserMessage(500, "SoloPlayer", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Player looks for fight
        telegram.simulateUserMessage(500, "SoloPlayer", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        
        // Player should be waiting for opponent initially
        testPassed &= telegram.getMessageCountForChat(500) > 0;
        
        // Simulate background task that assigns bot after timeout
        // We need to wait and then run background tasks
        try {
            Thread.sleep(11000); // Wait for bot assignment timeout (10+ seconds)
        } catch (InterruptedException e) {
            // Ignore
        }
        
        telegram.clearMessages();
        engine.runBackgroundTasks();
        
        // Player should now be fighting a bot
        testPassed &= telegram.hasMessageForChatContaining(500, "Du kämpfst jetzt mit");
        testPassed &= telegram.hasMessageForChatContaining(500, "Level:"); // Bot stats shown
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test fight with potion usage
     */
    private static boolean testFightWithPotionUsage() {
        System.out.print("Testing fight with potion usage... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player with a healing potion
        Client playerWithPotion = new Client(600, "PotionUser");
        playerWithPotion.giveItem(Game.Item.HPOTION);
        storage.addClient(playerWithPotion);
        
        // Create another player
        telegram.simulateUserMessage(700, "RegularPlayer", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Both look for fight
        telegram.simulateUserMessage(600, "PotionUser", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(2)[0]);
        telegram.simulateUserMessage(700, "RegularPlayer", "Kämpfen");
        engine.processUpdate(telegram.getUpdates(3)[0]);
        
        // Find who has the turn
        boolean potionUserHasTurn = telegram.hasMessageForChatContaining(600, "Du bist an der Reihe!");
        
        if (potionUserHasTurn) {
            // Check that potion option is available
            MockTelegram.SentMessage lastMsg = telegram.getLastMessageForChat(600);
            testPassed &= lastMsg.hasButton("Heiltrank [1]");
            
            telegram.clearMessages();
            
            // Use potion
            telegram.simulateUserMessage(600, "PotionUser", "Heiltrank [1]");
            engine.processUpdate(telegram.getUpdates(4)[0]);
            
            // Verify potion was consumed
            testPassed &= telegram.hasMessageForChatContaining(600, "Heiltrank konsumiert");
            testPassed &= telegram.hasMessageForChatContaining(700, "hat einen Heiltrank konsumiert");
        }
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
    
    /**
     * Test task completion and item finding
     */
    private static boolean testTaskAndItemFinding() {
        System.out.print("Testing task completion and item finding... ");
        boolean testPassed = true;
        
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        GameEngine engine = new GameEngine(storage, telegram);
        
        // Create a player
        telegram.simulateUserMessage(800, "TaskDoer", "/start");
        engine.processUpdate(telegram.getUpdates(1)[0]);
        
        telegram.clearMessages();
        
        // Player does a task multiple times to potentially find items
        for (int i = 0; i < 10; i++) {
            telegram.simulateUserMessage(800, "TaskDoer", "Aufgabe");
            engine.processUpdate(telegram.getUpdates(2 + i)[0]);
        }
        
        // Player should have received some responses (either items found or nothing found messages)
        testPassed &= telegram.getMessageCountForChat(800) >= 10;
        
        // Check profile to see inventory
        telegram.clearMessages();
        telegram.simulateUserMessage(800, "TaskDoer", "Profil");
        engine.processUpdate(telegram.getUpdates(12)[0]);
        
        // Profile should show player stats
        testPassed &= telegram.hasMessageForChatContaining(800, "Level:");
        testPassed &= telegram.hasMessageForChatContaining(800, "Gesundheit:");
        
        if (testPassed) {
            System.out.println("PASSED");
        } else {
            System.out.println("FAILED");
        }
        
        return testPassed;
    }
}