package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfigurationUtil {

    private Config config, messages, blacklist;
    private File configFile, messagesFile, blacklistFile;
    private BanSystem banSystem;

    public ConfigurationUtil(Config config, Config messages, Config blacklist, File configFile, File messagesFile, File blacklistFile, BanSystem banSystem) {
        this.config = config;
        this.blacklist = blacklist;
        this.messages = messages;
        this.configFile = configFile;
        this.messagesFile = messagesFile;
        this.blacklistFile = blacklistFile;
        this.banSystem = banSystem;
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


        // Messages
        if(messages.get("History.body") != null)
            messages.set("History.body", null);
        updateConfigValue(messages, "messages.yml", "History.ban");
        updateConfigValue(messages, "messages.yml", "History.kick");
        updateConfigValue(messages, "messages.yml", "History.kickWithReason");
        updateConfigValue(messages, "messages.yml", "History.unmute");
        updateConfigValue(messages, "messages.yml", "History.unmuteWithReason");
        updateConfigValue(messages, "messages.yml", "History.unban");
        updateConfigValue(messages, "messages.yml", "History.unbanWithReason");
        updateConfigValue(messages, "messages.yml", "History.clearedHistory");
        updateConfigValue(messages, "messages.yml", "bansystem.usage.showlogs");
        updateConfigValue(messages, "messages.yml", "bansystem.usage.clearlogs");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.clear success");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.clear.failed");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.empty");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.header");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.body");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.footer");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.button.next");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.button.previous");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.header");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.pageNotFound");
        updateConfigValue(messages, "messages.yml", "bansystem.logs.show.invalidInput");

        // config
        updateConfigValue(config, "config.yml", "webhook.enable");
        updateConfigValue(config, "config.yml", "webhook.url");

        // Blacklist
        updateConfigValue(blacklist, "blacklist.yml", "Whitelist");

        // Safe configs
        config.save(configFile);
        messages.save(messagesFile);
        blacklist.save(blacklistFile);
    }

    private Object getParameterValue(Map<String, Object> yamlData, String path) {
        String[] keys = path.split("\\.");
        Object value = null;
        int i=1;
        for(String key : keys) {
            if(i == 1) {
                value = yamlData.get(keys[0]);
            } else {
                if (value instanceof Map) {
                    value = ((Map<?, ?>) value).get(key);
                } else if (value != null && keys.length == i) {
                    // Value have
                    System.out.println("value: " + value + "\n");
                    return value;
                } else {
                    // Value is null
                    return null;
                }
            }
            i++;
        }
        return value;
    }

    private void updateConfigValue(Config config, String resource, String path) {
        Yaml resourceYAML = new Yaml();
        Map<String, Object> resourceData = resourceYAML.load(banSystem.getResourceAsInputStream(resource));
        if(config.get(path) == null) {
            config.set(path, getParameterValue(resourceData, path));
        }
    }

    public Config getConfig() {
        return config;
    }

    public Config getMessagesConfig() {
        return messages;
    }
}
