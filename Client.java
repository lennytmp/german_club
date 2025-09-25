package FightLang;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

class Client {
  // TODO: Status and body port should move to Game
  enum Status {
    FIGHTING, IDLE, READY_TO_FIGHT, TRADING
  };

  // Storage dependency for saving client state
  private transient StorageInterface storage;

  String username;
  int chatId = 0;
  boolean nameChangeHintSent = false;
  Status status = Client.Status.IDLE;

  int fightingChatId = 0;
  int lastRestore = 0;
  int readyToFightSince = 0;
  int lastFightActivitySince = 0;
  int lastActivity = 0;
  private int successToday = 0;
  private int lastDailyCleanup = 0;

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

  // Trading system fields
  Game.Item offeredItem = null;
  Game.Item requestedItem = null;

  // Potion effects tracking - persistent timestamps
  int strengthPotionExpiry = 0;  // Unix timestamp when strength effect expires
  int strengthPotionBonus = 0;   // Current strength bonus from potions
  int luckPotionExpiry = 0;      // Unix timestamp when luck effect expires  
  int luckPotionBonus = 0;       // Current luck bonus from potions

  Client(int chatId, String username) {
    this.chatId = chatId;
    this.username = username;
    hp = getMaxHp();
  }
  
  // Set storage dependency (needed after deserialization or for testing)
  public void setStorage(StorageInterface storage) {
    this.storage = storage;
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

  

  // Removes 1-3 random item units from the inventory (weighted by counts) and
  // returns a human-readable description of what was lost, e.g., "2 potions, 1 fang".
  public String loseRandomItems() {
    if (inventory.isEmpty()) {
      return "";
    }

    // Build a weighted list of item indices according to their counts
    java.util.List<Integer> weighted = new java.util.ArrayList<>();
    for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
      int itemIndex = entry.getKey();
      int count = entry.getValue() == null ? 0 : entry.getValue();
      for (int i = 0; i < count; i++) {
        weighted.add(itemIndex);
      }
    }

    if (weighted.isEmpty()) {
      return "";
    }

    int numToLose = Utils.rndInRange(1, 3);
    java.util.Map<Integer, Integer> lostCounts = new java.util.HashMap<>();
    for (int i = 0; i < numToLose && !weighted.isEmpty(); i++) {
      int pickIdx = Utils.rndInRange(0, weighted.size() - 1);
      int itemIndex = weighted.remove(pickIdx);
      // Decrement from inventory
      Integer have = inventory.get(itemIndex);
      if (have != null && have > 0) {
        int newCount = have - 1;
        if (newCount == 0) {
          inventory.remove(itemIndex);
        } else {
          inventory.put(itemIndex, newCount);
        }
        // Track what was lost
        lostCounts.put(itemIndex, lostCounts.getOrDefault(itemIndex, 0) + 1);
      }

      
    }

    // Build description string for lost items
    if (lostCounts.isEmpty()) {
      return "";
    }

    StringBuilder desc = new StringBuilder();
    boolean first = true;
    for (Map.Entry<Integer, Integer> e : lostCounts.entrySet()) {
      if (!first) {
        desc.append(", ");
      }
      first = false;
      int count = e.getValue();
      int idx = e.getKey();
      desc.append(count).append(" ");
      if (count == 1) {
        desc.append(Game.ITEM_VALUES[idx].singular);
      } else {
        desc.append(Game.ITEM_VALUES[idx].plural);
      }
    }

    return desc.toString();
  }

  public int getSuccessToday() {
    return successToday;
  }

  public void setSuccessToday(int val) {
    successToday = val;
    if (storage != null) {
      storage.saveClient(this);
    }
  }

  public void incSuccessToday() {
    successToday++;
    if (storage != null) {
      storage.saveClient(this);
    }
  }

  public int getLastDailyCleanup() {
    return lastDailyCleanup;
  }

  public void setLastDailyCleanup(int val) {
    lastDailyCleanup = val;
    if (storage != null) {
      storage.saveClient(this);
    }
  }

  public int getMaxHp() {
    return vitality * 2 + 5;
  }

  public int getMaxDamage() {
    return getEffectiveStrength();
  }

  public void giveItem(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    inventory.put(item.ordinal(), ++curHave);
  }

