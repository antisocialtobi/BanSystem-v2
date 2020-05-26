package net.coalcube.bansystem.core.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

public interface BanManager {
    void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException;
    void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException;
    void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException;
    void ban(UUID player, long time, String creator, Type type, String reason) throws IOException;
    void unBan(UUID player, UUID unBanner, String reason) throws IOException;
    void unBan(UUID player, String unBanner, String reason) throws IOException;
    void unBan(UUID player, UUID unBanner) throws IOException;
    void unBan(UUID player, String unBanner) throws IOException;
    void unMute(UUID player, UUID unBanner, String reason) throws IOException;
    void unMute(UUID player, String unBanner, String reason) throws IOException;
    void unMute(UUID player, UUID unBanner) throws IOException;
    void unMute(UUID player, String unBanner) throws IOException;
    void clearHistory(UUID player);
    String getBanReason(UUID player, Type type);
    Long getEnd(UUID player, Type type);
    String getBanner(UUID player, Type type);
    Long getRemainingTime(UUID player, Type type);
    String getReason(UUID player, Type type);
    int getLevel(UUID player, String reason) throws UnknownHostException;
    List<History> getHistory(UUID player) throws UnknownHostException;
    List<UUID> getBannedPlayersWithSameIP(InetAddress address);
    boolean hasHistory(UUID player) throws UnknownHostException;
    boolean hasHistory(UUID player, String reason) throws UnknownHostException;
    boolean isBanned(UUID player, Type type);
}
