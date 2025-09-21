package FightLang;

import java.util.EnumMap;
import java.util.Map;

class Game {
  static final BotConfig[] BOT_TYPES = {
      new BotConfig("Bettler",
          new int[] { 1, 3 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.COIN }),

      new BotConfig("Betrunkener",
          new int[] { 1, 3 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.BOTTLE }),

      new BotConfig("Crack-Süchtiger",
          new int[] { 1, 3 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.CRACK }),

      new BotConfig("Skelett",
          new int[] { 4, 6 },
          new int[] { 4, 4, 1 },
          new Item[] { Item.BONE }),

      new BotConfig("Zombie",
          new int[] { 4, 6 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.FLESH }),

      new BotConfig("Geist",
          new int[] { 4, 6 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.SOUL_STONE }),

      new BotConfig("Mumie",
          new int[] { 7, 9 },
          new int[] { 1, 1, 1 },
          new Item[] { Item.BANDAGE }),

      new BotConfig("Lich",
          new int[] { 7, 9 },
          new int[] { 4, 4, 1 },
          new Item[] { Item.WAX }),

      new BotConfig("Vampir",
          new int[] { 7, 9 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.FANG, Item.CLAW }),

      new BotConfig("Ghul",
          new int[] { 10, 12 },
          new int[] { 6, 1, 1 },
          new Item[] { Item.CLAW }),

      new BotConfig("Untoter",
          new int[] { 10, 12 },
          new int[] { 1, 6, 1 },
          new Item[] { Item.ASH }),

      new BotConfig("Nekromant",
          new int[] { 10, 12 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.PAPER, Item.ASH }),

      new BotConfig("Teufel",
          new int[] { 12, 20 },
          new int[] { 1, 1, 6 },
          new Item[] { Item.SILVER, Item.GOLD }),

      new BotConfig("Dämon",
          new int[] { 12, 20 },
          new int[] { 1, 1, 1 },
          new Item[] { Item.GOLD, Item.SILVER })
  };

  enum Item {
    // TODO: when an item is added to the list - clients needs to be updated with a
    // larger array.
    COIN("Münze", "Münzen"), BOTTLE("Flasche", "Flaschen"),
    CRACK("Gramm Crack", "Gramm Crack"), BONE("Knochen", "Knochen"),
    FLESH("Fleischstück", "Fleischstücke"), SOUL_STONE("Seelenstein", "Seelensteine"),
    BANDAGE("Bandage", "Bandagen"), WAX("Gramm Wachs", "Gramm Wachs"),
    FANG("Reißzahn", "Reißzähne"), CLAW("Klaue", "Klauen"), ASH("Gramm Asche", "Gramm Asche"),
    PAPER("Blatt Papier", "Blätter Papier"), SILVER("Silberstück", "Silberstücke"),
    GOLD("Goldstück", "Goldstücke"), HPOTION("Heiltrank", "Heiltränke"),
    SPOTION("Stärketrank", "Stärketränke"), LPOTION("Glückstrank", "Glückstränke");

    String singular, plural;

    private Item(String singular, String plural) {
      this.singular = singular;
      this.plural = plural;
    }
  };

  static final Item[] ITEM_VALUES = Item.values();

  // Define the recipe for brewing a healing potion
  private static final Map<Item, Integer> POTION_RECIPE = new EnumMap<>(Item.class);
  // Define the recipe for brewing a strength potion
  private static final Map<Item, Integer> STRENGTH_POTION_RECIPE = new EnumMap<>(Item.class);
  // Define the recipe for brewing a luck potion
  private static final Map<Item, Integer> LUCK_POTION_RECIPE = new EnumMap<>(Item.class);

  static {
    POTION_RECIPE.put(Item.ASH, 1);
    POTION_RECIPE.put(Item.BANDAGE, 1);
    POTION_RECIPE.put(Item.BOTTLE, 1);
    
    STRENGTH_POTION_RECIPE.put(Item.BONE, 1);
    STRENGTH_POTION_RECIPE.put(Item.FLESH, 1);
    STRENGTH_POTION_RECIPE.put(Item.FANG, 1);
    
    LUCK_POTION_RECIPE.put(Item.COIN, 1);
    LUCK_POTION_RECIPE.put(Item.GOLD, 1);
    LUCK_POTION_RECIPE.put(Item.SILVER, 1);
  }