  public void takeItem(Game.Item item) {
    Integer curHave = inventory.get(item.ordinal());
    curHave = curHave == null ? 0 : curHave;
    if (curHave > 0) {
      if (curHave - 1 == 0) {
        inventory.remove(item.ordinal());
      } else {
        inventory.put(item.ordinal(), curHave - 1);
      }
    }
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
    int exp = 30;
    for (int i = 2; i < level + 1; i++) {
      exp += i * 10 * 10;
    }
    return exp;
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
    StringBuilder result = new StringBuilder();
    boolean firstItem = true;
    boolean hasItems = false;

    for (Map.Entry<Integer, Integer> item : inventory.entrySet()) {
      int quantity = item.getValue();

      // Only process items with a positive quantity
      if (quantity > 0) {
        hasItems = true;

        // Append the separator before the item if it's not the first item
        if (!firstItem) {
          result.append(separator);
        }
        firstItem = false;

        // Append the item details to the result
        result.append(quantity);
        result.append(" ");
        if (quantity == 1) {
          result.append(Game.ITEM_VALUES[item.getKey()].singular);
        } else {
          result.append(Game.ITEM_VALUES[item.getKey()].plural);
        }
      }
    }

    // Return an empty string if no items with positive quantity were found
    if (!hasItems) {
      return "";
    }

    return result.toString();
  }

  // Trading system methods
  public boolean hasAnyItems() {
    for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
      if (entry.getValue() > 0) {
        return true;
      }
    }
    return false;
  }

  public Game.Item getRandomPlayerItem() {
    // Build a weighted list of item indices according to their counts
    java.util.List<Integer> weighted = new java.util.ArrayList<>();
    for (Map.Entry<Integer, Integer> entry : inventory.entrySet()) {
      int itemIndex = entry.getKey();
      int count = entry.getValue() == null ? 0 : entry.getValue();
      for (int i = 0; i < count; i++) {
        weighted.add(itemIndex);
      }
    }
    
    if (weighted.isEmpty()) {
      return null;
    }
    
    int randomIndex = weighted.get(Utils.rndInRange(0, weighted.size() - 1));
    return Game.ITEM_VALUES[randomIndex];
  }

  public void resetTradeState() {
    status = Client.Status.IDLE;
    offeredItem = null;
    requestedItem = null;
    if (storage != null) {
      storage.saveClient(this);
    }
  }

  public boolean generateTradeOffer(Game.Item requestedItem) {
    if (!hasAnyItems()) {
      return false;
    }

    // Generate trade offer
    this.offeredItem = getRandomPlayerItem();
    this.requestedItem = requestedItem;
    this.status = Client.Status.TRADING;
    return true;
  }

  public boolean executeTrade() {
    if (status != Client.Status.TRADING || offeredItem == null || requestedItem == null) {
      return false;
    }

    // Check if player still has the offered item
    if (!hasItem(offeredItem)) {
      return false;
    }

    // Execute the trade
    takeItem(offeredItem);
    giveItem(requestedItem);
    return true;
  }

  // Potion effect management methods
  public void addStrengthPotionEffect(int bonusValue, int currentTime) {
    int newExpiry = currentTime + 180; // 3 minutes = 180 seconds
    
    // For stacking behavior: if current effect is still active, add to bonus
    // and set expiry to the new potion's expiry (each potion's effect is independent)
    if (strengthPotionExpiry > currentTime) {
      strengthPotionBonus += bonusValue;
    } else {
      strengthPotionBonus = bonusValue;
    }
    strengthPotionExpiry = newExpiry;
  }

  public void addLuckPotionEffect(int bonusValue, int currentTime) {
    int newExpiry = currentTime + 180; // 3 minutes = 180 seconds
    
    // For stacking behavior: if current effect is still active, add to bonus
    // and set expiry to the new potion's expiry (each potion's effect is independent)
    if (luckPotionExpiry > currentTime) {
      luckPotionBonus += bonusValue;
    } else {
      luckPotionBonus = bonusValue;
    }
    luckPotionExpiry = newExpiry;
  }

  public void removeExpiredPotionEffects(int currentTime) {
    if (strengthPotionExpiry > 0 && strengthPotionExpiry <= currentTime) {
      strengthPotionExpiry = 0;
      strengthPotionBonus = 0;
    }
    if (luckPotionExpiry > 0 && luckPotionExpiry <= currentTime) {
      luckPotionExpiry = 0;
      luckPotionBonus = 0;
    }
  }

  public int getEffectiveStrength() {
    return strength + (strengthPotionExpiry > 0 ? strengthPotionBonus : 0);
  }

  public int getEffectiveLuck() {
    return luck + (luckPotionExpiry > 0 ? luckPotionBonus : 0);
  }

  public boolean hasActivePotionEffects() {
    return strengthPotionExpiry > 0 || luckPotionExpiry > 0;
  }
}
