package FightLang;

import com.google.gson.Gson;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class Storage implements StorageInterface {
  private Gson g = new Gson();

  @Override
  public void saveClients(Client... clients) {
    String[] names = new String[clients.length];
    String[] values = new String[clients.length];
    for (int i = 0; i < clients.length; i++) {
      names[i] = Integer.toString(clients[i].chatId);
      values[i] = g.toJson(clients[i]);
    }
    Logger.saveClients(names, values);
  }

  @Override
  public void saveClient(Client client) {
    String chatId = Integer.toString(client.chatId);
    Logger.saveClient(chatId, g.toJson(client));
  }

  @Override
  public void forEachClient(ClientDo doable) {
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

  @Override
  public Client getClientByChatId(int chatId) {
    String clientJson = Logger.getClient(Integer.toString(chatId));
    if (clientJson == null) {
      return null;
    }
    return g.fromJson(clientJson, Client.class);
  }

  @Override
  public Client[] getClientsByChatIds(Set<Integer> chatIds) {
    Client[] result = new Client[chatIds.size()];
    int i = 0;
    for (int chatId : chatIds) {
      result[i] = getClientByChatId(chatId);
      i++;
    }
    return result;
  }

  @Override
  public int getMaxUpdateId() {
    Integer result = Logger.getIntVar("maxUpdateId");
    if (result == null) {
      result = 0;
    }
    return result;
  }

  @Override
  public void saveMaxUpdateId(int id) {
    Logger.saveIntVar("maxUpdateId", id);
  }
}

interface ClientDo {
  public void run(Client c);
}
