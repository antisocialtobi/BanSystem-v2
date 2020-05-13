package net.coalcube.bansystem.spigot;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.listener.AsyncPlayerChatListener;
import net.coalcube.bansystem.spigot.listener.ChatListener;
import net.coalcube.bansystem.spigot.util.SpigotConfig;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanSystemSpigot extends JavaPlugin implements BanSystem {

    private static Plugin instance;
    private static BanManager banmanager;

    public static MySQL mysql;
    private UpdateChecker updatechecker;
    private PluginManager pluginmanager;
    private UpdateManager updatemanager;
    private ServerSocket serversocket;
    private TimeFormatUtil timeFormatUtil;
    public static Config config;
    public static Config messages;
    public static Config blacklist;
    public static Config bans;
    public static Config banhistories;
    public static Config unbans;
    public static String Banscreen;
    private static File fileDatabaseFolder;
    private static String hostname, database, user, pw;
    private static int port;
    private static CommandSender console;
    public static String PREFIX = "§8§l┃ §cBanSystem §8» §7",
            NOPERMISSION, NOPLAYER, NODBCONNECTION;

    @Override
    public void onEnable() {
        super.onEnable();

        BanSystem.setInstance(this);

        instance = this;
        pluginmanager = Bukkit.getPluginManager();
        console = Bukkit.getConsoleSender();
        updatechecker = new UpdateChecker(65863);
        timeFormatUtil = new TimeFormatUtil();

        console.sendMessage("§c  ____                    ____                  _                      ");
        console.sendMessage("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  ");
        console.sendMessage("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ ");
        console.sendMessage("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |");
        console.sendMessage("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|");
        console.sendMessage("§c                                  |___/                           §7v2.0");

        createConfig();
        loadConfig();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            banmanager = new BanManagerMySQL(config, messages, mysql);
            try {
                mysql.connect();
                console.sendMessage(PREFIX + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                console.sendMessage(PREFIX + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
            }
            try {
                if(mysql.isConnected()) {
                    mysql.createTables(config);
                    console.sendMessage(PREFIX + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                console.sendMessage(PREFIX + e.getMessage() + " " + e.getCause());
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    console.sendMessage(PREFIX + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                console.sendMessage(PREFIX + e.getMessage() + " " + e.getCause());
                e.printStackTrace();
            }

        } else {
            fileDatabaseFolder = new File(this.getDataFolder().getPath() + "/database");
            createFileDatabase();
            banmanager = new BanManagerFile(config, messages, bans, banhistories, unbans, fileDatabaseFolder);
            mysql = null;
        }

        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                UUIDFetcher.clearCache();
            }
        }, 72000, 72000);

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.autoban.enable"))
            console.sendMessage(
                    PREFIX + "§cBitte trage die IP des Servers in der config.yml ein.");


        console.sendMessage(PREFIX + "§7Das BanSystem wurde gestartet.");

        updatemanager = new UpdateManager(mysql);

        try {
            if (updatechecker.checkForUpdates()) {
                console.sendMessage(PREFIX + "§cEin neues Update ist verfügbar.");
                console.sendMessage(PREFIX + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        new Thread(() -> {
//            try {
//                serversocket = new ServerSocket(6000);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();

        init(pluginmanager);

    }

    @Override
    public void onDisable() {
        super.onDisable();

        try {
            if (config.getBoolean("mysql.enable")) {
                if (mysql.isConnected())
                    mysql.disconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PluginManager pm = Bukkit.getPluginManager();


        AsyncPlayerChatEvent.getHandlerList().unregister(instance);
        /*PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
        PlayerQuitEvent.getHandlerList().unregister(instance);
        PlayerJoinEvent.getHandlerList().unregister(instance);
        PlayerPreLoginEvent.getHandlerList().unregister(instance);*/

        console.sendMessage(BanSystemSpigot.PREFIX + "§7Das BanSystem wurde gestoppt.");

    }


    // create Config files
    private void createConfig() {
        try {
            File configfile = new File(this.getDataFolder(), "config.yml");
            if(!this.getDataFolder().exists()) {
                this.getDataFolder().mkdir();
            }
            if(!configfile.exists()) {
                configfile.createNewFile();

                config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));

                ConfigurationUtil.initConfig(config);

                config.save(configfile);
            }
            File messagesfile = new File(this.getDataFolder(), "messages.yml");
            if(!messagesfile.exists()) {
                messagesfile.createNewFile();

                messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));

                ConfigurationUtil.initMessages(messages);

                messages.save(messagesfile);

            }
            messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
            config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
        } catch (IOException e) {
            System.err.println("[Bansystem] Dateien konnten nicht erstellt werden.");
        }
    }

    private void createFileDatabase() {
        try {
            if (!fileDatabaseFolder.exists()) {
                fileDatabaseFolder.mkdir();
            }
            File bansfile = new File(fileDatabaseFolder.getPath(), "bans.yml");
            if (!bansfile.exists()) {
                bansfile.createNewFile();
                bans = new SpigotConfig(YamlConfiguration.loadConfiguration(bansfile));
            }
            File banhistoriesfile = new File(fileDatabaseFolder.getPath(), "banhistories.yml");
            if (!banhistoriesfile.exists()) {
                banhistoriesfile.createNewFile();
                banhistories = new SpigotConfig(YamlConfiguration.loadConfiguration(banhistoriesfile));
            }
            File unbansfile = new File(fileDatabaseFolder.getPath(), "unbans.yml");
            if (!unbansfile.exists()) {
                unbansfile.createNewFile();
                unbans = new SpigotConfig(YamlConfiguration.loadConfiguration(unbansfile));
            }

            bans = new SpigotConfig(YamlConfiguration.loadConfiguration(bansfile));
            banhistories = new SpigotConfig(YamlConfiguration.loadConfiguration(banhistoriesfile));
            unbans = new SpigotConfig(YamlConfiguration.loadConfiguration(unbansfile));
        } catch (IOException e) {
            console.sendMessage(PREFIX + "Die Filedatenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            PREFIX = messages.getString("prefix").replaceAll("&", "§");
            NOPERMISSION = messages.getString("NoPermissionMessage").replaceAll("%P%", PREFIX).replaceAll("&", "§");
            NOPLAYER = messages.getString("NoPlayerMessage").replaceAll("%P%", PREFIX).replaceAll("&", "§");
            NODBCONNECTION = messages.getString("NoMySQLconnection").replaceAll("%P%", PREFIX).replaceAll("&", "§");

            Banscreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (Banscreen == null) {
                    Banscreen = screen.replaceAll("%P%", PREFIX) + "\n";
                } else
                    Banscreen = Banscreen + screen.replaceAll("%P%", PREFIX) + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");
        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten. "
                    + e.getMessage());
        }
    }

    @Override
    public User getUser(String name) {
        SpigotUser su = new SpigotUser(Bukkit.getPlayer(name));
        if(su != null)
            return su;

        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof Player) {
            ((Player) u.getRawUser()).disconnect(msg);
        }
    }

    @Override
    public Config getMessages() {
        return messages;
    }

    @Override
    public Config getConfiguration() {
        return config;
    }

    private void init(PluginManager pluginManager) {
        getCommand("ban").setExecutor(new CommandWrapper(new CMDban(banmanager, config, messages, mysql),true));
        getCommand("check").setExecutor(new CommandWrapper(new CMDcheck(banmanager, mysql, messages), true));
        getCommand("deletehistory").setExecutor(new CommandWrapper(new CMDdeletehistory(banmanager, messages, mysql), true));
        getCommand("history").setExecutor(new CommandWrapper(new CMDhistory(banmanager, messages, mysql), true));
        getCommand("kick").setExecutor(new CommandWrapper(new CMDkick(messages, mysql), true));
        getCommand("unban").setExecutor(new CommandWrapper(new CMDunban(banmanager, mysql, messages, config), true));
        getCommand("unmute").setExecutor(new CommandWrapper(new CMDunmute(banmanager, messages, config, mysql), true));
        getCommand("bansystem").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, mysql), false));
        getCommand("bansys").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, mysql), false));

        pluginManager.registerEvents(new AsyncPlayerChatListener(config, messages, banmanager), this);
    }

    public MySQL getMySQL() {
        return mysql;
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            users.add(new SpigotUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new SpigotUser(Bukkit.getConsoleSender());
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }

    public static BanManager getBanmanager() {
        return banmanager;
    }
}
