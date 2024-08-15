package net.coalcube.bansystem.core.ban;

import net.coalcube.bansystem.core.util.History;
import net.coalcube.bansystem.core.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public interface BanManager {

    Ban getBan(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException;
    Ban getBan(String id) throws SQLException, ExecutionException, InterruptedException;
    List<Ban> getAllBans() throws SQLException, ExecutionException, InterruptedException;
    List<Ban> getAllBans(Type type) throws SQLException, ExecutionException, InterruptedException;
    void log(String action, String creator, String target, String note) throws SQLException;
    Log getLog(int id) throws SQLException, ExecutionException, InterruptedException, ParseException;
    List<Log> getAllLogs() throws SQLException, ExecutionException, InterruptedException, ParseException;
    void clearLogs() throws SQLException;
    void kick(UUID player, String creator) throws SQLException;
    void kick(UUID player, UUID creator) throws SQLException;
    void kick(UUID player, String creator, String reason) throws SQLException;
    void kick(UUID player, UUID creator, String reason) throws SQLException;
    Ban ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException, ExecutionException, InterruptedException;
    Ban ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException;
    Ban ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException, ExecutionException, InterruptedException;
    Ban ban(UUID player, long time, String creator, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException;
    void unBan(Ban ban, String unBanner, String reason) throws SQLException, ExecutionException, InterruptedException;
    void unBan(Ban ban, UUID unBanner, String reason) throws SQLException, ExecutionException, InterruptedException;
    void unBan(Ban ban, String unBanner) throws SQLException, ExecutionException, InterruptedException;
    void unBan(Ban ban, UUID unBanner) throws SQLException, ExecutionException, InterruptedException;
//    void unBan(UUID player, UUID unBanner, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException;
//    void unBan(UUID player, String unBanner, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException;
//    void unBan(UUID player, UUID unBanner, Type type) throws IOException, SQLException, ExecutionException, InterruptedException;
//    void unBan(UUID player, String unBanner, Type type) throws IOException, SQLException, ExecutionException, InterruptedException;
    void deleteHistory(UUID player) throws SQLException;
    void setIP(UUID player, InetAddress address) throws SQLException;
    void saveBedrockUser(UUID uuid, String username) throws SQLException;
    //String getBanReason(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException;
    //Long getEnd(UUID player, Type type) throws SQLException, ParseException, ExecutionException, InterruptedException;
    //String getBanner(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException;
    //Long getRemainingTime(UUID player, Type type) throws SQLException, ParseException, ExecutionException, InterruptedException;
    //String getReason(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException;
    int getLevel(UUID player, String reason) throws UnknownHostException, SQLException, ExecutionException, InterruptedException;
    //Long getCreationDate(UUID player, Type type) throws SQLException, ParseException, ExecutionException, InterruptedException;
    List<History> getHistory(UUID player) throws UnknownHostException, SQLException, ParseException, ExecutionException, InterruptedException;
    List<UUID> getBannedPlayersWithSameIP(InetAddress address) throws SQLException, ExecutionException, InterruptedException;
    String getSavedBedrockUsername(UUID player) throws SQLException, ExecutionException, InterruptedException;
    UUID getSavedBedrockUUID(String username) throws SQLException, ExecutionException, InterruptedException;
    boolean hasHistory(UUID player) throws UnknownHostException, SQLException, ExecutionException, InterruptedException;
    boolean hasHistory(UUID player, String reason) throws UnknownHostException, SQLException, ExecutionException, InterruptedException;
    boolean isSavedBedrockPlayer(UUID player) throws SQLException, ExecutionException, InterruptedException;
    boolean isSavedBedrockPlayer(String username) throws SQLException, ExecutionException, InterruptedException;
    //boolean isBanned(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException;
    boolean isSetIP(UUID player) throws SQLException, ExecutionException, InterruptedException;
    boolean isMaxBanLvl(String id, int lvl);
    int getMaxLvl(String id);
    String generateNewID() throws SQLException, ExecutionException, InterruptedException;
}