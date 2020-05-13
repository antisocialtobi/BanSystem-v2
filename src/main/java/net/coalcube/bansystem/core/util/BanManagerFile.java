package net.coalcube.bansystem.core.util;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManagerFile implements BanManager {

    private Config bans;
    private Config banhistories;
    private Config unbans;
    private File bansFile;
    private File unbansFile;
    private File banhistoriesFile;

    public BanManagerFile(Config bans, Config banhistories, Config unbans, File database) {
        this.bans = bans;
        this.banhistories = banhistories;
        this.unbans = unbans;
        this.bansFile = new File(database.getPath(), "bans.yml");
        this.banhistoriesFile = new File(database.getPath(), "banhistories.yml");
        this.unbansFile = new File(database.getPath(), "unbans.yml");
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

        banhistories.set(player + "." + currenttime + ".duration", time);
        banhistories.set(player + "." + currenttime + ".creator", creator);
        banhistories.set(player + "." + currenttime + ".reason", reason);
        banhistories.set(player + "." + currenttime + ".type", type.toString());
        banhistories.set(player + "." + currenttime + ".end", currenttime + time);
        if(adress != null)
            banhistories.set(player + "." + type.toString() + ".ip", adress.getAddress());

        bans.save(bansFile);
        banhistories.save(banhistoriesFile);
    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException {
        ban(player, time, creator, type, reason, null);
    }

    @Override
    public void unban(UUID player, UUID unbanner, String reason) throws IOException {
        unban(player, unbanner.toString(), reason);
    }

    @Override
    public void unban(UUID player, String unbanner, String reason) throws IOException {
        unbans.set(player + "." + Type.NETWORK.toString() + ".unbanner", unbanner);
        unbans.set(player + "." + Type.NETWORK.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            unbans.set(player + Type.CHAT.toString() + ".reason", reason);

        if(isBanned(player, Type.CHAT))
            bans.set(player.toString() + "." + Type.NETWORK.toString(), null);
        else
            bans.set(player.toString(), null);


        bans.save(bansFile);
        unbans.save(unbansFile);
    }

    @Override
    public void unban(UUID player, UUID unbanner) throws IOException {
        unban(player, unbanner.toString(), null);
    }

    @Override
    public void unban(UUID player, String unbanner) throws IOException {
        unban(player, unbanner, null);
    }

    @Override
    public void unmute(UUID player, UUID unbanner, String reason) throws IOException {
        unmute(player, unbanner.toString(), reason);
    }

    @Override
    public void unmute(UUID player, String unbanner, String reason) throws IOException {
        unbans.set(player + "." + Type.CHAT.toString() + ".unbanner", unbanner);
        unbans.set(player + "." + Type.CHAT.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            unbans.set(player + "." + Type.CHAT.toString() + ".reason", reason);


        bans.set(player.toString() + "." + Type.CHAT.toString(), null);

        unbans.save(unbansFile);
        bans.save(bansFile);
    }

    @Override
    public void unmute(UUID player, UUID unbanner) throws IOException {
        unmute(player, unbanner.toString(), null);
    }

    @Override
    public void unmute(UUID player, String unbanner) throws IOException {
        unmute(player, unbanner, null);
    }

    @Override
    public void clearHistory(UUID player) {

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
    public List<History> getHistory(UUID player) throws UnknownHostException {
        List<History> histories = new ArrayList<>();
        List<String> selection = banhistories.getSection(player.toString()).getKeys();

        for (String cd : selection) {
            Inet4Address v4 = null;
            if(banhistories.getString(player + "." + cd + ".ip") != null) {
                v4 = (Inet4Address) Inet4Address.getByName(banhistories.getString(player + "." + cd + ".ip"));
            }

            History h = new History(player,
                    banhistories.getString(player + "." + cd + ".creator"),
                    banhistories.getString(player + "." + cd + ".reason"),
                    Long.valueOf(cd),
                    banhistories.getLong(player + "." + cd + ".end"),
                    Type.valueOf(banhistories.getString(player + "." + cd + ".type")),
                    v4);

            histories.add(h);
        }
        return histories;
    }

    @Override
    public boolean hashistory(UUID player) throws UnknownHostException {
        return !getHistory(player).isEmpty();
    }

    @Override
    public boolean hashistory(UUID player, String reason) throws UnknownHostException {
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
