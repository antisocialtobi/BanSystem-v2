package net.coalcube.bansystem.bungee;

import net.coalcube.bansystem.bungee.event.ChatListener;
import net.coalcube.bansystem.bungee.event.LoginListener;
import net.coalcube.bansystem.bungee.util.BungeeConfig;
import net.coalcube.bansystem.bungee.util.BungeePlayer;
import net.coalcube.bansystem.bungee.util.BungeeUser;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BanSystemBungee extends Plugin implements BanSystem {

    private static Plugin instance;
    private static BanManager banmanager;

    private MySQL mysql;
    private UpdateChecker updatechecker;
    private PluginManager pluginmanager;
    private UpdateManager updatemanager;
    private ServerSocket serversocket;
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
        pluginmanager = ProxyServer.getInstance().getPluginManager();
        console = ProxyServer.getInstance().getConsole();
        updatechecker = new UpdateChecker(65863);

        console.sendMessage(new TextComponent("§c  ____                    ____                  _                      "));
        console.sendMessage(new TextComponent("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  "));
        console.sendMessage(new TextComponent("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ "));
        console.sendMessage(new TextComponent("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |"));
        console.sendMessage(new TextComponent("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|"));
        console.sendMessage(new TextComponent("§c                                  |___/                           §7v2.0"));

        createConfig();
        loadConfig();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            banmanager = new BanManagerMySQL(config, messages, mysql);
            try {
                mysql.connect();
                console.sendMessage(new TextComponent(PREFIX + "§7Datenbankverbindung §2erfolgreich §7hergestellt."));
            } catch (SQLException e) {
                console.sendMessage(new TextComponent(PREFIX + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden."));
                console.sendMessage(new TextComponent(PREFIX + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml."));
            }
            try {
                if(mysql.isConnected()) {
                    mysql.createTables(config);
                    console.sendMessage(new TextComponent(PREFIX + "§7Die MySQL Tabellen wurden §2erstellt§7."));
                }
            } catch (SQLException e) {
                console.sendMessage(new TextComponent(PREFIX + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden."));
                console.sendMessage(new TextComponent(PREFIX + e.getMessage() + " " + e.getCause()));
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    console.sendMessage(new TextComponent(PREFIX + "§7Die Ban IDs wurden §2synchronisiert§7."));
                }

            } catch (SQLException e) {
                console.sendMessage(new TextComponent(PREFIX + "§7Die IDs konnten nicht mit MySQL synchronisiert werden."));
                console.sendMessage(new TextComponent(PREFIX + e.getMessage() + " " + e.getCause()));
                e.printStackTrace();
            }

        } else {
            fileDatabaseFolder = new File(this.getDataFolder().getPath() + "/database");
            createFileDatabase();
            banmanager = new BanManagerFile(config, messages, bans, banhistories, unbans, fileDatabaseFolder);
            mysql = null;
        }

        ProxyServer.getInstance().getScheduler().schedule(this, () -> {
            UUIDFetcher.clearCache();
        }, 1, 1, TimeUnit.HOURS);

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.autoban.enable"))
            ProxyServer.getInstance().getConsole().sendMessage(new TextComponent(
                    BanSystemBungee.PREFIX + "§cBitte trage die IP des Servers in der config.yml ein."));


        console.sendMessage(new TextComponent(BanSystemBungee.PREFIX + "§7Das BanSystem wurde gestartet."));

        updatemanager = new UpdateManager(mysql);

        try {
            if (updatechecker.checkForUpdates()) {
                console.sendMessage(new TextComponent(PREFIX + "§cEin neues Update ist verfügbar."));
                console.sendMessage(new TextComponent(PREFIX + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben."));
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

        PluginManager pm = ProxyServer.getInstance().getPluginManager();

        pm.unregisterListeners(this);

        ProxyServer.getInstance().getConsole()
                .sendMessage(new TextComponent(BanSystemBungee.PREFIX + "§7Das BanSystem wurde gestoppt."));

    }


    // create Config files
    private void createConfig() {
        try {
            File configfile = new File(this.getDataFolder(), "config.yml");
            if (!this.getDataFolder().exists()) {
                this.getDataFolder().mkdir();
            }
            if (!configfile.exists()) {
                configfile.createNewFile();
                config = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(configfile));
                ConfigurationUtil.initConfig(config);
                config.save(configfile);
            } //else if(config.getSection("mysql").getKeys().contains("enable")) {

            //}
            File messagesfile = new File(this.getDataFolder(), "messages.yml");
            if (!messagesfile.exists()) {
                messagesfile.createNewFile();
                messages = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesfile));
                ConfigurationUtil.initMessages(messages);
                messages.save(messagesfile);
            }
            File blacklistfile = new File(this.getDataFolder(), "blacklist.yml");
            if (!blacklistfile.exists()) {
                blacklistfile.createNewFile();
                blacklist = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(blacklistfile));
                ConfigurationUtil.initBlacklist(blacklist);
                blacklist.save(blacklistfile);
            }
            config = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(configfile));
            messages = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesfile));
            blacklist = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(blacklistfile));
        } catch (IOException e) {
            console.sendMessage(new TextComponent(PREFIX + "Dateien konnten nicht erstellt werden."));
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
                bans = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansfile));
            }
            File banhistoriesfile = new File(fileDatabaseFolder.getPath(), "banhistories.yml");
            if (!banhistoriesfile.exists()) {
                banhistoriesfile.createNewFile();
                banhistories = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(banhistoriesfile));
            }
            File unbansfile = new File(fileDatabaseFolder.getPath(), "unbans.yml");
            if (!unbansfile.exists()) {
                unbansfile.createNewFile();
                unbans = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(unbansfile));
            }

            bans = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(bansfile));
            banhistories = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(banhistoriesfile));
            unbans = new BungeeConfig(ConfigurationProvider.getProvider(YamlConfiguration.class).load(unbansfile));
        } catch (IOException e) {
            console.sendMessage(new TextComponent(PREFIX + "Die Filedatenbank konnten nicht erstellt werden."));
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
        BungeeUser bu = new BungeeUser(ProxyServer.getInstance().getPlayer(name));
        if(bu != null)
            return bu;

        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof ProxiedPlayer) {
            ((ProxiedPlayer) u.getRawUser()).disconnect(msg);
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
        pluginManager.registerCommand(this, new CommandWrapper("ban", new CMDban(banmanager, config, messages, mysql), true));
        pluginManager.registerCommand(this, new CommandWrapper("check", new CMDcheck(banmanager, mysql, messages), true));
        pluginManager.registerCommand(this, new CommandWrapper("deletehistory", new CMDdeletehistory(banmanager, messages, mysql), true));
        pluginManager.registerCommand(this, new CommandWrapper("history", new CMDhistory(banmanager, messages, mysql), true));
        pluginManager.registerCommand(this, new CommandWrapper("kick", new CMDkick(messages, mysql), true));
        pluginManager.registerCommand(this, new CommandWrapper("unban", new CMDunban(banmanager, mysql, messages, config), true));
        pluginManager.registerCommand(this, new CommandWrapper("unmute", new CMDunmute(banmanager, messages, config, mysql), true));
        pluginManager.registerCommand(this, new CommandWrapper("bansystem", new CMDbansystem(messages, config, mysql), false));
        pluginManager.registerCommand(this, new CommandWrapper("bansys", new CMDbansystem(messages, config, mysql), false));

        pluginManager.registerListener(this, new LoginListener());
        pluginManager.registerListener(this, new ChatListener());
    }

    public MySQL getMySQL() {
        return mysql;
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return null;
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            users.add(new BungeeUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new BungeeUser(ProxyServer.getInstance().getConsole());
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }
}
