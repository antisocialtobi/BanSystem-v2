package net.coalcube.bansystem.core;

import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.MetricsAdapter;
import net.coalcube.bansystem.core.util.TimeFormatUtil;
import net.coalcube.bansystem.core.util.User;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface BanSystem {

    BanSystem[] BANSYSTEM = new BanSystem[1];
    static BanSystem getInstance() {
        return BANSYSTEM[0];
    }
    static void setInstance(BanSystem bs) {
        BANSYSTEM[0] = bs;
    }
    List<User> getAllPlayers();
    User getConsole();
    String getVersion();
    void onEnable();
    void onDisable();
    String getEnvironment();
    User getUser(String name);
    User getUser(UUID uniqueId);
    void disconnect(User u, String msg);
    void loadConfig();
    Database getSQL();
    TimeFormatUtil getTimeFormatUtil();
    String getBanScreen();
    ConfigurationUtil getConfigurationUtil();
    BanManager getBanManager();
    void sendConsoleMessage(String msg);
    InputStream getResourceAsInputStream(String path);
    List<String> getCachedBannedPlayerNames();
    List<String> getCachedMutedPlayerNames();
    void addCachedBannedPlayerNames(String name);
    void addCachedMutedPlayerNames(String name);
    void removeCachedBannedPlayerNames(String name);
    void removeCachedMutedPlayerNames(String name);
    MetricsAdapter getMetricsAdapter();
    boolean isUpdateAvailable();
}