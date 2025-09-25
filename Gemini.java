package FightLang;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Gemini {
    static String apiKey;
    private static Gson g = new Gson();

    private class Config {
        String gemini_api_key;
    }

    public static void initialize() {
        // In dev environment, skip API key initialization
        if (!FightLang.Main.isProd) {
            apiKey = null;
            return;
        }
        
        String configText = Logger.getConfigText();
        Config c = g.fromJson(configText, Config.class);
        apiKey = c.gemini_api_key;
    }

    /**
     * Creates the JSON payload for Gemini API requests.
     * This method is package-private to allow testing.
     */
    static String createGeminiJsonPayload(String prompt) {
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

    public static String AskGemini(String prompt) {
        // In dev environment, don't call Gemini API
        if (!FightLang.Main.isProd) {
            return "";
        }
        
        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key="
                + apiKey;

        try {
            // Create an HttpClient instance
            HttpClient client = HttpClient.newHttpClient();

            // Create the JSON payload using proper JSON construction
            String jsonInputString = createGeminiJsonPayload(prompt);

            // Create the HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                    .build();

            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status code
            if (response.statusCode() == 200) { 
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                String text = root
                    .getAsJsonArray("candidates").get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject()
                    .get("text").getAsString();
                return text;
            } else {
                throw new Exception("Bad response: " + response.body());
            }

        } catch (Exception e) {
            Logger.log(urlString);
            Logger.logException(e);
            return "";
        }
    }
}
