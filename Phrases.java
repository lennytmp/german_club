package FightLang;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

class Phrases {
  class CombatTexts {
    Map<String, String[]> adjective;
    Map<String, String[]> blocked;
    Map<String, String[]> but;
    Map<String, String[]> lookingForOpponent;
    Map<String, String[]> missed;
    Map<String, String[]> said;
    Map<String, String[]> wasDoingSomething;
    Map<String, String[]> wasTrying;
    Map<String, String[]> when;
    Map<String, String[]> wisdomIntro;
    Map<String, String[]> wisdoms;
    Map<String, String[]> won;

    Map<String, String[][]> hit;
    Map<String, String[][]> toHit;
  }

  class WiseTexts {
    Map<String, String[]> wisdomIntro;
    Map<String, String[]> wisdoms;
  }


  class Misc {
    Map<String, String> joinedTheFightClub;
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
