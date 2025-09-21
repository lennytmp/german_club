package FightLang;

import com.google.gson.Gson;


class Phrases {
  class CombatTexts {
    String[] lookingForOpponent;
    String[] won;
  }

  class WiseTexts {
    String[] wisdomIntro;
    String[] wisdoms;
  }


  class Misc {
    String joinedTheFightClub;
  }

  static WiseTexts wiseTexts;
  static CombatTexts combatTexts;
  static Misc miscTexts;

  public static void initialize() {
    Gson g = new Gson();
    //TODO(lenny): dehardcode file names?
    //TODO(lenny): make a stronger file validation to avoid nulls inside arrays
    String jsonStr = Logger.readAllFile("./text/combats.json");
    combatTexts = g.fromJson(jsonStr, CombatTexts.class);

    jsonStr = Logger.readAllFile("./text/wise.json");
    wiseTexts = g.fromJson(jsonStr, WiseTexts.class);

    jsonStr = Logger.readAllFile("./text/misc.json");
    miscTexts = g.fromJson(jsonStr, Misc.class);
  }
}
