package ChatBot;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Storage {
  private static Map<Integer, String> clients = new HashMap<>();
  private static Gson g = new Gson();
  
  static void saveClients(Client... clients) {
    String[] names =  new String[clients.length];
    String[] values =  new String[clients.length];
    for (int i = 0; i < clients.length; i++) {
      names[i] = Integer.toString(clients[i].chatId);
      values[i] = g.toJson(clients[i]);
    }
    Logger.saveClients(names, values);
  }

  static void saveClient(Client client) {
    String chatId = Integer.toString(client.chatId);
    Logger.saveClient(chatId, g.toJson(client));
  }

  static void forEachClient(ClientDo doable) {
    List<String> chatIds = Logger.getAllClientNames();
    for (String chatId : chatIds) {
      String clientJson = Logger.getClient(chatId);
      Client c = g.fromJson(clientJson, Client.class);
      if (c == null) {
        Logger.logException(new Exception(clientJson + " - " + chatId));
        continue;
      }
      doable.run(c);
    }
  }

  static Client getClientByChatId(int chatId) {
    String clientJson = Logger.getClient(Integer.toString(chatId));
    if (clientJson == null) {
      return null;
    }
    return g.fromJson(clientJson, Client.class);
  }

  static Client[] getClientsByChatIds(Set<Integer> chatIds) {
    Client[] result = new Client[chatIds.size()];
    int i = 0;
    for (int chatId : chatIds) {
      result[i] = getClientByChatId(chatId);
      i++;
    }
    return result;
  }

  static int getMaxUpdateId() {
    Integer result = Logger.getIntVar("maxUpdateId");
    if (result == null) {
      result = 0;
    }
    return result;
  }

  static void saveMaxUpdateId(int id) {
    Logger.saveIntVar("maxUpdateId", id);
  }
}

interface ClientDo {
  public void run(Client c);
}
