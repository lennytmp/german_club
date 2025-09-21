package FightLang;

import java.lang.reflect.Method;

public class CombatTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testDamageApplication();
        allTestsPassed &= testBotAttackingPlayer();
        allTestsPassed &= testPlayerAttackingBot();
        
        if (!allTestsPassed) {
            System.exit(1); 
        }
        System.out.println("All combat tests passed!");
    }

    // Helper method to call private methods using reflection
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
            e.printStackTrace();
            return null;
        }
    }

    public static boolean testDamageApplication() {
        System.out.print("Testing damage application... ");
        
        // Create two clients for testing
        Client attacker = new Client(1, "Attacker");
        Client victim = new Client(2, "Victim");
        
        // Set up attacker with some strength
        attacker.strength = 5;
        
        // Record victim's initial HP
        int initialHp = victim.hp;
        
        // Call makeHitTask with success = true (should do more damage)
        callPrivateMethod("makeHitTask", attacker, victim, true);
        
        // Check if victim took damage
        boolean tookDamage = victim.hp < initialHp;
        
        if (tookDamage) {
            System.out.println("PASSED - Victim took " + (initialHp - victim.hp) + " damage");
            return true;
        } else {
            System.out.println("FAILED - Victim did not take any damage. Initial HP: " + initialHp + ", Final HP: " + victim.hp);
            return false;
        }
    }

    public static boolean testBotAttackingPlayer() {
        System.out.print("Testing bot attacking player... ");
        
        // Create a bot and a player
        Client player = new Client(100, "Player");
        Client bot = new Client(-100, player); // Negative chatId indicates bot
        
        // Record player's initial HP
        int initialPlayerHp = player.hp;
        
        // Simulate bot attacking player
        callPrivateMethod("handleHitTask", bot, player, true);
        
        // Check if player took damage
        boolean playerTookDamage = player.hp < initialPlayerHp;
        
        if (playerTookDamage) {
            System.out.println("PASSED - Player took " + (initialPlayerHp - player.hp) + " damage from bot");
            return true;
        } else {
            System.out.println("FAILED - Player did not take damage from bot. Initial HP: " + initialPlayerHp + ", Final HP: " + player.hp);
            return false;
        }
    }

    public static boolean testPlayerAttackingBot() {
        System.out.print("Testing player attacking bot... ");
        
        // Create a player and a bot
        Client player = new Client(200, "Player");
        Client bot = new Client(-200, player); // Negative chatId indicates bot
        
        // Record bot's initial HP
        int initialBotHp = bot.hp;
        
        // Simulate player attacking bot
        callPrivateMethod("handleHitTask", player, bot, true);
        
        // Check if bot took damage
        boolean botTookDamage = bot.hp < initialBotHp;
        
        if (botTookDamage) {
            System.out.println("PASSED - Bot took " + (initialBotHp - bot.hp) + " damage from player");
            return true;
        } else {
            System.out.println("FAILED - Bot did not take damage from player. Initial HP: " + initialBotHp + ", Final HP: " + bot.hp);
            return false;
        }
    }
}