package FightLang;

public class RealTelegram implements TelegramInterface {
    @Override
    public Telegram.Update[] getUpdates(int offset) {
        return TelegramApi.getUpdates(offset);
    }
    
    @Override
    public void sendMessage(int chatId, String message, String[] buttons) {
        Messenger.send(chatId, message, buttons);
    }
    
    @Override
    public void sendMessage(int chatId, String message) {
        Messenger.send(chatId, message);
    }
    
    @Override
    public void sendTypingEvent(int chatId) {
        TelegramApi.sendTypingEvent(chatId);
    }
}