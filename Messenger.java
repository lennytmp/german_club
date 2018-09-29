package FightLang;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

class Messenger {
  // chatId -> last time the message was sent
  static Map<Integer, Integer> lastTimeSent = new HashMap<>();
  // TODO: save queue to disk to be resilient to task failurs
  static List<Message> queue = new LinkedList<>();
  static int DELAY = 2; // seconds

  private static class Message {
    int chatId;
    String msg;
    String[] options;
    boolean removeOptionsIfNeeded;

    Message(int chatId,
            String msg,
            String[] options,
            boolean removeOptionsIfNeeded) {
      this.chatId = chatId;
      this.msg = msg;
      this.options = options;
      this.removeOptionsIfNeeded = removeOptionsIfNeeded;
    }
  }

  public static void sendSheduledMessages() {
      int curTime = (int)(System.currentTimeMillis() / 1000L);
      List<Message> newQueue = new LinkedList<>();
      for (Message msg : queue) {
        Integer lastMessageTime = lastTimeSent.get(msg.chatId);
        if (lastMessageTime == null || lastMessageTime + DELAY < curTime) {
          TelegramApi.say(msg.chatId,
                          msg.msg,
                          msg.options,
                          msg.removeOptionsIfNeeded);
          lastTimeSent.put(msg.chatId, curTime);
        } else {
          TelegramApi.sendTypingEvent(msg.chatId);
          newQueue.add(msg);
        }
      }
      queue = newQueue;
  }

  public static void flush(int chatId) {
      List<Message> newQueue = new LinkedList<>();
      for (Message msg : queue) {
        if (chatId == msg.chatId) {
          TelegramApi.say(msg.chatId,
                          msg.msg,
                          msg.options,
                          msg.removeOptionsIfNeeded);
        } else {
          newQueue.add(msg);
        }
      }
      int curTime = (int)(System.currentTimeMillis() / 1000L);
      lastTimeSent.put(chatId, curTime);
      queue = newQueue;
  }

  public static void send(int chatId,
                          String msg,
                          String[] options,
                          boolean removeOptionsIfNeeded) {
    if (chatId < 0) {
      return;
    }
    TelegramApi.sendTypingEvent(chatId);
    queue.add(new Message(chatId, msg, options, removeOptionsIfNeeded));
  }

  public static void sendNow(int chatId,
                          String msg,
                          String[] options,
                          boolean removeOptionsIfNeeded) {
    if (chatId < 0) {
      return;
    }
    TelegramApi.say(chatId,
                    msg,
                    options,
                    removeOptionsIfNeeded);
  }

  public static void send(int chatId, String msg, String[] options) {
    send(chatId, msg, options, true);
  }

  public static void send(int chatId, String msg) {
    send(chatId, msg, new String[] {}, false);
  }

  public static void send(int chatId, String msg, List<String> options) {
    send(chatId, msg, options.toArray(new String[] {}), true);
  }
}
