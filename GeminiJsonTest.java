package FightLang;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Test to prevent JSON escaping issues in Gemini API calls.
 * This test ensures that the JSON payload construction properly handles
 * special characters that could break the API request.
 */
public class GeminiJsonTest {
    private static Gson g = new Gson();
    
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
    
    private static boolean assertEquals(boolean condition, String testName) {
        if (condition) {
            System.out.print("S");
            return true;
        }
        System.out.println(" Test: " + testName + " FAILED");
        return false;
    }
    
    /**
     * Creates JSON payload using the same method as Gemini.java
     */
    private static String createJsonPayload(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);
        
        JsonArray parts = new JsonArray();
        parts.add(textPart);
        
        JsonObject contentItem = new JsonObject();
        contentItem.add("parts", parts);
        
        JsonArray contents = new JsonArray();
        contents.add(contentItem);
        
        JsonObject payload = new JsonObject();
        payload.add("contents", contents);
        
        return g.toJson(payload);
    }
    
    /**
     * Validates that a JSON string can be parsed back without errors
     */
    private static boolean isValidJson(String jsonString) {
        try {
            JsonParser.parseString(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Test that prompts with double quotes are properly escaped
     */
    private static boolean testQuotesInPrompt() {
        boolean testPassed = true;
        
        String promptWithQuotes = "Du musst sagen: \"Hallo Welt!\" und dann: \"Auf Wiedersehen!\"";
        String json = createJsonPayload(promptWithQuotes);
        
        testPassed &= assertEquals(isValidJson(json), "JSON with quotes should be valid");
        
        // Verify the original text can be extracted back
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertEquals(extractedText.equals(promptWithQuotes), 
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
        String json = createJsonPayload(promptWithNewlines);
        
        testPassed &= assertEquals(isValidJson(json), "JSON with newlines should be valid");
        
        // Verify the newlines are preserved
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertEquals(extractedText.equals(promptWithNewlines), 
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
        String json = createJsonPayload(promptWithBackslashes);
        
        testPassed &= assertEquals(isValidJson(json), "JSON with backslashes should be valid");
        
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertEquals(extractedText.equals(promptWithBackslashes), 
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
        String json = createJsonPayload(promptWithSpecialChars);
        
        testPassed &= assertEquals(isValidJson(json), "JSON with special characters should be valid");
        
        return testPassed;
    }
    
    /**
     * Test the specific German text that was causing the original error
     */
    private static boolean testGermanTextWithQuotes() {
        boolean testPassed = true;
        
        // This is the exact text from NOT_FOUND_PROMPT that was causing the issue
        String problematicPrompt = "Du musst etwas sagen wie: \"Du hast versucht, etwas Nützliches zu finden, aber du hast nichts gefunden.\"";
        String json = createJsonPayload(problematicPrompt);
        
        testPassed &= assertEquals(isValidJson(json), "German text with quotes should be valid JSON");
        
        // Verify the German umlauts and quotes are preserved
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String extractedText = parsed
                .getAsJsonArray("contents").get(0).getAsJsonObject()
                .getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("text").getAsString();
            
            testPassed &= assertEquals(extractedText.equals(problematicPrompt), 
                "German text with umlauts and quotes should be preserved");
            testPassed &= assertEquals(extractedText.contains("Nützliches"), 
                "Umlauts should be preserved in JSON");
            testPassed &= assertEquals(extractedText.contains("\"Du hast versucht"), 
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
        String json = createJsonPayload(testPrompt);
        
        try {
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            
            // Verify the JSON has the expected structure
            testPassed &= assertEquals(parsed.has("contents"), "JSON should have 'contents' field");
            
            JsonArray contents = parsed.getAsJsonArray("contents");
            testPassed &= assertEquals(contents.size() == 1, "Contents array should have exactly one element");
            
            JsonObject contentItem = contents.get(0).getAsJsonObject();
            testPassed &= assertEquals(contentItem.has("parts"), "Content item should have 'parts' field");
            
            JsonArray parts = contentItem.getAsJsonArray("parts");
            testPassed &= assertEquals(parts.size() == 1, "Parts array should have exactly one element");
            
            JsonObject part = parts.get(0).getAsJsonObject();
            testPassed &= assertEquals(part.has("text"), "Part should have 'text' field");
            
            String text = part.get("text").getAsString();
            testPassed &= assertEquals(text.equals(testPrompt), "Text should match original prompt");
            
        } catch (Exception e) {
            testPassed = false;
        }
        
        return testPassed;
    }
}