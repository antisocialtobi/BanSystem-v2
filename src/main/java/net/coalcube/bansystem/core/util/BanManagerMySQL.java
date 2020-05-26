package net.coalcube.bansystem.core.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

public class BanManagerMySQL implements BanManager {

    private final Config messages, config;
    private final MySQL mysql;

    public BanManagerMySQL(Config config, Config messages, MySQL mysql) {
        this.messages = messages;
        this.config = config;
        this.mysql = mysql;
    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException {

    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException {

    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException {

    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException {

    }

    @Override
    public void unBan(UUID player, UUID unBanner, String reason) throws IOException {

    }

    @Override
    public void unBan(UUID player, String unBanner, String reason) throws IOException {

    }

    @Override
    public void unBan(UUID player, UUID unBanner) throws IOException {

    }

    @Override
    public void unBan(UUID player, String unBanner) throws IOException {

    }

    @Override
    public void unMute(UUID player, UUID unBanner, String reason) throws IOException {

    }

    @Override
    public void unMute(UUID player, String unBanner, String reason) throws IOException {

    }

    @Override
    public void unMute(UUID player, UUID unBanner) throws IOException {

    }

    @Override
    public void unMute(UUID player, String unBanner) throws IOException {

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
        return null;
    }

    @Override
    public String getReason(UUID player, Type type) {
        return null;
    }

    @Override
    public int getLevel(UUID player, String reason) throws UnknownHostException {
        return 0;
    }

    @Override
    public List<History> getHistory(UUID player) throws UnknownHostException {
        return null;
    }

    @Override
    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) {
        return null;
    }

    @Override
    public boolean hasHistory(UUID player) throws UnknownHostException {
        return false;
    }

    @Override
    public boolean hasHistory(UUID player, String reason) throws UnknownHostException {
        return false;
    }

    @Override
    public boolean isBanned(UUID player, Type type) {
        return false;
    }
}
