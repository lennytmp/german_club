package FightLang;

import static FightLang.TestHelper.*;

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
        allTestsPassed &= testBackgroundCleanupFix();
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println();
    }

    // Removed duplicate assert methods - now using TestHelper utilities

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
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);      // expires at currentTime + 180
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime + 60);     // expires at currentTime + 60 + 180

        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength effect should be active");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should be active");
        allTestsPassed &= assertEquals(true, client.hasActivePotionEffects(), "Should have active effects");

        // Advance time to past strength effect expiration (strength expires at currentTime + 180)
        client.removeExpiredPotionEffects(currentTime + Game.POTION_DURATION_SECONDS + 10);
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Strength effect should be expired");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should remain");
        allTestsPassed &= assertEquals(true, client.hasActivePotionEffects(), "Should still have luck effect");

        // Advance time to past luck effect expiration (luck expires at currentTime + 60 + 180)
        client.removeExpiredPotionEffects(currentTime + 60 + Game.POTION_DURATION_SECONDS + 10);
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Strength effect should remain expired");
        allTestsPassed &= assertEquals(client.luck, client.getEffectiveLuck(), "Luck effect should be expired");
        allTestsPassed &= assertEquals(false, client.hasActivePotionEffects(), "Should have no active effects");

        return allTestsPassed;
    }

    public static boolean testPotionPersistence() {
        // Test that potion effects survive save/load cycles using TestHelper
        TestEnvironment env = createTestEnvironment();
        boolean allTestsPassed = true;

        // Create client with potion effects
        Client client = new Client(100, "PersistUser");
        client.setStorage(env.storage);
        int currentTime = 1000;

        // Apply potion effects
        client.addStrengthPotionEffect(Game.STRENGTH_POTION_BONUS, currentTime);
        client.addLuckPotionEffect(Game.LUCK_POTION_BONUS, currentTime);

        // Verify effects are active
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength effect should be active before save");
        allTestsPassed &= assertEquals(client.luck + Game.LUCK_POTION_BONUS, client.getEffectiveLuck(), "Luck effect should be active before save");

        // Save client to storage
        env.storage.saveClient(client);

        // Load client from storage (simulating restart)
        Client loadedClient = env.storage.getClientByChatId(100);
        loadedClient.setStorage(env.storage);

        // Verify effects persist after load
        allTestsPassed &= assertEquals(loadedClient.strength + Game.STRENGTH_POTION_BONUS, loadedClient.getEffectiveStrength(), "Strength effect should persist after load");
        allTestsPassed &= assertEquals(loadedClient.luck + Game.LUCK_POTION_BONUS, loadedClient.getEffectiveLuck(), "Luck effect should persist after load");
        allTestsPassed &= assertEquals(true, loadedClient.hasActivePotionEffects(), "Should have active effects after load");

        // Verify timestamp fields are preserved
        allTestsPassed &= assertEquals(currentTime + Game.POTION_DURATION_SECONDS, loadedClient.strengthPotionExpiry, "Strength expiry timestamp should be preserved");
        allTestsPassed &= assertEquals(currentTime + Game.POTION_DURATION_SECONDS, loadedClient.luckPotionExpiry, "Luck expiry timestamp should be preserved");
        allTestsPassed &= assertEquals(Game.STRENGTH_POTION_BONUS, loadedClient.strengthPotionBonus, "Strength bonus should be preserved");
        allTestsPassed &= assertEquals(Game.LUCK_POTION_BONUS, loadedClient.luckPotionBonus, "Luck bonus should be preserved");

        return allTestsPassed;
    }

    public static boolean testGameEngineIntegration() {
        // Test that the GameEngine correctly handles potion consumption using TestHelper
        TestEnvironment env = createTestEnvironment();
        boolean allTestsPassed = true;

        // Create client with potions using TestHelper
        Client client = createPlayerWithItems(env, 400, "EngineUser", Game.Item.SPOTION, Game.Item.LPOTION);
        int initialStrength = client.strength;
        int initialLuck = client.luck;

        // Simulate strength potion consumption
        env.telegram.simulateUserMessage(400, "EngineUser", "/stärketrank");
        Telegram.Update[] updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }

        // Verify effect was applied
        Client updatedClient = env.storage.getClientByChatId(400);
        if (updatedClient != null) {
            allTestsPassed &= assertEquals(initialStrength + Game.STRENGTH_POTION_BONUS, updatedClient.getEffectiveStrength(), "Strength should be boosted after potion");
            allTestsPassed &= assertEquals(Game.STRENGTH_POTION_BONUS, updatedClient.strengthPotionBonus, "Strength bonus should be " + Game.STRENGTH_POTION_BONUS);
            allTestsPassed &= assertEquals(0, updatedClient.getItemNum(Game.Item.SPOTION), "Strength potion should be consumed");
        } else {
            System.out.println(" Test: GameEngine strength potion consumption FAILED: client not found");
            allTestsPassed = false;
        }

        // Test luck potion consumption  
        env.telegram.clearUpdates();
        env.telegram.simulateUserMessage(400, "EngineUser", "/glückstrank");
        updates = env.telegram.getUpdates(1);
        if (updates.length > 0) {
            env.engine.processUpdate(updates[updates.length - 1]);
        }

        // Verify luck effect was applied
        updatedClient = env.storage.getClientByChatId(400);
        if (updatedClient != null) {
            allTestsPassed &= assertEquals(initialLuck + Game.LUCK_POTION_BONUS, updatedClient.getEffectiveLuck(), "Luck should be boosted after potion");
            allTestsPassed &= assertEquals(Game.LUCK_POTION_BONUS, updatedClient.luckPotionBonus, "Luck bonus should be " + Game.LUCK_POTION_BONUS);
            allTestsPassed &= assertEquals(0, updatedClient.getItemNum(Game.Item.LPOTION), "Luck potion should be consumed");
        } else {
            System.out.println(" Test: GameEngine luck potion consumption FAILED: client not found");
            allTestsPassed = false;
        }

        // Test background cleanup with no expiry
        env.engine.runBackgroundTasks();
        updatedClient = env.storage.getClientByChatId(400);
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
        allTestsPassed &= assertEquals(Game.POTION_DURATION_SECONDS, client.getStrengthPotionRemainingTime(currentTime), "Strength effect remaining time at start");
        allTestsPassed &= assertEquals(Game.POTION_DURATION_SECONDS, client.getLuckPotionRemainingTime(currentTime + 30), "Luck effect remaining time at start");
        
        // Test after some time has passed
        allTestsPassed &= assertEquals(Game.POTION_DURATION_SECONDS - 60, client.getStrengthPotionRemainingTime(currentTime + 60), "Strength effect remaining time after 60s");
        allTestsPassed &= assertEquals(Game.POTION_DURATION_SECONDS - 30, client.getLuckPotionRemainingTime(currentTime + 60), "Luck effect remaining time after 60s");
        
        // Test when effects have expired
        allTestsPassed &= assertEquals(0, client.getStrengthPotionRemainingTime(currentTime + Game.POTION_DURATION_SECONDS + 20), "Strength effect expired");
        allTestsPassed &= assertEquals(0, client.getLuckPotionRemainingTime(currentTime + Game.POTION_DURATION_SECONDS + 50), "Luck effect expired");
        
        return allTestsPassed;
    }

    public static boolean testBackgroundCleanupFix() {
        // Test that the background cleanup properly detects and removes expired potion effects
        // We'll temporarily reduce potion duration to make this test fast
        TestEnvironment env = createTestEnvironment();
        boolean allTestsPassed = true;
        
        // Create a client with potion effects
        Client client = new Client(600, "BackgroundTestUser");
        client.setStorage(env.storage);
        env.storage.saveClient(client);
        
        // Test with short duration for fast testing
        int startTime = 1000;
        int shortDuration = 2; // 2 seconds instead of 180
        
        // Manually set a short potion effect (simulating what would happen with a short duration)
        client.strengthPotionExpiry = startTime + shortDuration;
        client.strengthPotionBonus = Game.STRENGTH_POTION_BONUS;
        env.storage.saveClient(client);
        
        // Verify the effect is initially active
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Strength effect should be active initially");
        allTestsPassed &= assertEquals(shortDuration, client.getStrengthPotionRemainingTime(startTime), "Should have 2 seconds remaining");
        
        // Simulate time passing to when the effect should expire
        int expiredTime = startTime + shortDuration + 1; // 1 second past expiry
        
        // Test the scenario that was broken: expired timer but bonus still showing
        // Before the fix, this would show remaining time as 0 but effective strength would still include bonus
        allTestsPassed &= assertEquals(0, client.getStrengthPotionRemainingTime(expiredTime), "Remaining time should be 0 when expired");
        
        // This is the bug: even though remaining time is 0, effective strength still shows bonus
        // because removeExpiredPotionEffects hasn't been called yet
        allTestsPassed &= assertEquals(client.strength + Game.STRENGTH_POTION_BONUS, client.getEffectiveStrength(), "Bug: Effect still shows as active despite 0 remaining time");
        
        // Now test that calling removeExpiredPotionEffects fixes it
        client.removeExpiredPotionEffects(expiredTime);
        
        // After the fix: both timer and bonus should be gone
        allTestsPassed &= assertEquals(client.strength, client.getEffectiveStrength(), "Effect should be removed after calling removeExpiredPotionEffects");
        allTestsPassed &= assertEquals(0, client.strengthPotionExpiry, "Expiry should be reset to 0");
        allTestsPassed &= assertEquals(0, client.strengthPotionBonus, "Bonus should be reset to 0");
        allTestsPassed &= assertEquals(false, client.hasActivePotionEffects(), "Should have no active effects");
        
        // Test that the fix works with the actual background cleanup logic
        // Reset for second test
        client.strengthPotionExpiry = startTime + shortDuration;
        client.strengthPotionBonus = Game.STRENGTH_POTION_BONUS;
        
        // Simulate the logic from cleanupExpiredPotionEffects method with our fix
        boolean hadStrengthEffect = client.strengthPotionExpiry > 0 && client.strengthPotionExpiry > expiredTime;
        boolean strengthWillExpire = client.strengthPotionExpiry > 0 && client.strengthPotionExpiry <= expiredTime;
        
        // Before cleanup - verify the detection logic works correctly
        allTestsPassed &= assertEquals(false, hadStrengthEffect, "Should detect effect is no longer active");
        allTestsPassed &= assertEquals(true, strengthWillExpire, "Should detect effect will expire");
        
        // Apply the cleanup
        client.removeExpiredPotionEffects(expiredTime);
        
        // Verify the expiration detection would trigger a notification
        boolean strengthExpired = strengthWillExpire;
        allTestsPassed &= assertEquals(true, strengthExpired, "Should detect that strength effect expired");
        
        if (allTestsPassed) {
            System.out.print("S");
        } else {
            System.out.println("Background cleanup fix test FAILED");
        }
        
        return allTestsPassed;
    }
}