package FightLang;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MockTelegram implements TelegramInterface {
    public static class SentMessage {
        public int chatId;
        public String message;
        public String[] buttons;
        
        public SentMessage(int chatId, String message, String[] buttons) {
            this.chatId = chatId;
            this.message = message;
            this.buttons = buttons != null ? Arrays.copyOf(buttons, buttons.length) : new String[0];
        }
        
        public boolean hasButton(String buttonText) {
            if (buttons == null) return false;
            for (String button : buttons) {
                if (button.equals(buttonText)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            return String.format("Message to %d: %s (buttons: %s)", 
                chatId, message, Arrays.toString(buttons));
        }
    }
    
    private List<SentMessage> sentMessages = new ArrayList<>();
    private List<Telegram.Update> queuedUpdates = new ArrayList<>();
    private int nextUpdateId = 1;
    
    @Override
    public Telegram.Update[] getUpdates(int offset) {
        List<Telegram.Update> updates = new ArrayList<>();
        for (Telegram.Update update : queuedUpdates) {
            if (update.update_id >= offset) {
                updates.add(update);
            }
        }
        return updates.toArray(new Telegram.Update[0]);
    }
    
    @Override
    public void sendMessage(int chatId, String message, String[] buttons) {
        sentMessages.add(new SentMessage(chatId, message, buttons));
    }
    
    @Override
    public void sendMessage(int chatId, String message) {
        sendMessage(chatId, message, new String[0]);
    }
    
    @Override
    public void sendTypingEvent(int chatId) {
        // Mock implementation - do nothing
    }
    
    // Test helper methods
    public List<SentMessage> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }
    
    public List<SentMessage> getMessagesForChat(int chatId) {
        List<SentMessage> result = new ArrayList<>();
        for (SentMessage msg : sentMessages) {
            if (msg.chatId == chatId) {
                result.add(msg);
            }
        }
        return result;
    }
    
    public SentMessage getLastMessage() {
        if (sentMessages.isEmpty()) return null;
        return sentMessages.get(sentMessages.size() - 1);
    }
    
    public SentMessage getLastMessageForChat(int chatId) {
        for (int i = sentMessages.size() - 1; i >= 0; i--) {
            SentMessage msg = sentMessages.get(i);
            if (msg.chatId == chatId) {
                return msg;
            }
        }
        return null;
    }
    
    public void clearMessages() {
        sentMessages.clear();
    }
    
    // Simulate user sending a message
    public void simulateUserMessage(int chatId, String username, String text) {
        Telegram.Update update = new Telegram.Update();
        update.update_id = nextUpdateId++;
        update.message = new Telegram.Message();
        update.message.chat = new Telegram.Chat();
        update.message.chat.id = chatId;
        update.message.from = new Telegram.User();
        update.message.from.username = username;
        update.message.from.first_name = username;
        update.message.text = text;
        update.message.date = (int) (System.currentTimeMillis() / 1000);
        
        queuedUpdates.add(update);
    }
    
    public void clearUpdates() {
        queuedUpdates.clear();
    }
    
    public int getMessageCount() {
        return sentMessages.size();
    }
    
    public int getMessageCountForChat(int chatId) {
        return getMessagesForChat(chatId).size();
    }
    
    // Helper to check if any message contains specific text
    public boolean hasMessageContaining(String text) {
        for (SentMessage msg : sentMessages) {
            if (msg.message.contains(text)) {
                return true;
            }
        }
        return false;
    }
    
    // Helper to check if any message to specific chat contains specific text
    public boolean hasMessageForChatContaining(int chatId, String text) {
        for (SentMessage msg : getMessagesForChat(chatId)) {
            if (msg.message.contains(text)) {
                return true;
            }
        }
        return false;
    }
}