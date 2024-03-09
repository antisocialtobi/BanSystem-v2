package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.sql.MySQL;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class BanManagerMySQL implements BanManager {

    private final MySQL mysql;

    public BanManagerMySQL(MySQL mysql) {
        this.mysql = mysql;
    }

    public void log(String action, String creator, String target, String note) throws SQLException {
        mysql.update("INSERT INTO `logs` (`action`, `target`, `creator`, `note`, `creationdate`) " +
                "VALUES ('" + action + "', '" + target + "','" + creator + "', '" + note + "', NOW());");
    }

    @Override
    public Log getLog(int id) throws SQLException, ExecutionException, InterruptedException {
        ResultSet rs = mysql.getResult("SELECT * FROM `logs` WHERE id=" + id + ";");
        Log log = null;
        while(rs.next()) {
            String target, creator, action, note;
            Date date;

            target = rs.getString("target");
            creator = rs.getString("creator");
            action = rs.getString("action");
            note = rs.getString("note");
            date = rs.getTimestamp("creationdate");

            log = new Log(id, target, creator, action, note, date);
        }
        return log;
    }

    @Override
    public List<Log> getAllLogs() throws SQLException, ExecutionException, InterruptedException {
        ResultSet rs = mysql.getResult("SELECT * FROM `logs` ORDER BY creationdate DESC;");
        List<Log> logs = new ArrayList<>();
        while(rs.next()) {
            int id;
            String target, creator, action, note;
            Date date;

            id = rs.getInt("id");
            target = rs.getString("target");
            creator = rs.getString("creator");
            action = rs.getString("action");
            note = rs.getString("note");
            date = rs.getTimestamp("creationdate");

            Log log = new Log(id, target, creator, action, note, date);
            logs.add(log);
        }
        return logs;
    }

    @Override
    public void clearLogs() throws SQLException {
        mysql.update("TRUNCATE TABLE logs;");
    }

    public void kick(UUID player, String creator) throws SQLException {
        kick(player, creator, "");
    }

    public void kick(UUID player, UUID creator) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public void kick(UUID player, String creator, String reason) throws SQLException {
        mysql.update("INSERT INTO `kicks` (`player`, `creator`, `reason`, `creationdate`) " +
                "VALUES ('" + player + "', '" + creator + "', '" + reason + "', NOW());");
    }

    public void kick(UUID player, UUID creator, String reason) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason, v4adress);
    }

    public void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason);
    }

    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        mysql.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', NOW(), '" + creator + "', '" + reason + "', '" + v4adress.getHostAddress() + "', '" + type + "');");

        mysql.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `ip`, `type`, `creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + v4adress.getHostAddress() + "', '" + type + "', NOW());");
    }

    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException, SQLException {
        mysql.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', NOW(), '" + creator + "', '" + reason + "', '','" + type + "');");

        mysql.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `type`, `ip`,`creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + type + "', '', NOW());");
    }

    public void unBan(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unBan(player, unBanner.toString(), reason);
    }

    public void unBan(UUID player, String unBanner, String reason) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.NETWORK + "';");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(), '" + reason + "','" + Type.NETWORK + "');");
    }

    public void unBan(UUID player, UUID unBanner) throws IOException, SQLException {
        unBan(player, unBanner.toString());
    }

    public void unBan(UUID player, String unBanner) throws IOException, SQLException {
        unBan(player, unBanner, "");
    }

    public void unMute(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unMute(player, unBanner.toString(), reason);
    }

    public void unMute(UUID player, String unBanner, String reason) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.CHAT + "'");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(), '" + reason + "','" + Type.CHAT + "');");
    }

    public void unMute(UUID player, UUID unBanner) throws IOException, SQLException {
        unMute(player, unBanner.toString());
    }

    public void unMute(UUID player, String unBanner) throws IOException, SQLException {
        unMute(player, unBanner, "");
    }

    public void deleteHistory(UUID player) throws SQLException {
        mysql.update("DELETE FROM `banhistories` WHERE player = '" + player + "';");
        mysql.update("DELETE FROM `kicks` WHERE player = '" + player + "';");
        mysql.update("DELETE FROM `unbans` WHERE player = '" + player + "';");
        mysql.update("DELETE FROM `logs` WHERE target = '" + player + "' AND action='Deleted History';");
    }

    public void setIP(UUID player, InetAddress address) throws SQLException {
        mysql.update("UPDATE `bans` SET ip='" + address.getHostAddress() + "' WHERE (ip IS NULL or ip = '') AND player = '" + player + "';");
    }

    @Override
    public void saveBedrockUser(UUID uuid, String username) throws SQLException {
        mysql.update("INSERT INTO `bedrockplayer` (`username`, `uuid`) VALUES ('" + username + "', '" + uuid + "')");
    }

    public String getBanReason(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public Long getEnd(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT duration FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            Long duration = resultSet.getLong("duration");

            return (duration == -1) ? duration : getCreationDate(player, type) + duration;
        }
        return null;
    }

    public String getBanner(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT creator FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            try {
                UUID uuid = UUID.fromString(resultSet.getString("creator"));
                return UUIDFetcher.getName(uuid);
            } catch (IllegalArgumentException exception) {
                return resultSet.getString("creator");
            }

        }
        return null;
    }

    public Long getRemainingTime(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        return (getEnd(player, type) == -1) ? -1 : getEnd(player, type) - System.currentTimeMillis();
    }

    public String getReason(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public int getLevel(UUID player, String reason) throws UnknownHostException, SQLException, ExecutionException, InterruptedException {
        int lvl = 0;
        if (hasHistory(player, reason)) {
            ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "' AND reason = '" + reason + "';");
            while (resultSet.next()) {
                lvl++;
            }
        }
        return lvl;
    }

    public Long getCreationDate(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT creationdate FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getTimestamp("creationdate").getTime();
        }
        return null;
    }

    public List<History> getHistory(UUID player) throws UnknownHostException, SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        List<History> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(new History(HistoryType.BAN,
                    UUID.fromString(resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    resultSet.getTimestamp("creationdate").getTime(),
                    resultSet.getLong("duration"),
                    Type.valueOf(resultSet.getString("type")),
                    (resultSet.getString("ip") == null ? null : InetAddress.getByName(resultSet.getString("ip")))));
        }

        resultSet = mysql.getResult("SELECT * FROM `kicks` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            HistoryType historyType = HistoryType.KICK;
            if(resultSet.getString("reason") != null && !resultSet.getString("reason").isEmpty())
                historyType = HistoryType.KICKWITHREASON;
            list.add(new History(historyType,
                    UUID.fromString(resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    resultSet.getTimestamp("creationdate").getTime(),
                    null,
                    null,
                    null));
        }
        resultSet = mysql.getResult("SELECT * FROM `unbans` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            Type type = Type.valueOf(resultSet.getString("type"));
            HistoryType historyType = HistoryType.UNBAN;
            if(type == Type.NETWORK) {
                if(resultSet.getString("reason") != null && !resultSet.getString("reason").isEmpty())
                    historyType = HistoryType.UNBANWITHREASON;
            } else {
                if(resultSet.getString("reason") != null && !resultSet.getString("reason").isEmpty())
                    historyType = HistoryType.UNMUTEWITHREASON;
                else
                    historyType = HistoryType.UNMUTE;
            }

            list.add(new History(historyType,
                    UUID.fromString(resultSet.getString("player")),
                    resultSet.getString("unbanner"),
                    resultSet.getString("reason"),
                    resultSet.getTimestamp("creationdate").getTime(),
                    null,
                    type,
                    null));
        }
        resultSet = mysql.getResult("SELECT * FROM logs WHERE target='" + player + "' AND action='Deleted History';");
        while (resultSet.next()) {
            list.add(new History(
                    HistoryType.CLEAR,
                    player,
                    resultSet.getString("creator"),
                    null,
                    resultSet.getTimestamp("creationdate").getTime(),
                    null,
                    null,
                    null));
        }

        return list;
    }

    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bans` WHERE ip = '" + address.getHostAddress() + "';");
        List<UUID> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(UUID.fromString(resultSet.getString("player")));
        }
        return list;
    }

    @Override
    public String getSavedBedrockUsername(UUID player) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bedrockplayer` WHERE uuid = '" + player + "';");
        while (resultSet.next()) {
            return resultSet.getString("username");
        }
        return null;
    }

    @Override
    public UUID getSavedBedrockUUID(String username) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bedrockplayer` WHERE username = '" + username + "';");
        while (resultSet.next()) {
            return UUID.fromString(resultSet.getString("uuid"));
        }
        return null;
    }

    public boolean hasHistory(UUID player) throws UnknownHostException, SQLException, ExecutionException, InterruptedException {
        return !getHistory(player).isEmpty();
    }

    public boolean hasHistory(UUID player, String reason) throws UnknownHostException, SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player='" + player + "' AND reason='" + reason + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSavedBedrockPlayer(UUID player) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bedrockplayer` WHERE uuid = '" + player + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSavedBedrockPlayer(String username) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bedrockplayer` WHERE username = '" + username + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isBanned(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' and type = '" + type.toString() + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isSetIP(UUID player) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' AND (ip is null or ip = '');");
        while (resultSet.next()) {
            return false;
        }
        return true;
    }
}
