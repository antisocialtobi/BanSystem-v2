package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ConfigurationUtil {

    private Config config, messages, blacklist;
    private File configFile, messagesFile, blacklistFile;

    public ConfigurationUtil(Config config, Config messages, Config blacklist, File configFile, File messagesFile, File blacklistFile) {
        this.config = config;
        this.blacklist = blacklist;
        this.messages = messages;
        this.configFile = configFile;
        this.messagesFile = messagesFile;
        this.blacklistFile = blacklistFile;
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

    public void update() throws IOException {

        if(messages.get("History.body") != null)
            messages.set("History.body", null);
        if(messages.get("History.ban") == null)
            messages.set("History.ban", Arrays.asList(new String[]{
                "%P%§7Grund §8» §c%reason%",
                "%P%§7Erstelldatum §8» §c%creationdate%",
                "%P%§7Enddatum §8» §c%enddate%",
                "%P%§7Ersteller §8» §c%creator%",
                "%P%§7IP §8» §c%ip%",
                "%P%§7Type §8» §c%type%",
                "%P%§7ID §8» §c%ID%",
                "%P%"}));


        if(blacklist.getStringList("Whitelist") == null)
            blacklist.set("Whitelist", Arrays.asList(new String[]{"example.org", "www.example.org"}));


        config.save(configFile);
        messages.save(messagesFile);
        blacklist.save(blacklistFile);
    }
}
