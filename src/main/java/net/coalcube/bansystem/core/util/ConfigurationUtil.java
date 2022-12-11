package net.coalcube.bansystem.core.util;

import java.util.List;

public class ConfigurationUtil {

    private Config config, messages, blacklist;

    public ConfigurationUtil(Config config, Config messages, Config blacklist) {
        this.config = config;
        this.blacklist = blacklist;
        this.messages = messages;
    }

    public String getMessage(String path) {
        String msg = "";

        if(messages.get(path) instanceof List) {
            int count = 0;
            for(String line : messages.getStringList(path)) {
                if(messages.getStringList(path).size()-1 == count) {
                    msg = msg + line;
                } else {
                    msg = msg + line + "\n";
                }
                count ++;
            }
        } else
            msg = messages.getString(path);

        if(msg.contains("&"))
            msg = msg.replaceAll("&", "§");

        if(msg.contains("%P%"))
            msg = msg.replaceAll("%P%", messages.getString("prefix").replaceAll("&", "§"));

        return msg;
    }
}
