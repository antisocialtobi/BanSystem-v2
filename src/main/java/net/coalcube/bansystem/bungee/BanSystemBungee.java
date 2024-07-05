package net.coalcube.bansystem.bungee;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.bungee.listener.ChatListener;
import net.coalcube.bansystem.bungee.listener.LoginListener;
import net.coalcube.bansystem.bungee.util.BungeeUser;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.BanManagerMySQL;
import net.coalcube.bansystem.core.ban.BanManagerSQLite;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.sql.MySQL;
import net.coalcube.bansystem.core.sql.SQLite;
import net.coalcube.bansystem.core.textcomponent.TextComponentmd5;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import net.coalcube.bansystem.bungee.listener.PluginMessageListener;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BanSystemBungee extends Plugin implements BanSystem {

    private static Plugin instance;
    private BanManager banManager;
    private IDManager idManager;
    private URLUtil urlUtil;
    private ConfigurationUtil configurationUtil;
    private BlacklistUtil blacklistUtil;
    private Database sql;
    private MySQL mysql;
    private TimeFormatUtil timeFormatUtil;
    private YamlDocument config, messages, blacklist;
    private net.coalcube.bansystem.core.textcomponent.TextComponent textComponent;
    private String banScreen;
    private List<String> blockedCommands, ads, blockedWords, whitelist;
    private File sqlitedatabase;
    private String hostname, database, user, pw;
    private int port;
    private CommandSender console;
    public static String prefix = "§8§l┃ §cBanSystem §8» §7";

    @Override
    public void onEnable() {
        super.onEnable();

        instance = this;
        BanSystem.setInstance(this);

        ProxyServer proxy = ProxyServer.getInstance();
        PluginManager pluginmanager = ProxyServer.getInstance().getPluginManager();
        UpdateChecker updatechecker = new UpdateChecker(65863);
        console = ProxyServer.getInstance().getConsole();
        configurationUtil = new ConfigurationUtil(config, messages, blacklist, this);

        console.sendMessage(new TextComponent("§c  ____                    ____                  _                      "));
        console.sendMessage(new TextComponent("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  "));
        console.sendMessage(new TextComponent("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ "));
        console.sendMessage(new TextComponent("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |"));
        console.sendMessage(new TextComponent("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|"));
        console.sendMessage(new TextComponent("§c                                  |___/                           §7v" + this.getVersion()));

        try {
            configurationUtil.createConfigs(getDataFolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        config = configurationUtil.getConfig();
        messages = configurationUtil.getMessagesConfig();
        blacklist = configurationUtil.getBlacklist();

        timeFormatUtil = new TimeFormatUtil(configurationUtil);

        loadConfig();


        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            sql = mysql;
            banManager = new BanManagerMySQL(mysql, config);
            try {
                mysql.connect();
                console.sendMessage(new TextComponent(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt."));
            } catch (SQLException e) {
                console.sendMessage(new TextComponent(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden."));
                console.sendMessage(new TextComponent(prefix + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml."));
                console.sendMessage(new TextComponent(prefix + "§cDebug Message: §e" + e.getMessage()));
            }
            try {
                if(mysql.isConnected()) {
                    mysql.createTables(config);
                    if(mysql.isOldDatabase()) {
                        console.sendMessage(new TextComponent(prefix + "§7Die MySQL Daten vom dem alten BanSystem wurden §2importiert§7."));
                    }
                    console.sendMessage(new TextComponent(prefix + "§7Die MySQL Tabellen wurden §2erstellt§7."));
                }
            } catch (SQLException | ExecutionException | InterruptedException e) {
                console.sendMessage(new TextComponent(prefix + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden."));
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    console.sendMessage(new TextComponent(prefix + "§7Die Ban IDs wurden §2synchronisiert§7."));
                }

            } catch (SQLException | ExecutionException | InterruptedException e) {
                console.sendMessage(new TextComponent(prefix + "§7Die IDs konnten nicht mit MySQL synchronisiert werden."));
                e.printStackTrace();
            }

        } else {
            createFileDatabase();
            SQLite sqlite = new SQLite(sqlitedatabase);
            banManager = new BanManagerSQLite(sqlite, config);
            sql = sqlite;
            try {
                sqlite.connect();
                console.sendMessage(new TextComponent(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt."));
            } catch (SQLException e) {
                console.sendMessage(new TextComponent(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden."));
                console.sendMessage(new TextComponent(prefix + "§cBitte überprüfe die eingetragenen SQlite daten in der Config.yml."));
                e.printStackTrace();
            }
            try {
                if (sqlite.isConnected()) {
                    sqlite.createTables(config);
                    console.sendMessage(new TextComponent(prefix + "§7Die SQLite Tabellen wurden §2erstellt§7."));
                }
            } catch (SQLException e) {
                console.sendMessage(new TextComponent(prefix + "§7Die SQLite Tabellen §ckonnten nicht §7erstellt werden."));
                console.sendMessage(new TextComponent(prefix + e.getMessage() + " " + e.getCause()));
                e.printStackTrace();
            }
        }

        // Clear UUID Fetcher Cache
        ProxyServer.getInstance().getScheduler().schedule(this, UUIDFetcher::clearCache, 1, 1, TimeUnit.HOURS);

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.enable"))
            console.sendMessage(new TextComponent(
                    BanSystemBungee.prefix + "§cBitte trage die IP des Servers in der config.yml ein."));

        idManager = new IDManager(config, sql, new File(this.getDataFolder(), "config.yml"));
        urlUtil = new URLUtil(configurationUtil, config);
        blacklistUtil = new BlacklistUtil(blacklist);
        textComponent = new TextComponentmd5(configurationUtil);

        // Register channel to bypass chat message signing
        this.getProxy().registerChannel("bansys:chatsign");

        init(pluginmanager);



        if(sql.isConnected()) {
            try {
                sql.updateTables();
            } catch (SQLException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


        console.sendMessage(new TextComponent(BanSystemBungee.prefix + "§7Das BanSystem wurde gestartet."));

        try {
            if (updatechecker.checkForUpdates()) {
                console.sendMessage(new TextComponent(prefix + "§cEin neues Update ist verfügbar."));
                console.sendMessage(new TextComponent(prefix + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            StatisticServerSocket statisticServerSocket = new StatisticServerSocket(banManager);

            String enviroment = "Bungeecord";
            String serverVersion = proxy.getVersion();

            //statisticServerSocket.register(enviroment, serverVersion, this.getVersion());
        }).start();

        /*
        WebHook webhook = new WebHook("https://discord.com/api/webhooks/1243098087862304788/4zAzjFPGPoHSIxoPbhcAFZIc-0oLHtwplZFD3klX4NSxdIL06HLBxg8r1Fo31XeO4NvC");
        webhook.setAvatarUrl("https://www.spigotmc.org/data/resource_icons/65/65863.jpg?1561923292");
        webhook.setUsername("Bansystem");
        webhook.setTts(false);
        webhook.addEmbed(new WebHook.EmbedObject()
                .setTitle("Neuer Ban")
                .setDescription("Ein Spieler wurde aus dem Spiel ausgeschlossen.")
                .setColor(Color.RED)
                .addField("Spieler", "Test", true)
                .addField("Ersteller", "TO81", true)
                .addField("Dauer", "30 Tage", true)
                .addField("Grund", "Unerlaubte Clientmodifikation/Hackclient", true)
                .setFooter("Bansystem by Tobias Herzig", "https://mineskin.eu/helm/617f0c2b-6014-47f2-bf89-fade1bc9bb59")
                .setThumbnail("https://mineskin.eu/helm/d8d5a923-7b20-43d8-883b-1150148d6955"));
        try {
            webhook.execute(); //Handle exception
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        */
    }

    @Override
    public void onDisable() {
        super.onDisable();

        try {
            if (sql.isConnected())
                sql.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PluginManager pm = ProxyServer.getInstance().getPluginManager();

        pm.unregisterListeners(this);

        ProxyServer.getInstance().getConsole()
                .sendMessage(new TextComponent(prefix + "§7Das BanSystem wurde gestoppt."));

    }

    private void createFileDatabase() {
        try {
            sqlitedatabase = new File(this.getDataFolder(), "database.db");

            if (!sqlitedatabase.exists()) {
                sqlitedatabase.createNewFile();
            }
        } catch (IOException e) {
            console.sendMessage(new TextComponent(prefix + "Die SQLite datenbank konnten nicht erstellt werden."));
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            prefix = messages.getString("prefix").replaceAll("&", "§");
            banScreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (banScreen == null) {
                    banScreen = screen.replaceAll("%P%", prefix).replaceAll("&", "§") + "\n";
                } else
                    banScreen += screen.replaceAll("%P%", prefix).replaceAll("&", "§") + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");

            ads = new ArrayList<>();
            blockedCommands = new ArrayList<>();
            blockedWords = new ArrayList<>();
            whitelist = new ArrayList<>();

            ads.addAll(blacklist.getStringList("Ads"));
            blockedCommands.addAll(config.getStringList("mute.blockedCommands"));
            blockedWords.addAll(blacklist.getStringList("Words"));
            whitelist.addAll(blacklist.getStringList("Whitelist"));

        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten.");
            e.printStackTrace();
        }
    }

    @Override
    public User getUser(String name) {
        return new BungeeUser(ProxyServer.getInstance().getPlayer(name));
    }

    @Override
    public User getUser(UUID uniqueId) {
        return new BungeeUser(ProxyServer.getInstance().getPlayer(uniqueId));
    }

    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof ProxiedPlayer) {
            ((ProxiedPlayer) u.getRawUser()).disconnect(msg);
        }
    }

    private void init(PluginManager pluginManager) {
        pluginManager.registerCommand(this, new CommandWrapper("ban",
                new CMDban(banManager, config, messages, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("check",
                new CMDcheck(banManager, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("deletehistory",
                new CMDdeletehistory(banManager, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("delhistory",
                new CMDdeletehistory(banManager, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("history",
                new CMDhistory(banManager, config, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("kick",
                new CMDkick(sql, banManager, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("unban",
                new CMDunban(banManager, sql, config, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("unmute",
                new CMDunmute(banManager, config, sql, configurationUtil), true));
        pluginManager.registerCommand(this, new CommandWrapper("bansystem",
                new CMDbansystem(config, sql, mysql, idManager, timeFormatUtil, banManager, configurationUtil, textComponent), false));
        pluginManager.registerCommand(this, new CommandWrapper("bansys",
                new CMDbansystem(config, sql, mysql, idManager, timeFormatUtil, banManager, configurationUtil, textComponent), false));

        pluginManager.registerListener(this, new LoginListener(banManager, config, sql, urlUtil, configurationUtil));
        pluginManager.registerListener(this, new ChatListener(banManager, config, sql, blacklistUtil, configurationUtil));
        pluginManager.registerListener(this, new PluginMessageListener());
    }

    public Database getSQL() {
        return sql;
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public String getBanScreen() {
        return banScreen;
    }

    @Override
    public ConfigurationUtil getConfigurationUtil() {
        return configurationUtil;
    }

    @Override
    public BanManager getBanManager() {
        return banManager;
    }

    @Override
    public void sendConsoleMessage(String msg) {
        for (String line : msg.split("\n")) {
            console.sendMessage(new TextComponent(line));
        }
    }

    @Override
    public InputStream getResourceAsInputStream(String path) {
        return this.getResourceAsStream(path);
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

    public static Plugin getInstance() {
        return instance;
    }
}
