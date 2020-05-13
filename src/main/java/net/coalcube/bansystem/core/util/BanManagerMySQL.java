package net.coalcube.bansystem.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;

public class BanManagerMySQL implements BanManager {

    private Config messages;
    private Config config;
    private MySQL mysql;

    public BanManagerMySQL(Config config, Config messages, MySQL mysql) {
        this.messages = messages;
        this.config = config;
        this.mysql = mysql;
    }


    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) {

    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason) {

    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) {

    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason) {

    }

    @Override
    public void unban(UUID player, UUID unbanner, String reason) {

    }

    @Override
    public void unban(UUID player, String unbanner, String reason) {

    }

    @Override
    public void unban(UUID player, UUID unbanner) {

    }

    @Override
    public void unban(UUID player, String unbanner) {

    }

    @Override
    public void unmute(UUID player, UUID unbanner, String reason) {

    }

    @Override
    public void unmute(UUID player, String unbanner, String reason) {

    }

    @Override
    public void unmute(UUID player, UUID unbanner) {

    }

    @Override
    public void unmute(UUID player, String unbanner) {

    }

    @Override
    public void clearHistory(UUID player) {

    }

    @Override
    public String getBanReason(UUID player, Type type) {
        return null;
    }

    @Override
    public Long getEnd(UUID player, Type type) {
        return null;
    }

    @Override
    public String getBanner(UUID player, Type type) {
        return null;
    }

    @Override
    public Long getRemainingTime(UUID player, Type type) {
        return (long) 86400000;
    }

    @Override
    public String getReason(UUID player, Type type) {
        return null;
    }

    @Override
    public int getLevel(UUID player, String reason) {
        return 0;
    }

    @Override
    public ArrayList<History> getHistory(UUID player) {
        return null;
    }


    @Override
    public boolean hashistory(UUID player) {
        return false;
    }

    @Override
    public boolean hashistory(UUID player, String reason) {
        return false;
    }

    @Override
    public boolean isBanned(UUID player, Type type) {
        return false;
    }
}
