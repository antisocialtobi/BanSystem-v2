package net.coalcube.bansystem.core.util;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManagerFile implements BanManager {

    private final Config bans, banHistories, unBans;
    private final File bansFile, unBansFile, banHistoriesFile;

    public BanManagerFile(Config bans, Config banHistories, Config unBans, File database) {
        this.bans = bans;
        this.banHistories = banHistories;
        this.unBans = unBans;
        this.bansFile = new File(database.getPath(), "bans.yml");
        this.banHistoriesFile = new File(database.getPath(), "banhistories.yml");
        this.unBansFile = new File(database.getPath(), "unbans.yml");
    }

    @Override
    public void log(String action, String creator, String target, String note) {

    }

    @Override
    public void kick(UUID player, String creator) {

    }

    @Override
    public void kick(UUID player, UUID creator) {

    }

    @Override
    public void kick(UUID player, String creator, String reason) {

    }

    @Override
    public void kick(UUID player, UUID creator, String reason) {

    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress adress) throws IOException {
        ban(player, time, creator.toString(), type, reason, adress);
    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException {
        ban(player, time, creator.toString(), type, reason, null);
    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress adress) throws IOException {
        Long currenttime = System.currentTimeMillis();

        bans.set(player + "." + type.toString() + ".duration", time);
        bans.set(player + "." + type.toString() + ".creator", creator);
        bans.set(player + "." + type.toString() + ".reason", reason);
        bans.set(player + "." + type.toString() + ".creationdate", currenttime);
        bans.set(player + "." + type.toString() + ".end", currenttime + time);
        if(adress != null)
            bans.set(player + "." + type.toString() + ".ip", adress.getAddress());

        banHistories.set(player + "." + currenttime + ".duration", time);
        banHistories.set(player + "." + currenttime + ".creator", creator);
        banHistories.set(player + "." + currenttime + ".reason", reason);
        banHistories.set(player + "." + currenttime + ".type", type.toString());
        banHistories.set(player + "." + currenttime + ".end", currenttime + time);
        if(adress != null)
            banHistories.set(player + "." + type.toString() + ".ip", adress.getAddress());

        bans.save(bansFile);
        banHistories.save(banHistoriesFile);
    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException {
        ban(player, time, creator, type, reason, null);
    }

    @Override
    public void unBan(UUID player, UUID unBanner, String reason) throws IOException {
        unBan(player, unBanner.toString(), reason);
    }

    @Override
    public void unBan(UUID player, String unBanner, String reason) throws IOException {
        unBans.set(player + "." + Type.NETWORK.toString() + ".unbanner", unBanner);
        unBans.set(player + "." + Type.NETWORK.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            unBans.set(player + Type.CHAT.toString() + ".reason", reason);

        if(isBanned(player, Type.CHAT))
            bans.set(player.toString() + "." + Type.NETWORK.toString(), null);
        else
            bans.set(player.toString(), null);


        bans.save(bansFile);
        unBans.save(unBansFile);
    }

    @Override
    public void unBan(UUID player, UUID unBanner) throws IOException {
        unBan(player, unBanner.toString(), null);
    }

    @Override
    public void unBan(UUID player, String unBanner) throws IOException {
        unBan(player, unBanner, null);
    }

    @Override
    public void unMute(UUID player, UUID unBanner, String reason) throws IOException {
        unMute(player, unBanner.toString(), reason);
    }

    @Override
    public void unMute(UUID player, String unbanner, String reason) throws IOException {
        unBans.set(player + "." + Type.CHAT.toString() + ".unbanner", unbanner);
        unBans.set(player + "." + Type.CHAT.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            unBans.set(player + "." + Type.CHAT.toString() + ".reason", reason);


        bans.set(player.toString() + "." + Type.CHAT.toString(), null);

        unBans.save(unBansFile);
        bans.save(bansFile);
    }

    @Override
    public void unMute(UUID player, UUID unbanner) throws IOException {
        unMute(player, unbanner.toString(), null);
    }

    @Override
    public void unMute(UUID player, String unbanner) throws IOException {
        unMute(player, unbanner, null);
    }

    @Override
    public void deleteHistory(UUID player, String actor) {

    }

    @Override
    public String getBanReason(UUID player, Type type) {
        return bans.getString(player + "." + type.toString() + ".reason");
    }

    @Override
    public Long getEnd(UUID player, Type type) {
        return bans.getLong(player + "." + type.toString() + ".end");
    }

    @Override
    public String getBanner(UUID player, Type type) {
        return bans.getString(player + "." + type.toString() + ".creator");
    }

    @Override
    public Long getRemainingTime(UUID player, Type type) {
        return System.currentTimeMillis() - bans.getLong(player + "." + type.toString() + ".end");
    }

    @Override
    public String getReason(UUID player, Type type) {
        return bans.getString(player + "." + type.toString() + ".reason");
    }

    @Override
    public int getLevel(UUID player, String reason) throws UnknownHostException {
        int lvl = 0;

        for(History h : getHistory(player)) {
            if(h.getReason().equals(reason))
                lvl++;
        }

        return lvl;
    }

    @Override
    public Long getCreationDate(UUID player, Type type) throws SQLException {
        return null;
    }

    @Override
    public List<History> getHistory(UUID player) throws UnknownHostException {
        List<History> histories = new ArrayList<>();
        List<String> selection = banHistories.getSection(player.toString()).getKeys();

        for (String cd : selection) {
            Inet4Address v4 = null;
            if(banHistories.getString(player + "." + cd + ".ip") != null) {
                v4 = (Inet4Address) Inet4Address.getByName(banHistories.getString(player + "." + cd + ".ip"));
            }

            History h = new History(player,
                    banHistories.getString(player + "." + cd + ".creator"),
                    banHistories.getString(player + "." + cd + ".reason"),
                    Long.valueOf(cd),
                    banHistories.getLong(player + "." + cd + ".end"),
                    Type.valueOf(banHistories.getString(player + "." + cd + ".type")),
                    v4);

            histories.add(h);
        }
        return histories;
    }

    @Override
    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) {
        return null;
    }

    @Override
    public boolean hasHistory(UUID player) throws UnknownHostException {
        return !getHistory(player).isEmpty();
    }

    @Override
    public boolean hasHistory(UUID player, String reason) throws UnknownHostException {
        for(History h : getHistory(player)) {
            if(h.getReason().equals(reason))
                return true;
        }
        return false;
    }

    @Override
    public boolean isBanned(UUID player, Type type) {
        return bans.getString(player + "." + type.toString()) != null;
    }
}
