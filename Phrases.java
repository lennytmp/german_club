package FightLang;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

class Phrases {
  class CombatTexts {
    String[] adjective;
    String[] blocked;
    String[] but;
    String[] lookingForOpponent;
    String[] missed;
    String[] said;
    String[] wasDoingSomething;
    String[] wasTrying;
    String[] when;
    String[] won;

    String[][] hit;
    String[][] toHit;
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