  // Generic method to check if a potion can be brewed based on recipe
  private static boolean canBrewPotionWithRecipe(Map<Integer, Integer> inventory, Map<Item, Integer> recipe) {
    for (Map.Entry<Item, Integer> entry : recipe.entrySet()) {
      Item item = entry.getKey();
      int requiredQuantity = entry.getValue();
      int itemIndex = item.ordinal();

      if (!inventory.containsKey(itemIndex) || inventory.get(itemIndex) < requiredQuantity) {
        return false;
      }
    }
    return true;
  }

  // Method to check if the healing potion can be brewed
  public static boolean canBrewPotion(Map<Integer, Integer> inventory) {
    return canBrewPotionWithRecipe(inventory, POTION_RECIPE);
  }

  // Generic method to brew a potion based on recipe and result potion type
  private static Map<Integer, Integer> brewPotionWithRecipe(Map<Integer, Integer> inventory, 
                                                           Map<Item, Integer> recipe, 
                                                           Item resultPotion) {
    if (!canBrewPotionWithRecipe(inventory, recipe)) {
      return inventory;
    }

    // Deduct the required ingredients from the inventory
    for (Map.Entry<Item, Integer> entry : recipe.entrySet()) {
      Item item = entry.getKey();
      int requiredQuantity = entry.getValue();
      int itemIndex = item.ordinal();

      inventory.put(itemIndex, inventory.get(itemIndex) - requiredQuantity);
    }

    // Add the brewed potion to the inventory
    int potionIndex = resultPotion.ordinal();
    inventory.put(potionIndex, inventory.getOrDefault(potionIndex, 0) + 1);

    return inventory;
  }

  // Method to brew a healing potion
  public static Map<Integer, Integer> brewPotion(Map<Integer, Integer> inventory) {
    return brewPotionWithRecipe(inventory, POTION_RECIPE, Item.HPOTION);
  }

  // Method to check if the strength potion can be brewed
  public static boolean canBrewStrengthPotion(Map<Integer, Integer> inventory) {
    return canBrewPotionWithRecipe(inventory, STRENGTH_POTION_RECIPE);
  }

  // Method to brew a strength potion
  public static Map<Integer, Integer> brewStrengthPotion(Map<Integer, Integer> inventory) {
    return brewPotionWithRecipe(inventory, STRENGTH_POTION_RECIPE, Item.SPOTION);
  }

  // Method to check if the luck potion can be brewed
  public static boolean canBrewLuckPotion(Map<Integer, Integer> inventory) {
    return canBrewPotionWithRecipe(inventory, LUCK_POTION_RECIPE);
  }

  // Method to brew a luck potion
  public static Map<Integer, Integer> brewLuckPotion(Map<Integer, Integer> inventory) {
    return brewPotionWithRecipe(inventory, LUCK_POTION_RECIPE, Item.LPOTION);
  }

  // Method to get all brewable potions with their names
  public static String[] getBrewableOptions(Map<Integer, Integer> inventory) {
    java.util.List<String> brewableOptions = new java.util.ArrayList<>();
    
    if (canBrewPotion(inventory)) {
      brewableOptions.add("Heiltrank brauen");
    }
    if (canBrewStrengthPotion(inventory)) {
      brewableOptions.add("Stärketrank brauen");
    }
    if (canBrewLuckPotion(inventory)) {
      brewableOptions.add("Glückstrank brauen");
    }
    
    return brewableOptions.toArray(new String[0]);
  }

  // Method to check if any potion can be brewed
  public static boolean canBrewAnyPotion(Map<Integer, Integer> inventory) {
    return canBrewPotion(inventory) || canBrewStrengthPotion(inventory) || canBrewLuckPotion(inventory);
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
