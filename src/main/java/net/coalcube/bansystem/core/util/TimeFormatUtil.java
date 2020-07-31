package net.coalcube.bansystem.core.util;

import java.util.ArrayList;
import java.util.List;

public class TimeFormatUtil {

    public String getFormattedRemainingTime(Long remainingTime) {
        long millis = remainingTime;

        List<Long> array = new ArrayList<>();

        long seconds = 0;
        long minutes = 0;
        long hours = 0;
        long days = 0;
        while (millis > 999) {
            millis -= 1000;
            seconds++;
        }
        while (seconds > 59) {
            seconds -= 60;
            minutes++;
        }
        while (minutes > 59) {
            minutes -= 60;
            hours++;
        }
        while (hours > 23) {
            hours -= 24;
            days++;
        }

        array.add(seconds);
        array.add(minutes);
        array.add(hours);
        array.add(days);

        String formattedRemainingTime;



        if (remainingTime != -1) {
            if (days > 0) {
                formattedRemainingTime = "§e" + days + " §cTag(e), §e" + hours + " §cStunde(n), §e" + minutes
                        + " §cMinute(n) und §e" + seconds + " §cSekunde(n)";
            } else if (hours > 0) {
                formattedRemainingTime = "§e" + hours + " §cStunde(n), §e" + minutes + " §cMinute(n) und §e" + seconds
                        + " §cSekunde(n)";
            } else if (minutes > 0) {
                formattedRemainingTime = "§e" + minutes + " §cMinute(n) und §e" + seconds + " §cSekunde(n)";
            } else {
                formattedRemainingTime = "§e" + seconds + " §cSekunde(n)";
            }
        } else {
            formattedRemainingTime = "§4§lPERMANENT";
        }
        return formattedRemainingTime;
    }
}
