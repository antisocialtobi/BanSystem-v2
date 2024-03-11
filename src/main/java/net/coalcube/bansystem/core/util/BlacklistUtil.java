package net.coalcube.bansystem.core.util;

public class BlacklistUtil {

    private Config blacklist;
    public BlacklistUtil(Config blacklist) {
        this.blacklist = blacklist;
    }
    public boolean hasBlockedWordsContains(String message) {
        message = message.trim();
        message = message.replaceAll("AE", "Ä");
        message = message.replaceAll("OE", "Ö");
        message = message.replaceAll("UE", "Ü");
        message = message.replaceAll("Ä", "AE");
        message = message.replaceAll("Ö", "OE");
        message = message.replaceAll("Ü", "UE");
        message = message.replaceAll("Punkt", ".");
        message = message.replaceAll("Point", ".");
        message = message.replaceAll("0", "O");
        message = message.replaceAll("1", "I");
        message = message.replaceAll("3", "E");
        message = message.replaceAll("4", "A");
        message = message.replaceAll("5", "S");
        message = message.replaceAll("8", "B");
        String[] trimmed = message.split(" ");

        for (String word : blacklist.getStringList("Words")) {
            if (message.contains(word) || message.equalsIgnoreCase(word) || message.toUpperCase().equals(word) || message.toLowerCase().equals(word))
                return true;

            for(String pice : trimmed) {
                if(pice.equalsIgnoreCase(word))
                    return true;
            }
        }
        return false;
    }

    public boolean hasAdContains(String message) {
        String rawMessage = message;

        message = message.trim();
        message = message.replaceAll("0", "O");
        message = message.replaceAll("1", "I");
        message = message.replaceAll("3", "E");
        message = message.replaceAll("4", "A");
        message = message.replaceAll("5", "S");
        message = message.replaceAll("8", "B");
        message = message.replaceAll("Ä", "AE");
        message = message.replaceAll("Ö", "OE");
        message = message.replaceAll("Ü", "UE");
        message = message.replaceAll("ä", "ae");
        message = message.replaceAll("ö", "oe");
        message = message.replaceAll("ü", "ue");
        message = message.replaceAll("Punkt", ".");
        message = message.replaceAll("Point", ".");

        String[] trimmed = message.split(" ");

        for(String ad : blacklist.getStringList("Ads")) {
            if(message.contains(ad)
                    || message.equalsIgnoreCase(ad)
                    || message.toUpperCase().equals(ad)
                    || message.toLowerCase().equals(ad)
                    || rawMessage.contains(ad)
                    || rawMessage.equalsIgnoreCase(ad)
                    || rawMessage.toUpperCase().equals(ad)
                    || rawMessage.toLowerCase().equals(ad))

                return true;

            for(String word : trimmed) {
                if(word.equalsIgnoreCase(ad))
                    return true;
            }

        }
        return false;
    }
}
