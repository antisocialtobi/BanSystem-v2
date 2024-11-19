package net.coalcube.bansystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.*;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.sql.MySQL;
import net.coalcube.bansystem.core.sql.SQLite;
import net.coalcube.bansystem.core.textcomponent.TextComponent;
import net.coalcube.bansystem.core.textcomponent.TextComponentKyori;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import net.coalcube.bansystem.velocity.listener.VelocityLoginEvent;
import net.coalcube.bansystem.velocity.listener.VelocityChatEvent;
import net.coalcube.bansystem.velocity.util.VelocityMetrics;
import net.coalcube.bansystem.velocity.util.VelocityUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.charts.AdvancedBarChart;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Plugin(id = "bansystem", name = "BanSystem", version = "3.1",
        url = "https://www.spigotmc.org/resources/bansystem-mit-ids-spigot-bungeecord.65863/",
        description = "Punishment System", authors = {"Tobi"})
public class BanSystemVelocity implements BanSystem {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private LegacyComponentSerializer lcs;
    private static BanManager banManager;
    private static IDManager idManager;
    private static URLUtil urlUtil;
    private static ConfigurationUtil configurationUtil;
    private BlacklistUtil blacklistUtil;
    private Database sql;
    private MySQL mysql;
    private TimeFormatUtil timeFormatUtil;
    private YamlDocument config, messages, blacklist;
    private TextComponent textComponent;
    private static String Banscreen;
    private static List<String> blockedCommands, ads, blockedWords;
    private File sqlitedatabase, configFile, messagesFile, blacklistFile;
    private String hostname, database, user, pw;
    private int port;
    private static List<String> cachedBannedPlayerNames;
    private static List<String> cachedMutedPlayerNames;
    private MetricsAdapter metricsAdapter;

    public static String prefix = "§8§l┃ §cBanSystem §8» §7";
    private boolean isUpdateAvailable;

