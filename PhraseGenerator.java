package FightLang;


class PhraseGenerator {
  static String getJoinedTheFightClub(String username) {
    return username + " " + Phrases.miscTexts.joinedTheFightClub;
  }

  static String getReadyToFightPhrase(Client client) {
    return "\u2694 " + client.username + " " +
        Utils.getRnd(Phrases.combatTexts.lookingForOpponent) + ".";
  }

  static String getWonPhrase(Client winner, Client loser) {
    return "\u2620 " + winner.username + " " +
        Utils.getRnd(Phrases.combatTexts.won) + " " + loser.username + ".";
  }

  static String getWisdom(Client client) {
    return Utils.getRnd(Phrases.wiseTexts.wisdomIntro) + " " +
        Utils.getRnd(Phrases.wiseTexts.wisdoms);
  }


  static String attackToVictim(Client offender, Client victim, int damage) {
    // Don't think about StringBuilder, it's a lie.
    // https://stackoverflow.com/questions/4965513/stringbuilder-vs-string-considering-replace 
    String  tmp = "";
    if (damage > offender.getMaxDamage()) {
      tmp += " Kritischer Treffer! ";
    }
    tmp += "-" + damage + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
    return tmp;
  }

  static String attackToOffender(Client offender, Client victim, int damage) {
    String  tmp = "";
    if (damage > offender.getMaxDamage()) {
      tmp += " Kritischer Treffer! ";
    }
    tmp += "-" + damage + " [" + victim.hp + "/" + victim.getMaxHp() + "]";
    return tmp;
  }
};
