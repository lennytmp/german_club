package FightLang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class MockStorage implements StorageInterface {
    private Map<Integer, Client> clients = new HashMap<>();
    private int maxUpdateId = 0;
    
    @Override
    public void saveClients(Client... clientsToSave) {
        for (Client client : clientsToSave) {
            clients.put(client.chatId, client);
        }
    }
    
    @Override
    public void saveClient(Client client) {
        clients.put(client.chatId, client);
    }
    
    @Override
    public void forEachClient(ClientDo doable) {
        for (Client client : clients.values()) {
            doable.run(client);
        }
    }
    
    @Override
    public Client getClientByChatId(int chatId) {
        return clients.get(chatId);
    }
    
    @Override
    public Client[] getClientsByChatIds(Set<Integer> chatIds) {
        List<Client> result = new ArrayList<>();
        for (int chatId : chatIds) {
            Client client = clients.get(chatId);
            if (client != null) {
                result.add(client);
            }
        }
        return result.toArray(new Client[0]);
    }
    
    @Override
    public int getMaxUpdateId() {
        return maxUpdateId;
    }
    
    @Override
    public void saveMaxUpdateId(int id) {
        this.maxUpdateId = id;
    }
    
    // Test helper methods
    public void addClient(Client client) {
        clients.put(client.chatId, client);
    }
    
    public Map<Integer, Client> getAllClients() {
        return new HashMap<>(clients);
    }
    
    public void clear() {
        clients.clear();
        maxUpdateId = 0;
    }
}