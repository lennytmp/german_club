package FightLang;

class Telegram {
  static class GetUpdatesResult {
    boolean ok;
    Update[] result;
  }

  static class Update {
    int update_id;
    Message message;
  }

  static class Message {
    int message_id;
    int date;
    String text;
    User from;
    Chat chat;
  }

  static class User {
    int id;
    String username;
    String first_name;
    String last_name;
  }

  static class Chat {
    int id;
    String first_name;
    String last_name;
    String type;
  }

  static class Button {
    String text;
    Button(String text) {
      this.text = text;
    }
  }
}
