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
    
    // Base game description without environmental context
    private static final String BASE_GAME_DESCRIPTION = "Du bist ein Rollenspiel. In diesem Spiel gibt es die folgenden Monster: " +
      "Bettler, Betrunkener, Crack-Süchtiger, Skelett, Zombie, Geist, Mumie, Lich, Vampir, Ghul, Nekromant, Teufel und Dämon. " +
      "Die Gegenstände, die Helden finden können, sind Gold, Silber, Asche, Papier, Klaue, Fangzahn, Wachs, Bandage, Seelenstein, " +
      "Fleisch, Knochen, Crack, Flasche, Münze.";
    
    // Environmental contexts to balance tone
    private static final String[] ENVIRONMENTAL_CONTEXTS = {
        // Dark/Grim contexts (40% of the time)
        "Die Welt ist von dunklen Schatten und verfallenen Ruinen geprägt. Nebel zieht durch verlassene Gassen und düstere Friedhöfe.",
        "Das Abenteuer spielt sich in den dunklen Katakomben und schattigen Gassen einer alten, verfallenen Stadt ab.",
        "Die Umgebung ist von Verfall und Dunkelheit geprägt - alte Grabstätten, verlassene Häuser und neblige Sümpfe.",
        "Die Atmosphäre ist düster und geheimnisvoll, mit alten Ruinen und schattigen Wäldern voller Geheimnisse.",
        
        // Neutral/Balanced contexts (40% of the time)
        "Das Abenteuer führt durch eine vielfältige Welt mit alten Bibliotheken, belebten Märkten und geheimnisvollen Wäldern.",
        "Die Welt besteht aus verschiedenen Orten: sonnige Marktplätze wechseln sich mit mystischen Höhlen und alten Tempeln ab.",
        "Der Held reist durch eine abwechslungsreiche Landschaft mit friedlichen Dörfern, antiken Ruinen und lebendigen Städten.",
        "Die Umgebung variiert zwischen belebten Handelswegen, ruhigen Gärten und geheimnisvollen, aber nicht bedrohlichen Orten.",
        
        // Beautiful/Bright contexts (20% of the time)
        "Die Welt ist voller Schönheit: blühende Gärten, kristallklare Seen, prächtige Paläste und sonnendurchflutete Marktplätze.",
        "Das Abenteuer spielt in einer wunderschönen Welt mit goldenen Feldern, eleganten Städten und majestätischen Bibliotheken."
    };
    
    // Weighted selection: 0-3 (dark), 4-7 (neutral), 8-9 (beautiful)
    private static final int[] CONTEXT_WEIGHTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    
    // Prompt templates for different scenarios
    private static final String NOT_FOUND_PROMPT = "In diesem Spiel suchte der Held nach etwas Nützlichem. " +
        "Du musst etwas sagen wie: \"Du hast versucht, etwas Nützliches zu finden, aber du hast nichts gefunden.\" Du kannst dir einen " +
        "Grund ausdenken, wo ich gesucht habe oder warum ich nichts gefunden habe. Halte dich kurz und erwähne nicht, wonach ich gesucht habe.";
    
    private static final String SOMETHING_FOUND_PROMPT = "Der Held hat gerade %s gefunden. Du musst es kurz beschreiben. " +
      "Zum Beispiel: \"Du bist durch den Wald spaziert und hast eine alte Feuerstelle gefunden, in der ein riesiges Skelett lag. " + 
      "Nach genauer Untersuchung hast du die Krallen herausgezogen und in deinen Rucksack gesteckt.\" " + 
      "Gib am Ende keine Zusammenfassung, der Spieler sollte den Text sorgfältig lesen, um zu verstehen, was er gefunden hat. Stelle keine Fragen. " +
      "Gehe davon aus, dass der Held diesen Gegenstand am Ende in seinen Rucksack steckt.";
    
    private static final String TRADER_PROMPT = "Der Held trifft auf eine Handelsperson, die einen Tausch anbietet. " +
      "WICHTIG: Die Handelsperson GIBT dem Spieler 1 %s und der Spieler GIBT der Handelsperson 1 %s. " +
      "Du musst kurz beschreiben, wie die Handelsperson aussieht und was sie sagt. Der Held kann auch zu einem Markt oder " +
      "in einen Laden kommen. Erstelle sowohl männliche als auch weibliche Handelspartner mit gleicher Wahrscheinlichkeit. " +
      "Beispiele: \"Eine geheimnisvolle Frau in einem dunklen Umhang tritt hervor. Ihre Augen funkeln neugierig.\" oder " +
      "\"Ein alter Mann mit weißem Bart nickt dir zu. Seine Hände sind voller Schmuck.\" " +
      "Beschreibe die Person kurz, halte dich an das Angebot und stelle am Ende die Frage nach der Entscheidung. " +
      "Verwende nicht die Wörter 'Händler' oder 'Handelsperson' in der Beschreibung.";

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

    /**
     * Builds a complete Gemini prompt with environmental context and player customization
     */
    public static String buildGamePrompt(Client client, String specificPrompt) {
        // Select environmental context based on weighted distribution
        int weightIndex = Utils.rndInRange(0, CONTEXT_WEIGHTS.length - 1);
        int contextIndex = CONTEXT_WEIGHTS[weightIndex];
        String environmentalContext = ENVIRONMENTAL_CONTEXTS[contextIndex];
        
        String basePrompt = BASE_GAME_DESCRIPTION + " " + environmentalContext;
        String playerPrompt = client.getPlayerPrompt();
        
        if (!playerPrompt.isEmpty()) {
            basePrompt += " Der Spieler beschreibt sich selbst als: " + playerPrompt + ".";
        }
        
        return basePrompt + " " + specificPrompt;
    }
    
    /**
     * Get the appropriate prompt template for finding something
     */
    public static String getSomethingFoundPrompt(String itemName) {
        return String.format(SOMETHING_FOUND_PROMPT, itemName);
    }
    
    /**
     * Get the appropriate prompt template for trader encounters
     */
    public static String getTraderPrompt(String requestedItem, String offeredItem) {
        return String.format(TRADER_PROMPT, requestedItem, offeredItem);
    }
    
    /**
     * Get the prompt template for not finding anything
     */
    public static String getNotFoundPrompt() {
        return NOT_FOUND_PROMPT;
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