    @Inject
    public BanSystemVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    public void onEnable() {
        BanSystem.setInstance(this);

        int pluginId = 23650; // bstats Plugin ID
        Metrics metrics = metricsFactory.make(this, pluginId);

        metricsAdapter = new VelocityMetrics(metrics);
        PluginManager pluginmanager = server.getPluginManager();
        UpdateChecker updatechecker = new UpdateChecker(65863);
        lcs = LegacyComponentSerializer.legacySection();
        configurationUtil = new ConfigurationUtil(config, messages, blacklist, this);
        timeFormatUtil = new TimeFormatUtil(configurationUtil);
        cachedBannedPlayerNames = new ArrayList<>();
        cachedMutedPlayerNames = new ArrayList<>();

        sendConsoleMessage("§c  ____                    ____                  _                      ");
        sendConsoleMessage("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  ");
        sendConsoleMessage("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ ");
        sendConsoleMessage("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |");
        sendConsoleMessage("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|");
        sendConsoleMessage("§c                                  |___/                           §7v" + this.getVersion());

        try {
            configurationUtil.createConfigs(dataDirectory.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        config = configurationUtil.getConfig();
        messages = configurationUtil.getMessagesConfig();
        blacklist = configurationUtil.getBlacklist();

        loadConfig();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            sql = mysql;
            banManager = new BanManagerMySQL(mysql, config);
            try {
                mysql.connect();
                sendConsoleMessage(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                sendConsoleMessage(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                sendConsoleMessage(prefix + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
                sendConsoleMessage(prefix + "§cDebug Message: §e" + e.getMessage());
            }
            try {
                if(mysql.isConnected()) {
                    if(mysql.isOldDatabase()) {
                        sendConsoleMessage(prefix + "§7Die MySQL Daten vom dem alten BanSystem wurden §2importiert§7.");
                    }
                    mysql.createTables(config);
                    sendConsoleMessage(prefix + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException | ExecutionException | InterruptedException e) {
                sendConsoleMessage(prefix + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    sendConsoleMessage(prefix + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                sendConsoleMessage(prefix + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                e.printStackTrace();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        } else {
            createFileDatabase();
            SQLite sqlite = new SQLite(sqlitedatabase);
            banManager = new BanManagerSQLite(sqlite, config);
            sql = sqlite;
            try {
                sqlite.connect();
                sendConsoleMessage(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                sendConsoleMessage(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                sendConsoleMessage(prefix + "§cBitte überprüfe die eingetragenen SQlite daten in der Config.yml.");
                e.printStackTrace();
            }
            try {
                if(sqlite.isConnected()) {
                    sqlite.createTables(config);
                    sendConsoleMessage(prefix + "§7Die SQLite Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                sendConsoleMessage(prefix + "§7Die SQLite Tabellen §ckonnten nicht §7erstellt werden.");
                sendConsoleMessage(prefix + e.getMessage() + " " + e.getCause());
            }
        }


        server.getScheduler()
                .buildTask(this, () -> {
                    UUIDFetcher.clearCache();
                    try {
                        isUpdateAvailable = updatechecker.checkForUpdates();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .delay(1, TimeUnit.HOURS)
                .schedule();

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.enable"))
            sendConsoleMessage(
                    prefix + "§cBitte trage die IP des Servers in der config.yml ein.");

        if(sql.isConnected()) {
            try {
                sql.updateTables();
            } catch (SQLException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            initCachedBannedPlayerNames();
        } catch (SQLException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        sendConsoleMessage(prefix + "§7Das BanSystem wurde gestartet.");

        try {
            isUpdateAvailable = updatechecker.checkForUpdates();
            if (config.getBoolean("updateCheck")) {
                if (isUpdateAvailable) {
                    sendConsoleMessage(prefix + "§cEin neues Update ist verfügbar.");
                    sendConsoleMessage(prefix + "§7Lade es dir unter " +
                            "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        idManager = new IDManager(config, sql, new File(dataDirectory.toFile(), "config.yml"));
        urlUtil = new URLUtil(configurationUtil, config);
        blacklistUtil = new BlacklistUtil(blacklist);
        textComponent = new TextComponentKyori(configurationUtil);
    }

    public void onDisable() {
        try {
            if (sql.isConnected())
                sql.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.info(BanSystemVelocity.prefix + "§7Das BanSystem wurde gestoppt.");

    }

    @Override
    public String getEnvironment() {
        return server.getVersion().getName();
    }


    // create Config files
    /*private void createConfig() {
        try {
            if(!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdir();
            }

            configFile = new File(dataDirectory.toFile(), "config.yml");
            if (!configFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.yml");
                Files.copy(in, configFile.toPath());
                config = new VelocityConfig(configFile);
            }

            messagesFile = new File(dataDirectory.toFile(), "messages.yml");
            if (!messagesFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("messages.yml");
                Files.copy(in, messagesFile.toPath());
                messages = new VelocityConfig(messagesFile);
            }

            blacklistFile = new File(dataDirectory.toFile(), "blacklist.yml");
            if (!blacklistFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("blacklist.yml");
                Files.copy(in, blacklistFile.toPath());
                blacklist = new VelocityConfig(blacklistFile);
            }
            messages = new VelocityConfig(messagesFile);
            config = new VelocityConfig(configFile);
            blacklist = new VelocityConfig(blacklistFile);

        } catch (IOException e) {
            System.err.println("[Bansystem] Dateien konnten nicht erstellt werden.");
        }
    }*/

    private void createFileDatabase() {
        try {
            sqlitedatabase = new File(dataDirectory.toFile(), "database.db");

            if (!sqlitedatabase.exists()) {
                sqlitedatabase.createNewFile();
            }
        } catch (IOException e) {
            logger.error(prefix + "Die SQLite datenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            prefix = messages.getString("prefix").replaceAll("&", "§");

            Banscreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (Banscreen == null) {
                    Banscreen = screen.replaceAll("%P%", prefix) + "\n";
                } else
                    Banscreen = Banscreen + screen.replaceAll("%P%", prefix) + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");

            ads = new ArrayList<>();
            blockedCommands = new ArrayList<>();
            blockedWords = new ArrayList<>();

            ads.addAll(blacklist.getStringList("Ads"));

            blockedCommands.addAll(config.getStringList("mute.blockedCommands"));

            blockedWords.addAll(blacklist.getStringList("Words"));

        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten.");
            e.printStackTrace();
        }
    }

    @Override
    public Database getSQL() {
        return sql;
    }

    @Override
    public User getUser(String name) {
        if(server.getPlayer(name).isPresent())
            return new VelocityUser(server.getPlayer(name).get());
        return new VelocityUser(null);
    }

    @Override
    public User getUser(UUID uniqueId) {
        if(server.getPlayer(uniqueId).isPresent())
            return new VelocityUser(server.getPlayer(uniqueId).get());
        return new VelocityUser(null);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        onEnable();
        CommandManager commandManager = server.getCommandManager();

        CommandMeta commandBanMeta = commandManager.metaBuilder("ban")
                .plugin(this)
                .build();
        CommandMeta commandBanSystemMeta = commandManager.metaBuilder("bansystem")
                .aliases("bansys")
                .plugin(this)
                .build();
        CommandMeta commandCheckMeta = commandManager.metaBuilder("check")
                .plugin(this)
                .build();
        CommandMeta commandDeleteHistoryMeta = commandManager.metaBuilder("deletehistory")
                .aliases("delhistory")
                .plugin(this)
                .build();
        CommandMeta commandHistoryMeta = commandManager.metaBuilder("history")
                .plugin(this)
                .build();
        CommandMeta commandKickMeta = commandManager.metaBuilder("kick")
                .plugin(this)
                .build();
        CommandMeta commandUnBanMeta = commandManager.metaBuilder("unban")
                .plugin(this)
                .build();
        CommandMeta commandUnMuteMeta = commandManager.metaBuilder("unmute")
                .plugin(this)
                .build();

        SimpleCommand commandBan = new CommandWrapper(
                new CMDban(banManager, config, messages, sql, configurationUtil));
        SimpleCommand commandBanSystem = new CommandWrapper(
                new CMDbansystem(config, sql, mysql, idManager, timeFormatUtil, banManager, configurationUtil, textComponent));
        SimpleCommand commandCheck = new CommandWrapper(
                new CMDcheck(banManager, sql, configurationUtil));
        SimpleCommand commandDeleteHistory = new CommandWrapper(
                new CMDdeletehistory(banManager, sql, configurationUtil));
        SimpleCommand commandHistory = new CommandWrapper(
                new CMDhistory(banManager, config, sql, configurationUtil));
        SimpleCommand commandKick = new CommandWrapper(
                new CMDkick(sql, banManager, configurationUtil));
        SimpleCommand commandUnBan = new CommandWrapper(
                new CMDunban(banManager, sql, config, configurationUtil));
        SimpleCommand commandUnMute = new CommandWrapper(
                new CMDunmute(banManager, config, sql, configurationUtil));

        commandManager.register(commandBanMeta, commandBan);
        commandManager.register(commandBanSystemMeta, commandBanSystem);
        commandManager.register(commandCheckMeta, commandCheck);
        commandManager.register(commandDeleteHistoryMeta, commandDeleteHistory);
        commandManager.register(commandHistoryMeta, commandHistory);
        commandManager.register(commandKickMeta, commandKick);
        commandManager.register(commandUnBanMeta, commandUnBan);
        commandManager.register(commandUnMuteMeta, commandUnMute);

        server.getEventManager().register(this,
                new VelocityChatEvent(this, server, banManager, config, sql, blacklistUtil, configurationUtil, idManager));
        server.getEventManager().register(this,
                new VelocityLoginEvent(this, banManager, config, sql, urlUtil, configurationUtil, idManager));
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public String getBanScreen() {
        return Banscreen;
    }

    @Override
    public void sendConsoleMessage(String msg) {
        for (String line : msg.split("\n")) {
            server.getConsoleCommandSource().sendMessage(lcs.deserialize(line));
        }
    }

    @Override
    public InputStream getResourceAsInputStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public List<String> getCachedBannedPlayerNames() {
        return cachedBannedPlayerNames;
    }

    @Override
    public List<String> getCachedMutedPlayerNames() {
        return cachedMutedPlayerNames;
    }

    @Override
    public void addCachedMutedPlayerNames(String name) {
        cachedMutedPlayerNames.add(name);
    }

    @Override
    public void addCachedBannedPlayerNames(String name) {
        cachedBannedPlayerNames.add(name);
    }

    @Override
    public void removeCachedBannedPlayerNames(String name) {
        cachedBannedPlayerNames.remove(name);
    }

    @Override
    public void removeCachedMutedPlayerNames(String name) {
        cachedMutedPlayerNames.remove(name);
    }

    @Override
    public MetricsAdapter getMetricsAdapter() {
        return metricsAdapter;
    }

    @Override
    public boolean isUpdateAvailable() {
        return isUpdateAvailable;
    }

    private void initCachedBannedPlayerNames() throws SQLException, ExecutionException, InterruptedException {
        new Thread(() -> {
            try {
                for(Ban ban : banManager.getAllBans()) {
                    String name = UUIDFetcher.getName(ban.getPlayer());
                    if(ban.getType() == Type.NETWORK) {
                        cachedBannedPlayerNames.add(name);
                    } else {
                        cachedMutedPlayerNames.add(name);
                    }
                }
            } catch (SQLException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (Player p : server.getAllPlayers()) {
            users.add(new VelocityUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new VelocityUser(server.getConsoleCommandSource());
    }

    @Override
    public String getVersion() {
        PluginContainer pluginContainer = server.getPluginManager().getPlugin("bansystem").orElse(null);
        if (pluginContainer != null) {
            return pluginContainer.getDescription().getVersion().orElse("Version not available");
        } else {
            return "Plugin not found";
        }
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
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof Player) {
            ((Player) u.getRawUser()).disconnect(Component.text(msg));
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }
}
