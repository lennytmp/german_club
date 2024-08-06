package FightLang;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

class Client {
  // TODO: Status and body port should move to Game
  enum Status {
    FIGHTING, IDLE, READY_TO_FIGHT
  };

  enum BodyPart {
    HEAD, TORSO, LEGS
  };

  String username;
  // TODO(lenny): add unkown language by default and ask players to provide one.
  String lang = "en";
  int chatId = 0;
  boolean nameChangeHintSent = false;
  Status status = Client.Status.IDLE;
  // challenge: [questionId, difficulty]
  int[] challenge = new int[2];
  // questionId -> next challenge level
  // 0: guess the word
  // 1: guess the article (if it is present)
  // 2: guess correct spelling
  // 3: spell given the letters
  // 4: spell, no hints
  Map<Integer, Integer> fightQuestions = new HashMap<>();

  int fightingChatId = 0;
  int lastRestore = 0;
  int readyToFightSince = 0;
  int lastFightActivitySince = 0;
  int lastActivity = 0;
  private int successToday = 0;
  private int lastDailyCleanup = 0;
  boolean timeoutWarningSent = false;

  int totalFights = 0;
  int fightsWon = 0;

  int exp = 0;
  int level = 1;
  int strength = 3;
  int vitality = 3;
  int luck = 3;
  int levelPoints = 0;

  int hp;
  // Enum ID to the quantity of that item.
  Map<Integer, Integer> inventory = new HashMap<>(Game.ITEM_VALUES.length);

  Client(int chatId, String username) {
    this.chatId = chatId;
    this.username = username;
    hp = getMaxHp();
  }

  // Used for creating bots
  Client(int chatId, Client opponent) {
    this.chatId = chatId;
    if (opponent.level == 1) {
      vitality = 1;
      strength = 1;
      luck = 1;
      level = 1;
    } else {
      int k = 1;
      if (Utils.rndInRange(0, opponent.totalFights) > opponent.fightsWon) {
        k *= -1;
      }
      level = Math.max(opponent.level + k * Utils.rndInRange(0, 4), 1);
    }
    BotConfig bc = pickBotType();
    this.username = bc.name;
    for (int i = 1; i < this.level; i++) {
      int ch = Utils.rndInRangeWeighted(bc.characteristics);
      if (ch == 0) {
        strength++;
      } else if (ch == 1) {
        vitality++;
      } else {
        luck++;
      }
    }
    for (int i = 0; i < bc.loot.length; i++) {
      Game.Item item = bc.loot[i];
      giveItem(item);
    }
    hp = getMaxHp();
  }

  public void loseInvetory() {
    inventory = new HashMap<>(Game.ITEM_VALUES.length);
  }

  public int getSuccessToday() {
    return successToday;
  }

  public void setSuccessToday(int val) {
    successToday = val;
    Storage.saveClient(this);
  }

  public void incSuccessToday() {
    successToday++;
    Storage.saveClient(this);
  }

  public int getLastDailyCleanup() {
    return lastDailyCleanup;
  }

  public void setLastDailyCleanup(int val) {
    lastDailyCleanup = val;
    Storage.saveClient(this);
  }

  public int getMaxHp() {
    return vitality * 2 + 5;
  }

  public int getMaxDamage() {
    return strength;
  }

  public void giveItem(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    inventory.put(item.ordinal(), ++curHave);
  }

  public void takeItem(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    if (curHave - 1 == 0) {
      inventory.remove(item.ordinal());
    }
    inventory.put(item.ordinal(), --curHave);
  }

  public boolean hasItem(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    return curHave > 0;
  }

  public int getItemNum(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    return curHave.intValue();
  }

  // Returns true if promoted.
  public boolean levelUpIfNeeded() {
    if (exp < nextExp()) {
      return false;
    }
    level++;
    levelPoints++;
    return true;
  }

  public int nextExp() {
    return level * 10 * 10;
  }

  public int expForKillingMe() {
    return level * 10;
  }

  private BotConfig pickBotType() {
    List<BotConfig> eligible = new LinkedList<>();
    for (BotConfig bc : Game.BOT_TYPES) {
      if (level >= bc.minLevel && level <= bc.maxLevel) {
        eligible.add(bc);
      }
    }
    return Utils.getRnd(eligible.toArray(new BotConfig[0]));
  }

  public String getInventoryDescription(String separator) {
    if (inventory.isEmpty()) {
      return "";
    }

    StringBuilder result = new StringBuilder();
    boolean firstItem = true; // Flag to handle separator

    for (Map.Entry<Integer, Integer> item : inventory.entrySet()) {
      int quantity = item.getValue();
      if (quantity <= 0) {
        continue;
      }

      if (!firstItem) {
        result.append(separator);
      }
      firstItem = false;

      result.append(quantity);
      result.append(" ");
      if (quantity == 1) {
        result.append(Game.ITEM_VALUES[item.getKey()].singular);
      } else {
        result.append(Game.ITEM_VALUES[item.getKey()].plural);
      }
    }

    return result.toString();
  }
}
