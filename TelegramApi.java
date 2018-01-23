package ChatBot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonDeserializationContext;

class TelegramApi {
  static String token;
  private static final String URL = "https://api.telegram.org/bot";
  private final String method;
  private final String params;
  HttpURLConnection connection;
  private static Gson g = new Gson();

  private class Config {
    String token;
    String file;
  }

  public static void initialize() {
    String configText = Logger.getConfigText();
    Config c = g.fromJson(configText, Config.class);
    token =  c.token;
  }

  public static void say(int chatId,
                         String text,
                         String[] buttonTexts,
                         boolean removeButtonsIfNeeded) {
    try {
      text = URLEncoder.encode(text, "UTF-8");
    } catch (Exception e) {
      Logger.logException(e);
    }
    String params = "chat_id=" + chatId + "&text=" + text;
    if (buttonTexts.length > 0) {
      int numberOfRows = (int)Math.ceil(buttonTexts.length/3.0);
      Telegram.Button[][] arr = new Telegram.Button[numberOfRows][];
      int row = 0;
      int col = 0;
      for (int i = 0; i < buttonTexts.length; i++) {
        if (col == 0) {
          arr[row] = new Telegram.Button[Math.min(3, buttonTexts.length - i)];
        }
        arr[row][col] = new Telegram.Button(buttonTexts[i]);
        col++;
        if (col == 3) {
          row++;
          col = 0;
        }
      }
      params += "&reply_markup={\"keyboard\":" + g.toJson(arr) + "}";
    } else if (buttonTexts.length == 0 && removeButtonsIfNeeded) {
      params += "&reply_markup={\"remove_keyboard\":true}";
    }
    TelegramApi req = new TelegramApi("sendMessage", params);
    req.execute();
  }

  public static Telegram.Update[] getUpdates(int offset) {
    TelegramApi req = new TelegramApi(
      "getUpdates",
      "offset=" + offset);
    String resp = req.execute();
    if (resp == "") {
      return new Telegram.Update[0];
    }
    Telegram.GetUpdatesResult updates =
        g.fromJson(resp, Telegram.GetUpdatesResult.class);
    return updates.result;
  }

  private TelegramApi(String method) {
    this(method, "");
  }

  private TelegramApi(String method, String params) {
    this.method = method;
    this.params = params;
    if (!method.equals("getUpdates")) {
      Logger.log("request: " + method + "?" + params);
    }
  }

  public String execute() {
    String result = "";
    try {
      connection = getConnection();
      sendRequest();
      result = getResponse();
    } catch (Exception e) {
      Logger.logException(e);
    }
    if (!result.trim().equals("{\"ok\":true,\"result\":[]}")) {
      Logger.log("response: " + result);
    }
    return result;
  }

  private String getResponse() throws IOException {
    InputStream is = connection.getInputStream();
    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
    StringBuilder response = new StringBuilder();
    String line;
    while((line = rd.readLine()) != null) {
      response.append(line);
      response.append('\r');
    }
    rd.close();
    return response.toString();
  }

  private void sendRequest() throws IOException {
    DataOutputStream wr = new DataOutputStream(
        connection.getOutputStream());
    wr.write(params.getBytes("UTF-8"));
    wr.close();
  }

  private HttpURLConnection getConnection()
      throws MalformedURLException, IOException, ProtocolException {
    URL url = new URL(URL + token + "/" + method);
    connection = (HttpURLConnection)url.openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type",
        "application/x-www-form-urlencoded");
    connection.setRequestProperty("Content-Length",
        Integer.toString(params.getBytes().length));
    connection.setRequestProperty("Content-Language", "en-US");
    connection.setUseCaches(false);
    connection.setDoOutput(true);
    return connection;
  }
}
