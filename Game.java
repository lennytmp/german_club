package FightLang;

import java.util.EnumMap;
import java.util.Map;

class Game {
  static final BotConfig[] BOT_TYPES = {
      new BotConfig("Beggar",
          new int[] { 1, 3 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.COIN }),

      new BotConfig("Drunk",
          new int[] { 1, 3 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.BOTTLE }),

      new BotConfig("Crackhead",
          new int[] { 1, 3 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.CRACK }),

      new BotConfig("Skeleton",
          new int[] { 4, 6 },
          new int[] { 4, 4, 1 },
          new Item[] { Item.BONE }),

      new BotConfig("Zombie",
          new int[] { 4, 6 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.FLESH }),

      new BotConfig("Ghost",
          new int[] { 4, 6 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.SOUL_STONE }),

      new BotConfig("Mummy",
          new int[] { 7, 9 },
          new int[] { 1, 1, 1 },
          new Item[] { Item.BANDAGE }),

      new BotConfig("Lich",
          new int[] { 7, 9 },
          new int[] { 4, 4, 1 },
          new Item[] { Item.WAX }),

      new BotConfig("Vampire",
          new int[] { 7, 9 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.FANG, Item.CLAW }),

      new BotConfig("Ghoul",
          new int[] { 10, 12 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.CLAW }),

      new BotConfig("Undead",
          new int[] { 10, 12 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.ASH }),

      new BotConfig("Necromant",
          new int[] { 10, 12 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.PAPER, Item.ASH }),

      new BotConfig("Devil",
          new int[] { 12, 20 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.SILVER, Item.GOLD }),

      new BotConfig("Demon",
          new int[] { 12, 20 },
          new int[] { 1, 1, 1 },
          new Item[] { Item.GOLD, Item.SILVER })
  };

  enum Item {
    // TODO: when an item is added to the list - clients needs to be updated with a
    // larger array.
    COIN("Münze", "coins"), BOTTLE("Flasche", "Flaschen"),
    CRACK("Gramm Crack", "Gramm Crack"), BONE("Knochen", "Knochen"),
    FLESH("Fleischstück", "Fleischstücke"), SOUL_STONE("Seelenstein", "Seelensteine"),
    BANDAGE("Bandage", "Bandagen"), WAX("Gramm Wachs", "Gramm Wachs"),
    FANG("Reißzahn", "Reißzähne"), CLAW("Klaue", "Klauen"), ASH("Gramm Asche", "Gramm Asche"),
    PAPER("Blatt Papier", "Blätter Papier"), SILVER("Silberstück", "Silberstücke"),
    GOLD("Goldstück", "Goldstücke"), HPOTION("Heiltrank", "Heiltränke");

    String singular, plural;

    private Item(String singular, String plural) {
      this.singular = singular;
      this.plural = plural;
    }
  };

  static final Item[] ITEM_VALUES = Item.values();

  // Define the recipe for brewing a potion
  private static final Map<Item, Integer> POTION_RECIPE = new EnumMap<>(Item.class);

  static {
    POTION_RECIPE.put(Item.ASH, 1);
    POTION_RECIPE.put(Item.BANDAGE, 1);
    POTION_RECIPE.put(Item.BOTTLE, 1);
  }

  // Method to check if the potion can be brewed
  public static boolean canBrewPotion(Map<Integer, Integer> inventory) {
    for (Map.Entry<Item, Integer> entry : POTION_RECIPE.entrySet()) {
      Item item = entry.getKey();
      int requiredQuantity = entry.getValue();
      int itemIndex = item.ordinal();

      if (!inventory.containsKey(itemIndex) || inventory.get(itemIndex) < requiredQuantity) {
        return false;
      }
    }
    return true;
  }

  // Method to brew a potion
  public static Map<Integer, Integer> brewPotion(Map<Integer, Integer> inventory) {
    if (!canBrewPotion(inventory)) {
      return inventory;
    }

    // Deduct the required ingredients from the inventory
    for (Map.Entry<Item, Integer> entry : POTION_RECIPE.entrySet()) {
      Item item = entry.getKey();
      int requiredQuantity = entry.getValue();
      int itemIndex = item.ordinal();

      inventory.put(itemIndex, inventory.get(itemIndex) - requiredQuantity);
    }

    // Optionally, add the brewed potion to the inventory
    int potionIndex = Item.HPOTION.ordinal();
    inventory.put(potionIndex, inventory.getOrDefault(potionIndex, 0) + 1);

    return inventory;
  }
}

class BotConfig {
  int minLevel, maxLevel;
  int[] characteristics;
  Game.Item[] loot;
  String name;

  BotConfig(String name, int[] levels, int[] characteristics, Game.Item[] loot) {
    this.name = name;
    this.minLevel = levels[0];
    this.maxLevel = levels[1];
    this.characteristics = characteristics;
    this.loot = loot;
  }
}
