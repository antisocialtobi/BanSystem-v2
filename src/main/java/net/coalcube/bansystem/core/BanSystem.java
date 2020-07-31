package net.coalcube.bansystem.core;

import java.util.List;

import net.coalcube.bansystem.core.util.*;

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

    Database getSQL();

    TimeFormatUtil getTimeFormatUtil();

    String getBanScreen();

}