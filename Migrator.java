package FightLang;

class Migrator {
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: FightLang.jar path/to/db");
      System.exit(0);
    }
    Logger.setDbPath(args[0]);
    Logger.initialize();

    System.out.println("Starting migration...");
    Storage storage = new Storage();
    storage.forEachClient(new ClientDo() {
      public void run(Client client) {
        if (client == null) {
          return; // this shouldn't happen
        }
        if (client.chatId < 0) {
          return; // bots have no async logic as of now
        }
        migrate(client, storage);
      }
    });
    System.out.println("Migration finished...");
  }

  private static void migrate(Client client, Storage storage) {
    client.setStorage(storage); // Set storage dependency
    
    if (client.level == 0) {
      client.level = 1;
    }
    int neededExp = client.nextExp();
    if (client.exp >= neededExp) {
      System.out.println(client.username + " "
        + "was " + client.exp + " "
        + "needed " + neededExp
        + "got level up");
      client.level++;
    }
    storage.saveClient(client);
  }
}
