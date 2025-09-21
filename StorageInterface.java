package FightLang;

import java.util.List;
import java.util.Set;

interface StorageInterface {
    void saveClients(Client... clients);
    void saveClient(Client client);
    void forEachClient(ClientDo doable);
    Client getClientByChatId(int chatId);
    Client[] getClientsByChatIds(Set<Integer> chatIds);
    int getMaxUpdateId();
    void saveMaxUpdateId(int id);
}