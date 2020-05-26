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

    private final Config BANS;
    private final Config BANHISTORIES;
    private final Config UNBANS;
    private final File BANSFILE;
    private final File UNBANSFILE;
    private final File BANHISTORIESFILE;

    public BanManagerFile(Config bans, Config banhistories, Config unbans, File database) {
        this.BANS = bans;
        this.BANHISTORIES = banhistories;
        this.UNBANS = unbans;
        this.BANSFILE = new File(database.getPath(), "bans.yml");
        this.BANHISTORIESFILE = new File(database.getPath(), "banhistories.yml");
        this.UNBANSFILE = new File(database.getPath(), "unbans.yml");
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

        BANS.set(player + "." + type.toString() + ".duration", time);
        BANS.set(player + "." + type.toString() + ".creator", creator);
        BANS.set(player + "." + type.toString() + ".reason", reason);
        BANS.set(player + "." + type.toString() + ".creationdate", currenttime);
        BANS.set(player + "." + type.toString() + ".end", currenttime + time);
        if(adress != null)
            BANS.set(player + "." + type.toString() + ".ip", adress.getAddress());

        BANHISTORIES.set(player + "." + currenttime + ".duration", time);
        BANHISTORIES.set(player + "." + currenttime + ".creator", creator);
        BANHISTORIES.set(player + "." + currenttime + ".reason", reason);
        BANHISTORIES.set(player + "." + currenttime + ".type", type.toString());
        BANHISTORIES.set(player + "." + currenttime + ".end", currenttime + time);
        if(adress != null)
            BANHISTORIES.set(player + "." + type.toString() + ".ip", adress.getAddress());

        BANS.save(BANSFILE);
        BANHISTORIES.save(BANHISTORIESFILE);
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
        UNBANS.set(player + "." + Type.NETWORK.toString() + ".unbanner", unbanner);
        UNBANS.set(player + "." + Type.NETWORK.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            UNBANS.set(player + Type.CHAT.toString() + ".reason", reason);

        if(isBanned(player, Type.CHAT))
            BANS.set(player.toString() + "." + Type.NETWORK.toString(), null);
        else
            BANS.set(player.toString(), null);


        BANS.save(BANSFILE);
        UNBANS.save(UNBANSFILE);
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
        UNBANS.set(player + "." + Type.CHAT.toString() + ".unbanner", unbanner);
        UNBANS.set(player + "." + Type.CHAT.toString() + ".date", System.currentTimeMillis());
        if(reason != null)
            UNBANS.set(player + "." + Type.CHAT.toString() + ".reason", reason);


        BANS.set(player.toString() + "." + Type.CHAT.toString(), null);

        UNBANS.save(UNBANSFILE);
        BANS.save(BANSFILE);
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
        return BANS.getString(player + "." + type.toString() + ".reason");
    }

    @Override
    public Long getEnd(UUID player, Type type) {
        return BANS.getLong(player + "." + type.toString() + ".end");
    }

    @Override
    public String getBanner(UUID player, Type type) {
        return BANS.getString(player + "." + type.toString() + ".creator");
    }

    @Override
    public Long getRemainingTime(UUID player, Type type) {
        return System.currentTimeMillis() - BANS.getLong(player + "." + type.toString() + ".end");
    }

    @Override
    public String getReason(UUID player, Type type) {
        return BANS.getString(player + "." + type.toString() + ".reason");
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
        List<String> selection = BANHISTORIES.getSection(player.toString()).getKeys();

        for (String cd : selection) {
            Inet4Address v4 = null;
            if(BANHISTORIES.getString(player + "." + cd + ".ip") != null) {
                v4 = (Inet4Address) Inet4Address.getByName(BANHISTORIES.getString(player + "." + cd + ".ip"));
            }

            History h = new History(player,
                    BANHISTORIES.getString(player + "." + cd + ".creator"),
                    BANHISTORIES.getString(player + "." + cd + ".reason"),
                    Long.valueOf(cd),
                    BANHISTORIES.getLong(player + "." + cd + ".end"),
                    Type.valueOf(BANHISTORIES.getString(player + "." + cd + ".type")),
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
        return BANS.getString(player + "." + type.toString()) != null;
    }
}
