package FightLang;

/**
 * Test to verify that German grammar is correct in victory phrases.
 * Specifically tests that the word order follows German syntax rules.
 */
public class GermanGrammarTest {
    
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        
        // Initialize phrases system
        Phrases.initialize();
        
        allTestsPassed &= testGermanGrammarInVictoryPhrases();
        allTestsPassed &= testSpecificTargetPhrase();
        allTestsPassed &= testAllExpectedPhrasesGenerated();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
        System.out.println();
    }
    
    /**
     * Test that all generated victory phrases follow correct German grammar.
     */
    private static boolean testGermanGrammarInVictoryPhrases() {
        Client winner = new Client(1, "Lenny");
        Client loser = new Client(2, "Dämon");
        
        // Test grammar rules across multiple generations
        for (int i = 0; i < 50; i++) {
            String phrase = PhraseGenerator.getWonPhrase(winner, loser);
            if (!isGermanGrammarCorrect(phrase, "Lenny", "Dämon")) {
                System.out.println("German grammar test FAILED: " + phrase);
                return false;
            }
        }
        
        System.out.print("S");
        return true;
    }
    
    /**
     * Test that the specific target phrase "☠ Lenny hat gewonnen gegen Dämon." is generated.
     */
    private static boolean testSpecificTargetPhrase() {
        Client winner = new Client(1, "Lenny");
        Client loser = new Client(2, "Dämon");
        String targetPhrase = "☠ Lenny hat gewonnen gegen Dämon.";
        
        // Generate enough phrases to find the target (should appear within reasonable attempts)
        for (int i = 0; i < 100; i++) {
            String phrase = PhraseGenerator.getWonPhrase(winner, loser);
            if (phrase.equals(targetPhrase)) {
                System.out.print("S");
                return true;
            }
        }
        
        System.out.println("Target phrase test FAILED: \"" + targetPhrase + "\" not found");
        return false;
    }
    
    /**
     * Test that all expected phrases can be generated.
     */
    private static boolean testAllExpectedPhrasesGenerated() {
        Client winner = new Client(1, "Lenny");
        Client loser = new Client(2, "Dämon");
        
        String[] expectedPhrases = {
            "☠ Lenny hat gewonnen gegen Dämon.",
            "☠ Lenny hat Dämon besiegt.",
            "☠ Lenny hat Dämon überwunden."
        };
        
        boolean[] phraseSeen = new boolean[expectedPhrases.length];
        
        // Generate phrases until we see all expected ones
        for (int i = 0; i < 200; i++) {  // Increased iterations for safety
            String phrase = PhraseGenerator.getWonPhrase(winner, loser);
            
            for (int j = 0; j < expectedPhrases.length; j++) {
                if (phrase.equals(expectedPhrases[j])) {
                    phraseSeen[j] = true;
                    break;
                }
            }
            
            // Check if we've found all phrases
            boolean allFound = true;
            for (boolean seen : phraseSeen) {
                if (!seen) {
                    allFound = false;
                    break;
                }
            }
            if (allFound) {
                System.out.print("S");
                return true;
            }
        }
        
        // Report which phrases were missing
        for (int i = 0; i < expectedPhrases.length; i++) {
            if (!phraseSeen[i]) {
                System.out.println("All phrases test FAILED: Missing \"" + expectedPhrases[i] + "\"");
                return false;
            }
        }
        
        System.out.print("S");
        return true;
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
            return false;
        }
        
        // If we get here, it's an unexpected pattern
        return false;
    }
}