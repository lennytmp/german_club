package FightLang;

interface TelegramInterface {
    Telegram.Update[] getUpdates(int offset);
    void sendMessage(int chatId, String message, String[] buttons);
    void sendMessage(int chatId, String message);
    void sendTypingEvent(int chatId);
}