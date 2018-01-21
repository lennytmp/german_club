package ChatBot;

class Game {
  static final BotConfig[] BOT_TYPES = {
    new BotConfig("Beggar",
                  new int[] {1,3},
                  new int[] {1,1,6},
                  new Item[] {Item.COIN}),

    new BotConfig("Drunk",
                  new int[] {1,3},
                  new int[] {1,6,1},
                  new Item[] {Item.BOTTLE}),

    new BotConfig("Crackhead",
                  new int[] {1,3},
                  new int[] {6,1,1},
                  new Item[] {Item.CRACK}),

    new BotConfig("Skeleton",
                  new int[] {4,6},
                  new int[] {4,4,1},
                  new Item[] {Item.BONE}),

    new BotConfig("Zombie",
                  new int[] {4,6},
                  new int[] {6,1,1},
                  new Item[] {Item.FLESH}),

    new BotConfig("Ghost",
                  new int[] {4,6},
                  new int[] {1,1,6},
                  new Item[] {Item.SOUL_STONE}),

    new BotConfig("Mummy",
                  new int[] {7,9},
                  new int[] {1,1,1},
                  new Item[] {Item.BANDAGE}),

    new BotConfig("Lich",
                  new int[] {7,9},
                  new int[] {4,4,1},
                  new Item[] {Item.WAX}),

    new BotConfig("Vampire",
                  new int[] {7,9},
                  new int[] {1,6,1},
                  new Item[] {Item.FANG, Item.CLAW}),

    new BotConfig("Ghoul",
                  new int[] {10,12},
                  new int[] {6,1,1},
                  new Item[] {Item.CLAW}),

    new BotConfig("Undead",
                  new int[] {10,12},
                  new int[] {1,6,1},
                  new Item[] {Item.ASH}),

    new BotConfig("Necromant",
                  new int[] {10,12},
                  new int[] {1,1,6},
                  new Item[] {Item.PAPER, Item.ASH}),

    new BotConfig("Devil",
                  new int[] {12,20},
                  new int[] {1,1,6},
                  new Item[] {Item.SILVER, Item.GOLD}),

    new BotConfig("Demon",
                  new int[] {12,20},
                  new int[] {1,1,1},
                  new Item[] {Item.GOLD, Item.SILVER})
  };

  enum Item {
    // TODO: when an item is added to the list - clients needs to be updated with a
    // larger array.
    COIN("coin", "coins"), BOTTLE("bottle", "bottles"), 
    CRACK("gr of crack", "gr of crack"), BONE("bone", "bones"),
    FLESH("piece of flesh", "pieces of flesh"), SOUL_STONE("soul stone", "soul stones"),
    BANDAGE("bandage", "bandages"), WAX("gr of wax", "gr of wax"),
    FANG("fang", "fangs"), CLAW("claw", "claws"), ASH("gr of ash", "gr of ash"),
    PAPER("sheet of paper", "sheets of paper"), SILVER("silver piece", "silver pieces"),
    GOLD("golden piece", "golden pieces"), HPOTION("healing potion", "healing potions");

    String singular, plural;

    private Item(String singular, String plural) {
      this.singular = singular;
      this.plural = plural;
    }
  };

  static final Item[] ITEM_VALUES = Item.values();
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

