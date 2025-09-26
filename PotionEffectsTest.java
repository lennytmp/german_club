package FightLang;

public class PotionEffectsTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testBasicPotionEffects();
        allTestsPassed &= testPotionStacking();
        allTestsPassed &= testPotionExpiration();
        allTestsPassed &= testPotionPersistence();
        allTestsPassed &= testGameEngineIntegration();
        allTestsPassed &= testTimeFormatting();
        allTestsPassed &= testRemainingTimeCalculation();
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println();
    }

    // A simple assertEquals function to compare expected and actual values
    public static boolean assertEquals(int expected, int actual, String testName) {
        if (expected == actual) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected " + expected + " but got " + actual);
        return false;
    }

    public static boolean assertEquals(boolean expected, boolean actual, String testName) {
        if (expected == actual) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected " + expected + " but got " + actual);
        return false;
    }

    public static boolean assertEquals(String expected, String actual, String testName) {
        if (expected.equals(actual)) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED: expected \"" + expected + "\" but got \"" + actual + "\"");
        return false;
    }

    public static boolean testBasicPotionEffects() {
        Client client = new Client(1, "TestUser");
        int currentTime = 1000;
        boolean allTestsPassed = true;

        // Test initial state
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Initial effective strength should equal base strength");
        allTestsPassed &= assertEquals(client.luck, client.getEffectiveLuck(), "Initial effective luck should equal base luck");
        allTestsPassed &= assertEquals(false, client.hasActivePotionEffects(), "Should have no active effects initially");

        // Add strength effect
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Effective strength should increase by " + Game.STRENGTH_POTION_BONUS);
        allTestsPassed &= assertEquals(client.luck, client.getEffectiveLuck(), "Luck should remain unchanged");
        allTestsPassed &= assertEquals(true, client.hasActivePotionEffects(), "Should have active effects");

        // Add luck effect
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime);
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength should remain boosted");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Effective luck should increase by " + Game.LUCK_POTION_BONUS);

        return allTestsPassed;
    }

    public static boolean testPotionStacking() {
        Client client = new Client(2, "StackTestUser");
        int currentTime = 1000;
        boolean allTestsPassed = true;

        // Add first strength potion
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "First strength potion effect");

        // Add second strength potion
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime + 10);
        allTestsPassed &= assertEquals(client.strength + (Game.STRENGTH_POTION_BONUS * 2), client.getEffectiveStrength(), "Two strength potions should stack");

        // Add third strength potion
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime + 20);
        allTestsPassed &= assertEquals(client.strength + (Game.STRENGTH_POTION_BONUS * 3), client.getEffectiveStrength(), "Three strength potions should stack");

        // Add multiple luck potions
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime);
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime + 30);
        allTestsPassed &= assertEquals(client.luck + (Game.LUCK_POTION_BONUS * 2), client.getEffectiveLuck(), "Two luck potions should stack");

        return allTestsPassed;
    }

    public static boolean testPotionExpiration() {
        Client client = new Client(3, "ExpirationTestUser");
        int currentTime = 1000;
        boolean allTestsPassed = true;

        // Add effects with known expiration times
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);      // expires at 1180
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime + 60);     // expires at 1240

        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength effect should be active");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should be active");
        allTestsPassed &= assertEquals(true, client.hasActivePotionEffects(), "Should have active effects");

        // Advance time to 1190 (past strength effect expiration)
        client.removeExpiredPotionEffects(1190);
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Strength effect should be expired");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should remain");
        allTestsPassed &= assertEquals(true, client.hasActivePotionEffects(), "Should still have luck effect");

        // Advance time to 1250 (past luck effect expiration)
        client.removeExpiredPotionEffects(1250);
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Strength effect should remain expired");
        allTestsPassed &= assertEquals(client.luck, client.getEffectiveLuck(), "Luck effect should be expired");
        allTestsPassed &= assertEquals(false, client.hasActivePotionEffects(), "Should have no active effects");

        return allTestsPassed;
    }

    public static boolean testPotionPersistence() {
        // Test that potion effects survive save/load cycles
        MockStorage storage = new MockStorage();
        boolean allTestsPassed = true;

        // Create client with potion effects
        Client client = new Client(100, "PersistUser");
        client.setStorage(storage);
        int currentTime = 1000;

        // Apply potion effects
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime);

        // Verify effects are active
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength effect should be active before save");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should be active before save");

        // Save client to storage
        storage.saveClient(client);

        // Load client from storage (simulating restart)
        Client loadedClient = storage.getClientByChatId(100);
        loadedClient.setStorage(storage);

        // Verify effects persist after load
        allTestsPassed &= assertEquals(loadedClient.strength + Game.STRENGTH_POTION_BONUS, loadedClient.getEffectiveStrength(), "Strength effect should persist after load");
        allTestsPassed &= assertEquals(loadedClient.luck + Game.LUCK_POTION_BONUS, loadedClient.getEffectiveLuck(), "Luck effect should persist after load");
        allTestsPassed &= assertEquals(true, loadedClient.hasActivePotionEffects(), "Should have active effects after load");

        // Verify timestamp fields are preserved
        allTestsPassed &= assertEquals(currentTime + 180, loadedClient.strengthPotionExpiry, "Strength expiry timestamp should be preserved");
        allTestsPassed &= assertEquals(currentTime + 180, loadedClient.luckPotionExpiry, "Luck expiry timestamp should be preserved");
        allTestsPassed &= assertEquals(Game.STRENGTH_POTION_BONUS, loadedClient.strengthPotionBonus, "Strength bonus should be preserved");
        allTestsPassed &= assertEquals(Game.LUCK_POTION_BONUS, loadedClient.luckPotionBonus, "Luck bonus should be preserved");

        return allTestsPassed;
    }

    public static boolean testGameEngineIntegration() {
        // Test that the GameEngine correctly handles potion consumption
        MockStorage storage = new MockStorage();
        MockTelegram telegram = new MockTelegram();
        boolean allTestsPassed = true;

        // Create client with potions
        Client client = new Client(400, "EngineUser");
        client.setStorage(storage);
        client.giveItem(Game.Item.SPOTION);
        client.giveItem(Game.Item.LPOTION);
        int initialStrength = client.strength;
        int initialLuck = client.luck;
        storage.saveClient(client);

        // Create GameEngine and simulate potion consumption
        GameEngine engine = new GameEngine(storage, telegram);

        // Simulate strength potion consumption
        telegram.simulateUserMessage(400, "EngineUser", "/stärketrank");
        Telegram.Update[] updates = telegram.getUpdates(1);
        if (updates.length > 0) {
            engine.processUpdate(updates[updates.length - 1]);
        }

        // Verify effect was applied
        Client updatedClient = storage.getClientByChatId(400);
        if (updatedClient != null) {
            allTestsPassed &= assertEquals(initialStrength + Game.STRENGTH_POTION_BONUS, updatedClient.getEffectiveStrength(), "Strength should be boosted after potion");
            allTestsPassed &= assertEquals(Game.STRENGTH_POTION_BONUS, updatedClient.strengthPotionBonus, "Strength bonus should be " + Game.STRENGTH_POTION_BONUS);
            allTestsPassed &= assertEquals(0, updatedClient.getItemNum(Game.Item.SPOTION), "Strength potion should be consumed");
        } else {
            System.out.println(" Test: GameEngine strength potion consumption FAILED: client not found");
            allTestsPassed = false;
        }

        // Test luck potion consumption  
        telegram.clearUpdates();
        telegram.simulateUserMessage(400, "EngineUser", "/glückstrank");
        updates = telegram.getUpdates(1);
        if (updates.length > 0) {
            engine.processUpdate(updates[updates.length - 1]);
        }

        // Verify luck effect was applied
        updatedClient = storage.getClientByChatId(400);
        if (updatedClient != null) {
            allTestsPassed &= assertEquals(initialLuck + Game.LUCK_POTION_BONUS, updatedClient.getEffectiveLuck(), "Luck should be boosted after potion");
            allTestsPassed &= assertEquals(Game.LUCK_POTION_BONUS, updatedClient.luckPotionBonus, "Luck bonus should be " + Game.LUCK_POTION_BONUS);
            allTestsPassed &= assertEquals(0, updatedClient.getItemNum(Game.Item.LPOTION), "Luck potion should be consumed");
        } else {
            System.out.println(" Test: GameEngine luck potion consumption FAILED: client not found");
            allTestsPassed = false;
        }

        // Test background cleanup with no expiry
        engine.runBackgroundTasks();
        updatedClient = storage.getClientByChatId(400);
        if (updatedClient != null) {
            allTestsPassed &= assertEquals(initialStrength + Game.STRENGTH_POTION_BONUS, updatedClient.getEffectiveStrength(), "Effect should still be active after background tasks");
            allTestsPassed &= assertEquals(initialLuck + Game.LUCK_POTION_BONUS, updatedClient.getEffectiveLuck(), "Luck effect should still be active after background tasks");
        } else {
            System.out.println(" Test: Background cleanup preservation FAILED: client not found");
            allTestsPassed = false;
        }

        return allTestsPassed;
    }

    public static boolean testTimeFormatting() {
        boolean allTestsPassed = true;
        
        // Test formatting of different time durations
        allTestsPassed &= assertEquals("0s", Client.formatTimeRemaining(0), "Zero seconds");
        allTestsPassed &= assertEquals("5s", Client.formatTimeRemaining(5), "5 seconds");
        allTestsPassed &= assertEquals("1m", Client.formatTimeRemaining(60), "1 minute");
        allTestsPassed &= assertEquals("1m30s", Client.formatTimeRemaining(90), "1 minute 30 seconds");
        allTestsPassed &= assertEquals("2m", Client.formatTimeRemaining(120), "2 minutes");
        allTestsPassed &= assertEquals("3m", Client.formatTimeRemaining(180), "3 minutes");
        allTestsPassed &= assertEquals("2m32s", Client.formatTimeRemaining(152), "2 minutes 32 seconds");
        
        return allTestsPassed;
    }

    public static boolean testRemainingTimeCalculation() {
        Client client = new Client(5, "TimeTestUser");
        int currentTime = 1000;
        boolean allTestsPassed = true;
        
        // Test when no effects are active
        allTestsPassed &= assertEquals(0, client.getStrengthPotionRemainingTime(currentTime), "No strength effect remaining time");
        allTestsPassed &= assertEquals(0, client.getLuckPotionRemainingTime(currentTime), "No luck effect remaining time");
        
        // Add effects
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime + 30);
        
        // Test remaining time calculations
        allTestsPassed &= assertEquals(180, client.getStrengthPotionRemainingTime(currentTime), "Strength effect remaining time at start");
        allTestsPassed &= assertEquals(180, client.getLuckPotionRemainingTime(currentTime + 30), "Luck effect remaining time at start");
        
        // Test after some time has passed
        allTestsPassed &= assertEquals(120, client.getStrengthPotionRemainingTime(currentTime + 60), "Strength effect remaining time after 60s");
        allTestsPassed &= assertEquals(150, client.getLuckPotionRemainingTime(currentTime + 60), "Luck effect remaining time after 60s");
        
        // Test when effects have expired
        allTestsPassed &= assertEquals(0, client.getStrengthPotionRemainingTime(currentTime + 200), "Strength effect expired");
        allTestsPassed &= assertEquals(0, client.getLuckPotionRemainingTime(currentTime + 250), "Luck effect expired");
        
        return allTestsPassed;
    }
}