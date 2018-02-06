package FightLang;

import java.util.HashMap;
import java.util.Map;

class PhraseGenerator {
  static String[] languages = {"en", "ru"};

  static Map<String, String> getJoinedTheFightClub(String username) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang, username + " " +
        getTranslation(Phrases.miscTexts.joinedTheFightClub, lang));
    }
    return result;
  }

  static Map<String, String> getReadyToFightPhrase(Client client) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang, "\u2694 " + client.username + " " +
        Utils.getRnd(getTranslation(
            Phrases.combatTexts.lookingForOpponent, lang)) + ".");
    }
    return result;
  }

  static Map<String, String> getWonPhrase(Client winner, Client loser) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang, "\u2620 " + winner.username + " " +
        Utils.getRnd(getTranslation(Phrases.combatTexts.won, lang)) + " " +
        loser.username + ".");
    }
    return result;
  }

  static Map<String, String> getWisdom(Client client) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang,
        Utils.getRnd(getTranslation(Phrases.wiseTexts.wisdomIntro,
            client.lang)) + " " +
        Utils.getRnd(getTranslation(Phrases.wiseTexts.wisdoms,
            client.lang)));
    }
    return result;
  }

  static Map<String, String> getHitPhrase(Client offender,
                             Client victim,
                             Client.BodyPart part,
                             boolean critHit,
                             int damage) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      // Don't think about StringBuilder, it's a lie.
      // https://stackoverflow.com/questions/4965513/stringbuilder-vs-string-considering-replace 
      String tmp = victim.username + " " + Utils.getRnd(getTranslation(
            Phrases.combatTexts.wasDoingSomething, lang)) + ", " +
        Utils.getRnd(getTranslation(Phrases.combatTexts.when, lang)) + " " +
        Utils.getRnd(getTranslation(Phrases.combatTexts.adjective, lang)) +
        " " + offender.username +  " " + Utils.getRnd(getTranslation(
            Phrases.combatTexts.hit, lang)[part.ordinal()]) + ". ";
      if (critHit) {
        tmp += "Critical hit! ";
      }
      tmp += "-" + damage + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
      result.put(lang, tmp);
    }
    return result;
  }

  static String incorrectTranslationToVictim(Client offender,
                                             Client victim,
                                             String[] challengeWord) {
    return offender.username + " incorrectly translated the word `" +
           challengeWord[0] + "`. No damage to you!"
           + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
  }

  static String incorrectTranslationToOffender(Client offender,
                                               Client victim,
                                               String[] challengeWord) {
   return "Correct translation for `" + challengeWord[1] +
           "` is: `" + challengeWord[0] + "`. You caused no damage." +
          " [" + victim.hp + "/" + victim.getMaxHp() + "]";
  }

  static String correctTranslationToVictim(Client offender,
                                           Client victim,
                                           int damage,
                                           String[] challengeWord) {
    // Don't think about StringBuilder, it's a lie.
    // https://stackoverflow.com/questions/4965513/stringbuilder-vs-string-considering-replace 
    String  tmp = offender.username + " correctly translated the word `" +
                  challengeWord[0] + "`! ";
    if (damage > offender.getMaxDamage()) {
      tmp += " Critical hit! ";
    }
    tmp += "-" + damage + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
    return tmp;
  }

  static String correctTranslationToOffender(Client offender,
                                             Client victim,
                                             int damage,
                                             String[] challengeWord) {
    String  tmp = "Correct! ";
    if (damage > offender.getMaxDamage()) {
      tmp += " Critical hit! ";
    }
    tmp += "-" + damage + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
    return tmp;
  }


  static Map<String, String> getSayingPhrase(Client teller, String msg, Client listener) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang, teller.username + " " +
        Utils.getRnd(getTranslation(Phrases.combatTexts.said, lang)) + " " +
        listener.username + ": " + msg);
    }
    return result;
  }

  static Map<String, String> getLangMap(String message) {
    Map<String, String> result = new HashMap<>();
    for (String lang : languages) {
      result.put(lang, message);
    }
    return result;
  }

  private static <K> K getTranslation(Map<String, K> translations,
                                       String targetLang) {
    if (translations.get(targetLang) == null) {
      if (translations.get("en") == null) {
        Logger.logException(new Exception("No language for the phrase:" +
            translations.toString()));
      }
      return translations.get("en");
    }
    return translations.get(targetLang);
  }
};
