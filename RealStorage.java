package FightLang;

import java.util.Set;

public class RealStorage implements StorageInterface {
    @Override
    public void saveClients(Client... clients) {
        Storage.saveClients(clients);
    }
    
    @Override
    public void saveClient(Client client) {
        Storage.saveClient(client);
    }
    
    @Override
    public void forEachClient(ClientDo doable) {
        Storage.forEachClient(doable);
    }
    
    @Override
    public Client getClientByChatId(int chatId) {
        return Storage.getClientByChatId(chatId);
    }
    
    @Override
    public Client[] getClientsByChatIds(Set<Integer> chatIds) {
        return Storage.getClientsByChatIds(chatIds);
    }
    
    @Override
    public int getMaxUpdateId() {
        return Storage.getMaxUpdateId();
    }
    
    @Override
    public void saveMaxUpdateId(int id) {
        Storage.saveMaxUpdateId(id);
    }
}