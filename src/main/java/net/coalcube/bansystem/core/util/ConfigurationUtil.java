package net.coalcube.bansystem.core.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import net.coalcube.bansystem.core.BanSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ConfigurationUtil {

    private YamlDocument config, messages, blacklist;
    private File configFile;
    private File messagesFile;
    private File blacklistFile;
    private BanSystem banSystem;

    public ConfigurationUtil(YamlDocument config, YamlDocument messages, YamlDocument blacklist, BanSystem banSystem) {
        this.config = config;
        this.blacklist = blacklist;
        this.messages = messages;
        this.banSystem = banSystem;
    }

    public void createConfigs(File dataFolder) throws IOException {
        configFile = new File(dataFolder, "config.yml");
        messagesFile = new File(dataFolder, "messages.yml");
        blacklistFile = new File(dataFolder, "blacklist.yml");

        config = YamlDocument.create(
                configFile,
                banSystem.getResourceAsInputStream("config.yml"),
                GeneralSettings.builder().setUseDefaults(false).build(),
                LoaderSettings.builder().setCreateFileIfAbsent(true).setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("file-version"))
                        .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                        .addIgnoredRoute("1", "IDs", '.')
                        .addIgnoredRoute("2", "IDs", '.')
                        .addIgnoredRoute("3", "IDs", '.')
                        .addIgnoredRoute("4", "IDs", '.')
                        .addIgnoredRoute("5", "IDs", '.')
                        .addIgnoredRoute("6", "IDs", '.')
                        .addIgnoredRoute("7", "IDs", '.')
                        .addIgnoredRoute("8", "IDs", '.')
                        .addIgnoredRoute("9", "IDs", '.')
                        .addIgnoredRoute("10", "IDs", '.').build());

        messages = YamlDocument.create(
                messagesFile,
                banSystem.getResourceAsInputStream("messages.yml"),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("file-version"))
                        .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS)
                        .build());

        blacklist = YamlDocument.create(
                blacklistFile,
                banSystem.getResourceAsInputStream("blacklist.yml"),
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder()
                        .setVersioning(new BasicVersioning("file-version")).build());

        config.update(banSystem.getResourceAsInputStream("config.yml"));
        messages.update();
        blacklist.update();

        //update();
        config.save();
        messages.save();
        blacklist.save();
    }

    public String getMessage(String path) {
        String msg;
        if (messages.isList(path)) {
            msg = String.join("\n", messages.getStringList(path));
        } else {
            msg = messages.getString(path);
        }

        if (msg == null) {
            BanSystem.getInstance().getConsole().sendMessage("Missing message: " + path);
            return "Missing message: " + path;
        }

        if (msg.contains("%P%")) {
            msg = msg.replace("%P%", messages.getString("prefix"));
        }
        
        if (msg.contains("&")) {
            msg = msg.replace("&", "§");
        }
        return msg;
    }

    public void update() throws IOException {
        // Messages
        if(messages.get("History.body") != null)
            messages.set("History.body", null);
        updateConfigValue(messages, "messages.yml", "file-version");
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
        updateConfigValue(config, "config.yml", "file-version");
//        updateConfigValue(config, "config.yml", "webhook.enable");
//        updateConfigValue(config, "config.yml", "webhook.url");

        // Blacklist
        updateConfigValue(blacklist, "blacklist.yml", "file-version");
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

    private void updateConfigValue(YamlDocument config, String resource, String path) {
        Yaml resourceYAML = new Yaml();
        Map<String, Object> resourceData = resourceYAML.load(banSystem.getResourceAsInputStream(resource));
        if(config.get(path) == null) {
            config.set(path, getParameterValue(resourceData, path));
        }
    }

    public YamlDocument getConfig() {
        return config;
    }

    public YamlDocument getMessagesConfig() {
        return messages;
    }
    public YamlDocument getBlacklist() {
        return blacklist;
    }

    public File getMessagesFile() {
        return messagesFile;
    }

    public File getConfigFile() {
        return configFile;
    }

    public File getBlacklistFile() {
        return blacklistFile;
    }
}
