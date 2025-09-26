package FightLang;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import static FightLang.TestHelper.*;

/**
 * Test to prevent JSON escaping issues in Gemini API calls.
 * This test validates the actual JSON creation logic used by the Gemini.AskGemini method.
 */
public class GeminiJsonTest {
    
    public static void main(String[] args) {
        boolean allTestsPassed = true;
        
        allTestsPassed &= testQuotesInPrompt();
        allTestsPassed &= testNewlinesInPrompt();
        allTestsPassed &= testBackslashesInPrompt();
        allTestsPassed &= testSpecialCharactersInPrompt();
        allTestsPassed &= testGermanTextWithQuotes();
        allTestsPassed &= testJsonStructureIntegrity();
        
        if (!allTestsPassed) {
            System.exit(1);
        }
    }
    
    // Removed duplicate helpers - now using TestHelper utilities (assertEquals and isValidJson)
    
    /**
     * Test that prompts with double quotes are properly escaped
     */
    private static boolean testQuotesInPrompt() {
        boolean testPassed = true;
        
        String promptWithQuotes = "Du musst sagen: \"Hallo Welt!\" und dann: \"Auf Wiedersehen!\"";
        String json = Gemini.createGeminiJsonPayload(promptWithQuotes);
        
        testPassed &= assertTrue(isValidJson(json), "JSON with quotes should be valid");
        
        // Verify the original text can be extracted back
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertTrue(extractedText.equals(promptWithQuotes), 
                "Extracted text should match original prompt with quotes");
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test that prompts with newlines are properly handled
     */
    private static boolean testNewlinesInPrompt() {
        boolean testPassed = true;
        
        String promptWithNewlines = "Erste Zeile\nZweite Zeile\nDritte Zeile";
        String json = Gemini.createGeminiJsonPayload(promptWithNewlines);
        
        testPassed &= assertTrue(isValidJson(json), "JSON with newlines should be valid");
        
        // Verify the newlines are preserved
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertTrue(extractedText.equals(promptWithNewlines), 
                "Extracted text should preserve newlines");
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test that prompts with backslashes are properly handled
     */
    private static boolean testBackslashesInPrompt() {
        boolean testPassed = true;
        
        String promptWithBackslashes = "Pfad: C:\\Users\\Player\\Game\\file.txt";
        String json = Gemini.createGeminiJsonPayload(promptWithBackslashes);
        
        testPassed &= assertTrue(isValidJson(json), "JSON with backslashes should be valid");
        
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertTrue(extractedText.equals(promptWithBackslashes), 
                "Extracted text should preserve backslashes");
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test that prompts with various special characters are handled
     */
    private static boolean testSpecialCharactersInPrompt() {
        boolean testPassed = true;
        
        String promptWithSpecialChars = "Special chars: \t (tab), \b (backspace), \f (form feed), \r (carriage return)";
        String json = Gemini.createGeminiJsonPayload(promptWithSpecialChars);
        
        testPassed &= assertTrue(isValidJson(json), "JSON with special characters should be valid");
        
        return testPassed;
    }
    
    /**
     * Test the specific German text that was causing the original error
     */
    private static boolean testGermanTextWithQuotes() {
        boolean testPassed = true;
        
        // This is the exact text from NOT_FOUND_PROMPT that was causing the issue
        String problematicPrompt = "Du musst etwas sagen wie: \"Du hast versucht, etwas Nützliches zu finden, aber du hast nichts gefunden.\"";
        String json = Gemini.createGeminiJsonPayload(problematicPrompt);
        
        testPassed &= assertTrue(isValidJson(json), "German text with quotes should be valid JSON");
        
        // Verify the German umlauts and quotes are preserved
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertTrue(extractedText.equals(problematicPrompt), 
                "German text with umlauts and quotes should be preserved");
            testPassed &= assertTrue(extractedText.contains("Nützliches"), 
                "Umlauts should be preserved in JSON");
            testPassed &= assertTrue(extractedText.contains("\"Du hast versucht"), 
                "Quotes should be preserved in JSON");
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
    
    /**
     * Test that the JSON structure matches Google Gemini API expectations
     */
    private static boolean testJsonStructureIntegrity() {
        boolean testPassed = true;
        
        String testPrompt = "Test prompt";
        String json = Gemini.createGeminiJsonPayload(testPrompt);
        
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            
            // Verify the JSON has the expected structure
            testPassed &= assertTrue(parsed.has("contents"), "JSON should have 'contents' field");
            
            JsonArray contents = parsed.getAsJsonArray("contents");
            testPassed &= assertTrue(contents.size() == 1, "Contents array should have exactly one element");
            
            JsonObject contentItem = contents.get(0).getAsJsonObject();
            testPassed &= assertTrue(contentItem.has("parts"), "Content item should have 'parts' field");
            
            JsonArray parts = contentItem.getAsJsonArray("parts");
            testPassed &= assertTrue(parts.size() == 1, "Parts array should have exactly one element");
            
            JsonObject part = parts.get(0).getAsJsonObject();
            testPassed &= assertTrue(part.has("text"), "Part should have 'text' field");
            
            String text = part.get("text").getAsString();
            testPassed &= assertTrue(text.equals(testPrompt), "Text should match original prompt");
            
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
}