package net.coalcube.bansystem.velocity;


import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import net.coalcube.bansystem.spigot.util.SpigotConfig;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Plugin(id = "bansystem", name = "BanSystem", version = "2.7",
        url = "https://www.spigotmc.org/resources/bansystem-mit-ids-spigot-bungeecord.65863/",
        description = "Punishment System", authors = {"Tobi"})
public class BanSystemVelocity implements BanSystem {

    private final ProxyServer server;
    private final Logger logger;

    private static BanManager banManager;
    private static IDManager idManager;
    private static URLUtil urlUtil;

    private Database sql;
    private MySQL mysql;
    private TimeFormatUtil timeFormatUtil;
    private Config config, messages, blacklist;
    private static String Banscreen;
    private static List<String> blockedCommands, ads, blockedWords;
    private File sqlitedatabase;
    private String hostname, database, user, pw;
    private int port;
    public static String prefix = "§8§l┃ §cBanSystem §8» §7";


    @Override
    public void onEnable() {

        BanSystem.setInstance(this);
        PluginManager pluginManager = server.getPluginManager();
        UpdateChecker updatechecker = new UpdateChecker(65863);

        logger.info("§c  ____                    ____                  _                      ");
        logger.info("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  ");
        logger.info("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ ");
        logger.info("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |");
        logger.info("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|");
        logger.info("§c                                  |___/                           §7v" + this.getVersion());

        createConfig();
        loadConfig();

        timeFormatUtil = new TimeFormatUtil(messages);

        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            sql = mysql;
            banManager = new BanManagerMySQL(mysql);
            try {
                mysql.connect();
                logger.info(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                logger.error(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                logger.error(prefix + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
                logger.error(prefix + "§cDebug Message: §e" + e.getMessage());
            }
            try {
                if(mysql.isConnected()) {
                    mysql.createTables(config);
                    logger.info(prefix + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException | ExecutionException | InterruptedException e) {
                logger.error(prefix + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    logger.info(prefix + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                logger.error(prefix + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                e.printStackTrace();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        } else {
            createFileDatabase();
            SQLite sqlite = new SQLite(sqlitedatabase);
            banManager = new BanManagerSQLite(sqlite);
            sql = sqlite;
            try {
                sqlite.connect();
                logger.info(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                logger.error(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                logger.error(prefix + "§cBitte überprüfe die eingetragenen SQlite daten in der Config.yml.");
                e.printStackTrace();
            }
            try {
                if(sqlite.isConnected()) {
                    sqlite.createTables(config);
                    logger.info(prefix + "§7Die SQLite Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                logger.error(prefix + "§7Die SQLite Tabellen §ckonnten nicht §7erstellt werden.");
                logger.error(prefix + e.getMessage() + " " + e.getCause());
            }
        }

        server.getScheduler().buildTask(this, () -> {
            UUIDFetcher.clearCache();
        }).repeat(1L, TimeUnit.HOURS).schedule();

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.enable"))
            logger.warn(prefix + "§cBitte trage die IP des Servers in der config.yml ein.");

        logger.info(prefix + "§7Das BanSystem wurde gestartet.");

        try {
            if (updatechecker.checkForUpdates()) {
                logger.info(prefix + "§cEin neues Update ist verfügbar.");
                logger.info(prefix + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        idManager = new IDManager(config, sql, new File("plugins/BanSystem/", "config.yml"));
        urlUtil = new URLUtil(messages, config);

        init(pluginManager);
    }

    @Override
    public void onDisable() {
        try {
            if (sql.isConnected())
                sql.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }

//        AsyncPlayerChatEvent.getHandlerList().unregister(instance);
//        PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
//        PlayerQuitEvent.getHandlerList().unregister(instance);
//        PlayerJoinEvent.getHandlerList().unregister(instance);
//        PlayerPreLoginEvent.getHandlerList().unregister(instance);

        logger.info(BanSystemSpigot.prefix + "§7Das BanSystem wurde gestoppt.");
    }

    @Inject
    public BanSystemVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;

        onEnable();

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        onDisable();
    }

    public void init(PluginManager pluginManager) {

    }

    private void createFileDatabase() {
        try {
            sqlitedatabase = new File("plugins/BanSystem/", "database.db");

            if (!sqlitedatabase.exists()) {
                sqlitedatabase.createNewFile();
            }
        } catch (IOException e) {
            logger.error(prefix + "Die SQLite datenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    private void createConfig() {
        try {
            File configfile = new File("plugins/BanSystem/", "config.yml");
            if (!new File("plugins/BanSystem/").exists()) {
                new File("plugins/BanSystem/").mkdir();
            }
            if (!configfile.exists()) {
                configfile.createNewFile();
                config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
                ConfigurationUtil.initConfig(config);
                config.save(configfile);
            }
            File messagesfile = new File("plugins/BanSystem/", "messages.yml");
            if (!messagesfile.exists()) {
                messagesfile.createNewFile();
                messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
                ConfigurationUtil.initMessages(messages);
                messages.save(messagesfile);
            }
            File blacklistfile = new File("plugins/BanSystem/", "blacklist.yml");
            if (!blacklistfile.exists()) {
                blacklistfile.createNewFile();
                blacklist = new SpigotConfig(YamlConfiguration.loadConfiguration(blacklistfile));
                ConfigurationUtil.initBlacklist(blacklist);
                blacklist.save(blacklistfile);
            }
            messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
            config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
            blacklist = new SpigotConfig(YamlConfiguration.loadConfiguration(blacklistfile));
        } catch (IOException e) {
            logger.error("[Bansystem] Dateien konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllPlayers() {
        return null;
    }

    @Override
    public User getConsole() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }



    @Override
    public User getUser(String name) {
        return null;
    }

    @Override
    public User getUser(UUID uniqueId) {
        return null;
    }

    @Override
    public void disconnect(User u, String msg) {

    }

    @Override
    public Config getMessages() {
        return null;
    }

    @Override
    public Config getConfiguration() {
        return null;
    }

    @Override
    public void loadConfig() {

    }

    @Override
    public Database getSQL() {
        return null;
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return null;
    }

    @Override
    public String getBanScreen() {
        return null;
    }
}
