package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
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
        Yaml messagesResource = new Yaml();
        Map<String, Object> yamlData = messagesResource.load(banSystem.getResourceAsStream("messages.yml"));

        // Messages
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
        if(messages.get("bansystem.usage.showlogs") == null)
            messages.set("bansystem.usage.showlogs", "%P%§7Benutze §e/bansystem logs show");
        if(messages.get("bansystem.usage.clearlogs") == null)
            messages.set("bansystem.usage.clearlogs", "%P%§7Benutze §e/bansystem logs clear");
        if(messages.get("bansystem.logs.clear.success") == null)
            messages.set("bansystem.logs.clear.success", "%P%§7Die Logs wurden §2erfolgreich §7gelöscht.");
        if(messages.get("bansystem.logs.clear.failed") == null)
            messages.set("bansystem.logs.clear.failed", "%P%§cDie Logs konnten nicht gelöscht werden.");
        if(messages.get("bansystem.logs.show.empty") == null)
            messages.set("bansystem.logs.show.empty", "%P%§cEs sind keine Logs vorhanden.");
        if(messages.get("bansystem.logs.show.header") == null)
            messages.set("bansystem.logs.show.header", Arrays.asList(new String[] {
                    "%P%§8§m---------------§7» §7Logs §7«§8§m---------------",
                    "§fDatum     §7-     §eErsteller §7- §7Aktion - §eSpieler §7- §bNotiz"}));
        if(messages.get("bansystem.logs.show.body") == null)
            messages.set("bansystem.logs.show.body", "§f%date% §e%creator% §7%action% §e%target% §b%note%");
        if(messages.get("bansystem.logs.show.footer") == null)
            messages.set("bansystem.logs.show.footer", "%P%%previous% §8§m-------§r %next% §8[§7Seite %curpage%/%maxpage% §8]");
        if(messages.get("bansystem.logs.show.button.next") == null)
            messages.set("bansystem.logs.show.button.next", "§2§lWeiter §2»");
        if(messages.get("bansystem.logs.show.button.previous") == null)
            messages.set("bansystem.logs.show.button.previous", "§c« §c§lZurück");
        if(messages.get("bansystem.logs.show.pageNotFound") == null)
            messages.set("bansystem.logs.show.pageNotFound", "%P%§cDie angegebene Seite wurde nicht gefunden. Die Maximale Seitenanzahl beträgt §e%maxpage%§7.");
        if(messages.get("bansystem.logs.show.invalidInput") == null)
            messages.set("bansystem.logs.show.invalidInput", "%P%§cUngültige Eingabe. Bitte geben Sie eine Seitenzahl als Zahl an. 1-%maxpage%");


        // Blacklist
        if(blacklist.getStringList("Whitelist") == null)
            blacklist.set("Whitelist", Arrays.asList(new String[]{"example.org", "www.example.org"}));

        // Safe configs
        config.save(configFile);
        messages.save(messagesFile);
        blacklist.save(blacklistFile);
    }

    public static Object getParameterValue(Map<String, Object> yamlData, String path) {
        String[] keys = path.split("\\."); // Split the path by dot
        Object value = yamlData;
        for (String key : keys) {
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                // Key not found or reached a non-map value
                return null;
            }
        }
        return value;
    }
}
