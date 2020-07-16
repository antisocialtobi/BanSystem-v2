package net.coalcube.bansystem.core;

import java.util.List;

import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.MySQL;
import net.coalcube.bansystem.core.util.TimeFormatUtil;
import net.coalcube.bansystem.core.util.User;

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

    User getUser(String name);

    void disconnect(User u, String msg);

    Config getMessages();

    Config getConfiguration();

    void loadConfig();

    MySQL getMySQL();

    TimeFormatUtil getTimeFormatUtil();

    String getBanScreen();

}