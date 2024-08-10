package FightLang;

public class ClientTest {
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        allTestsPassed &= testNextExpLevel();
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

    public static boolean testNextExpLevel() {
        Client c =  new Client(0, "test");
        c.level = 1;
        boolean allTestsPassed = true;
        allTestsPassed &= assertEquals(30, c.nextExp(), "Expected experience for level 1 should be 30.");
        c.level = 2;
        allTestsPassed &= assertEquals(230, c.nextExp(), "Expected experience for level 2 should be 230.");
        c.level = 3;
        allTestsPassed &= assertEquals(530, c.nextExp(), "Expected experience for level 2 should be 530.");
        return allTestsPassed;
    }
}
