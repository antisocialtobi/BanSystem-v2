package net.coalcube.bansystem.core.util;

import java.util.ArrayList;
import java.util.List;

public class TimeFormatUtil {

    private static Config config;

    public TimeFormatUtil(Config config) {
        this.config = config;
    }

    public String getFormattedRemainingTime(Long remainingTime) {
        long millis = remainingTime;

        if(millis == 0)
            return "§e0 §cSekunde(n)";
        if(millis == -1)
            return "§4§lPERMANENT";

        List<String> array = new ArrayList<>();

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

        if(seconds > 0) array.add(config.getString("TimeFormat.seconds").replaceAll("%sec%", String.valueOf(seconds)));
        if(minutes > 0) array.add(config.getString("TimeFormat.minutes").replaceAll("%sec%", String.valueOf(seconds)));;
        if(hours > 0) array.add(config.getString("TimeFormat.hours").replaceAll("%sec%", String.valueOf(seconds)));
        if(days > 0) array.add(config.getString("TimeFormat.days").replaceAll("%sec%", String.valueOf(seconds)));

        String formattedRemainingTime = null;

        if (array.size() >= 1) {
            int size = array.size();

            if(size == 1)
                formattedRemainingTime = array.get(0);
            if(size == 2)
                formattedRemainingTime = array.get(1) + " und " + array.get(0);
            if(size == 3)
                formattedRemainingTime = array.get(2) + ", " + array.get(1) + " und " + array.get(0);
            if(size == 4)
                formattedRemainingTime = array.get(3) + ", " + array.get(2) + ", " + array.get(1) + " und " + array.get(0);

        } else {
            return "§e0 §cSekunde(n)";
        }

        return formattedRemainingTime;
    }
}
