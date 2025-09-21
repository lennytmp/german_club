package FightLang;

import java.lang.reflect.Method;

public class DetailedCombatTest {
    public static void main(String[] args) {
        System.out.println("=== Detailed Combat Flow Test ===");
        testFullCombatFlow();
    }

    private static Object callPrivateMethod(String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer) {
                    paramTypes[i] = int.class;
                } else if (args[i] instanceof Boolean) {
                    paramTypes[i] = boolean.class;
                } else {
                    paramTypes[i] = args[i].getClass();
                }
            }
            
            Method method = Main.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (Exception e) {
            System.out.println("Error calling method " + methodName + ": " + e.getMessage());
            return null;
        }
    }

    private static int callGetDamageTask(Client client, boolean isSuccess) {
        try {
            Method method = Main.class.getDeclaredMethod("getDamageTask", Client.class, boolean.class);
            method.setAccessible(true);
            return (Integer) method.invoke(null, client, isSuccess);
        } catch (Exception e) {
            System.out.println("Error calling getDamageTask: " + e.getMessage());
            return -1;
        }
    }

    public static void testFullCombatFlow() {
        System.out.println("\n--- Testing Full Combat Flow ---");
        
        // Create a player
        Client player = new Client(100, "TestPlayer");
        System.out.println("Player created:");
        System.out.println("  Level: " + player.level);
        System.out.println("  Strength: " + player.strength);
        System.out.println("  Vitality: " + player.vitality);
        System.out.println("  Luck: " + player.luck);
        System.out.println("  HP: " + player.hp + "/" + player.getMaxHp());
        System.out.println("  Max Damage: " + player.getMaxDamage());
        
        // Create a bot opponent
        Client bot = new Client(-100, player);
        System.out.println("\nBot created:");
        System.out.println("  Name: " + bot.username);
        System.out.println("  Level: " + bot.level);
        System.out.println("  Strength: " + bot.strength);
        System.out.println("  Vitality: " + bot.vitality);
        System.out.println("  Luck: " + bot.luck);
        System.out.println("  HP: " + bot.hp + "/" + bot.getMaxHp());
        System.out.println("  Max Damage: " + bot.getMaxDamage());
        
        // Set up the fight
        player.status = Client.Status.FIGHTING;
        player.fightingChatId = bot.chatId;
        bot.status = Client.Status.FIGHTING;
        bot.fightingChatId = player.chatId;
        
        System.out.println("\n--- Combat Simulation ---");
        
        // Test damage calculation for both
        System.out.println("\nTesting damage calculation:");
        int playerDamageOnSuccess = callGetDamageTask(player, true);
        int playerDamageOnFail = callGetDamageTask(player, false);
        int botDamageOnSuccess = callGetDamageTask(bot, true);
        int botDamageOnFail = callGetDamageTask(bot, false);
        
        System.out.println("Player damage (success): " + playerDamageOnSuccess);
        System.out.println("Player damage (fail): " + playerDamageOnFail);
        System.out.println("Bot damage (success): " + botDamageOnSuccess);
        System.out.println("Bot damage (fail): " + botDamageOnFail);
        
        // Simulate player attacking bot (success)
        System.out.println("\n1. Player attacks bot (success):");
        int playerInitialHp = player.hp;
        int botInitialHp = bot.hp;
        System.out.println("   Before: Player HP=" + playerInitialHp + ", Bot HP=" + botInitialHp);
        
        callPrivateMethod("handleHitTask", player, bot, true);
        
        System.out.println("   After:  Player HP=" + player.hp + ", Bot HP=" + bot.hp);
        System.out.println("   Bot took " + (botInitialHp - bot.hp) + " damage");
        
        // Simulate bot counter-attack
        System.out.println("\n2. Bot counter-attacks player:");
        playerInitialHp = player.hp;
        botInitialHp = bot.hp;
        System.out.println("   Before: Player HP=" + playerInitialHp + ", Bot HP=" + botInitialHp);
        
        callPrivateMethod("handleHitTask", bot, player, true);
        
        System.out.println("   After:  Player HP=" + player.hp + ", Bot HP=" + bot.hp);
        System.out.println("   Player took " + (playerInitialHp - player.hp) + " damage");
        
        // Test the activateBotTask method directly
        System.out.println("\n3. Testing activateBotTask:");
        playerInitialHp = player.hp;
        System.out.println("   Before activateBotTask: Player HP=" + playerInitialHp);
        
        callPrivateMethod("activateBotTask", bot);
        
        System.out.println("   After activateBotTask:  Player HP=" + player.hp);
        System.out.println("   Player took " + (playerInitialHp - player.hp) + " damage from bot task");
        
        System.out.println("\n=== Test Complete ===");
    }
}