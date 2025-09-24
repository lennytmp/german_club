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
        
        // First, let's determine all possible phrases by checking the JSON structure
        System.out.println("📋 All possible victory phrases according to combats.json:");
        String[] expectedPhrases = {
            "☠ Lenny hat gewonnen gegen Dämon.",
            "☠ Lenny hat Dämon besiegt.",
            "☠ Lenny hat Dämon überwunden."
        };
        
        for (String phrase : expectedPhrases) {
            System.out.println("   " + phrase);
        }
        System.out.println();
        
        // Now generate phrases exhaustively to ensure we see all variants
        System.out.println("🔄 Generating phrases to find all variants...");
        boolean[] phraseSeen = new boolean[expectedPhrases.length];
        boolean allTestsPassed = true;
        boolean specificPhraseFound = false;
        String targetPhrase = "☠ Lenny hat gewonnen gegen Dämon.";
        
        // Generate enough phrases to guarantee we see all variants (with high probability)
        // Since there are 3 possible phrases, generating 100 should be more than enough
        for (int i = 0; i < 100; i++) {
            String phrase = PhraseGenerator.getWonPhrase(winner, loser);
            
            // Check if this is our specific target phrase
            if (phrase.equals(targetPhrase)) {
                if (!specificPhraseFound) {
                    System.out.println("🎯 FOUND TARGET PHRASE: " + phrase);
                    specificPhraseFound = true;
                }
            }
            
            // Track which expected phrases we've seen
            for (int j = 0; j < expectedPhrases.length; j++) {
                if (phrase.equals(expectedPhrases[j])) {
                    if (!phraseSeen[j]) {
                        System.out.println("✅ Generated expected phrase: " + phrase);
                        phraseSeen[j] = true;
                    }
                    break;
                }
            }
            
            // Verify the phrase follows correct German grammar patterns
            if (!isGermanGrammarCorrect(phrase, "Lenny", "Dämon")) {
                System.err.println("❌ GRAMMAR ERROR: " + phrase);
                allTestsPassed = false;
            }
        }
        
        System.out.println();
        System.out.println("📊 Test Results:");
        System.out.println("================");
        
        // Check if we found the specific phrase
        if (specificPhraseFound) {
            System.out.println("✅ SPECIFIC PHRASE CONFIRMED: \"☠ Lenny hat gewonnen gegen Dämon.\"");
        } else {
            System.err.println("❌ SPECIFIC PHRASE NOT FOUND: \"☠ Lenny hat gewonnen gegen Dämon.\"");
            allTestsPassed = false;
        }
        
        // Check if we found all expected phrases
        boolean allPhrasesFound = true;
        for (int i = 0; i < expectedPhrases.length; i++) {
            if (phraseSeen[i]) {
                System.out.println("✅ Found: " + expectedPhrases[i]);
            } else {
                System.err.println("❌ Missing: " + expectedPhrases[i]);
                allPhrasesFound = false;
            }
        }
        
        if (allTestsPassed && allPhrasesFound && specificPhraseFound) {
            System.out.println("\n🎉 ALL TESTS PASSED!");
            System.out.println("   ✓ German grammar is correct");
            System.out.println("   ✓ All expected phrases generated");
            System.out.println("   ✓ Specific target phrase confirmed: \"☠ Lenny hat gewonnen gegen Dämon.\"");
        } else {
            System.out.println("\n💥 SOME TESTS FAILED!");
            if (!allTestsPassed) System.out.println("   ✗ Grammar issues detected");
            if (!allPhrasesFound) System.out.println("   ✗ Not all expected phrases found");
            if (!specificPhraseFound) System.out.println("   ✗ Target phrase not generated");
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