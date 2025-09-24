package FightLang;

/**
 * Test to verify that German grammar is correct in victory phrases.
 * Specifically tests that the word order follows German syntax rules.
 */
public class GermanGrammarTest {
    
    public static void main(String[] args) {
        // Initialize phrases system
        Phrases.initialize();
        
        // Create test clients
        Client winner = new Client(1, "Lenny");
        Client loser = new Client(2, "Dämon");
        
        System.out.println("=== German Grammar Test for Victory Phrases ===");
        System.out.println("Testing that German word order is correct...\n");
        
        // Test each victory phrase template multiple times to ensure all variants work
        boolean allTestsPassed = true;
        
        for (int i = 0; i < 20; i++) {
            String phrase = PhraseGenerator.getWonPhrase(winner, loser);
            System.out.println("Generated: " + phrase);
            
            // Verify the phrase follows correct German grammar patterns
            if (!isGermanGrammarCorrect(phrase, "Lenny", "Dämon")) {
                System.err.println("❌ GRAMMAR ERROR: " + phrase);
                allTestsPassed = false;
            } else {
                System.out.println("✅ Grammar correct");
            }
            System.out.println();
        }
        
        if (allTestsPassed) {
            System.out.println("🎉 ALL TESTS PASSED - German grammar is correct!");
        } else {
            System.out.println("💥 SOME TESTS FAILED - Grammar issues detected!");
            System.exit(1);
        }
    }
    
    /**
     * Verifies that a German victory phrase follows correct grammar rules.
     * 
     * Expected patterns:
     * - "☠ Lenny hat gewonnen gegen Dämon." (preposition 'gegen' before object)
     * - "☠ Lenny hat Dämon besiegt." (object before past participle)
     * - "☠ Lenny hat Dämon überwunden." (object before past participle)
     * 
     * Incorrect patterns to avoid:
     * - "☠ Lenny hat besiegt Dämon." (past participle before object)
     * - "☠ Lenny hat überwunden Dämon." (past participle before object)
     */
    private static boolean isGermanGrammarCorrect(String phrase, String winner, String loser) {
        // Remove the skull emoji and period for easier parsing
        String content = phrase.replace("☠ ", "").replace(".", "").trim();
        
        // Expected format: "Lenny hat ..."
        if (!content.startsWith(winner + " hat ")) {
            return false;
        }
        
        String afterHat = content.substring((winner + " hat ").length());
        
        // Check the three valid patterns
        if (afterHat.equals("gewonnen gegen " + loser)) {
            // Pattern: "hat gewonnen gegen Dämon" - correct with preposition
            return true;
        } else if (afterHat.equals(loser + " besiegt")) {
            // Pattern: "hat Dämon besiegt" - correct object before past participle
            return true;
        } else if (afterHat.equals(loser + " überwunden")) {
            // Pattern: "hat Dämon überwunden" - correct object before past participle
            return true;
        }
        
        // Check for incorrect patterns that would indicate grammar errors
        if (afterHat.equals("besiegt " + loser) || afterHat.equals("überwunden " + loser)) {
            System.err.println("DETECTED INCORRECT WORD ORDER: Past participle before object");
            return false;
        }
        
        // If we get here, it's an unexpected pattern
        System.err.println("UNEXPECTED PHRASE PATTERN: " + afterHat);
        return false;
    }
}