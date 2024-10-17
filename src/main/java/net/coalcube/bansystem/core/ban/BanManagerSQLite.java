package net.coalcube.bansystem.core.ban;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.sql.SQLite;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import org.bstats.charts.SimplePie;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class BanManagerSQLite implements BanManager {

    private final SQLite sqlite;
    SimpleDateFormat simpleDateFormat;
    private final YamlDocument config;
    private final MetricsAdapter metricsAdapter;

    public BanManagerSQLite(SQLite sqlite, YamlDocument config) {
        this.sqlite = sqlite;
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2024-05-23 15:44:17"
        this.config = config;
        this.metricsAdapter = BanSystem.getInstance().getMetricsAdapter();
    }

    @Override
    public Ban getBan(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet rs = sqlite.getResult("SELECT * FROM `bans` WHERE player='" + player.toString() + "' AND type='" + type + "';");

        long duration = 0;
        String reason = "",
                creator = "",
                ip = "",
                id = "";
        Date creationDate = null;

        while(rs.next()) {
            duration = rs.getLong("duration");
            reason = rs.getString("reason");
            creator = rs.getString("creator");
            ip = rs.getString("ip");
            creationDate = rs.getTimestamp("creationdate");
            id = rs.getString("id");

            return new Ban(id, player, type, reason, creator, ip, creationDate, duration);
        }
        return null;
    }

    @Override
    public Ban getBan(String id) throws SQLException, ExecutionException, InterruptedException {
        ResultSet rs = sqlite.getResult("SELECT * FROM `bans` WHERE id='" + id + "';");

        long duration = 0;
        String reason = "",
                creator = "",
                ip = "";
        Date creationDate = null;
        UUID player = null;
        Type type = null;

        while(rs.next()) {
            id = rs.getString("id");
            duration = rs.getLong("duration");
            reason = rs.getString("reason");
            creator = rs.getString("creator");
            ip = rs.getString("ip");
            creationDate = rs.getTimestamp("creationdate");
            player = UUID.fromString(rs.getString("player"));
            type = Type.valueOf(rs.getString("type"));

            return new Ban(id, player, type, reason, creator, ip, creationDate, duration);
        }
        return null;
    }

    @Override
    public List<Ban> getAllBans() throws SQLException, ExecutionException, InterruptedException {
        List<Ban> bans = new ArrayList<>();
        ResultSet rs = sqlite.getResult("SELECT * FROM `bans`;");

        long duration = 0;
        String reason = "",
                creator = "",
                ip = "",
                id = "";
        Date creationDate = null;
        UUID player = null;
        Type type = null;

        while(rs.next()) {
            id = rs.getString("id");
            duration = rs.getLong("duration");
            reason = rs.getString("reason");
            creator = rs.getString("creator");
            ip = rs.getString("ip");
            creationDate = rs.getTimestamp("creationdate");
            player = UUID.fromString(rs.getString("player"));
            type = Type.valueOf(rs.getString("type"));

            bans.add(new Ban(id, player, type, reason, creator, ip, creationDate, duration));
        }

        return bans;
    }

    @Override
    public List<Ban> getAllBans(Type type) throws SQLException, ExecutionException, InterruptedException {
        List<Ban> bans = new ArrayList<>();
        for(Ban ban : getAllBans()) {
            if(ban.getType() == type) {
                bans.add(ban);
            }
        }
        return bans;
    }

    public void log(String action, String creator, String target, String note) throws SQLException {
        sqlite.update("INSERT INTO `logs` (`action`, `target`, `creator`, `note`, `creationdate`) " +
                "VALUES ('" + action + "', '" + target + "','" + creator + "', '" + note + "', datetime('now', 'localtime'));");
    }

    @Override
    public Log getLog(int id) throws SQLException, ExecutionException, InterruptedException, ParseException {
        ResultSet rs = sqlite.getResult("SELECT * FROM `logs` WHERE id=" + id + ";");
        Log log = null;
        while(rs.next()) {
            String target, creator, action, note;
            Date date;

            target = rs.getString("target");
            creator = rs.getString("creator");
            action = rs.getString("action");
            note = rs.getString("note")
                    .replaceAll("\\{", "\u007B")
                    .replaceAll("\\$", "\\\\\\$");
            date = simpleDateFormat.parse(rs.getString("creationdate"));

            log = new Log(id, target, creator, action, note, date);
        }
        return log;
    }

    @Override
    public List<Log> getAllLogs() throws SQLException, ExecutionException, InterruptedException, ParseException {
        ResultSet rs = sqlite.getResult("SELECT * FROM `logs` ORDER BY creationdate DESC;");
        List<Log> logs = new ArrayList<>();
        while(rs.next()) {
            int id;
            String target, creator, action, note;
            Date date;

            id = rs.getInt("id");
            target = rs.getString("target");
            creator = rs.getString("creator");
            action = rs.getString("action");
            note = rs.getString("note")
                    .replaceAll("\\{", "\u007B")
                    .replaceAll("\\$", "\\\\\\$");
            date = simpleDateFormat.parse(rs.getString("creationdate"));

            Log log = new Log(id, target, creator, action, note, date);
            logs.add(log);
        }
        return logs;
    }

    @Override
    public void clearLogs() throws SQLException {
        sqlite.update("TRUNCATE TABLE logs;");
    }
    public void kick(UUID player, String creator) throws SQLException {
        kick(player, creator, "");
    }

    public void kick(UUID player, UUID creator) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public void kick(UUID player, String creator, String reason) throws SQLException {
        sqlite.update("INSERT INTO `kicks` (`player`, `creator`, `reason`, `creationdate`) " +
                "VALUES ('" + player + "', '" + creator + "', '" + reason + "', datetime('now', 'localtime'));");
        metricsAdapter.addCustomChart(new SimplePie("punishments", () -> {
            return "Kick";
        }));
    }

    public void kick(UUID player, UUID creator, String reason) throws SQLException {
        kick(player, creator.toString(), "");
    }

    public Ban ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException, ExecutionException, InterruptedException {
        return ban(player, time, creator.toString(), type, reason, v4adress);
    }

    public Ban ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException {
        return ban(player, time, creator.toString(), type, reason);
    }

    public Ban ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException, ExecutionException, InterruptedException {
        String id = generateNewID();

        sqlite.update("INSERT INTO `bans` (`id`, `player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + id + "', '" + player + "', '" + time + "', datetime('now', 'localtime'), '" + creator + "', '" + reason + "', '" + v4adress.getHostAddress() + "', '" + type + "');");

        sqlite.update("INSERT INTO `banhistories` (`id`, `player`, `duration`, `creator`, `reason`, `ip`, `type`, `creationdate`) " +
                "VALUES ('" + id + "', '" + player + "', '" + time + "', '" + creator + "', '" + reason + "', " +
                "'" + v4adress.getHostName() + "', '" + type + "', datetime('now', 'localtime'));");
        if(type == Type.CHAT) {
            BanSystem.getInstance().addCachedMutedPlayerNames(UUIDFetcher.getName(player));
            metricsAdapter.addCustomChart(new SimplePie("punishments", () -> {
                return "Mute";
            }));
        } else {
            BanSystem.getInstance().addCachedBannedPlayerNames(UUIDFetcher.getName(player));
            metricsAdapter.addCustomChart(new SimplePie("punishments", () -> {
                return "Ban";
            }));
        }
        return new Ban(id, player, type, reason, creator, v4adress.getHostAddress(), new Date(System.currentTimeMillis()), time);
    }

    public Ban ban(UUID player, long time, String creator, Type type, String reason) throws IOException, SQLException, ExecutionException, InterruptedException {
        String id = generateNewID();

        sqlite.update("INSERT INTO `bans` (`id`, `player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + id + "', '" + player + "', '" + time + "', datetime('now', 'localtime')," +
                " '" + creator + "', '" + reason + "', '','" + type + "');");

        sqlite.update("INSERT INTO `banhistories` (`id`, `player`, `duration`, `creator`, `reason`, `type`, `ip`,`creationdate`) " +
                "VALUES ('" + id + "', '" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + type + "', '', datetime('now', 'localtime'));");
        if(type == Type.CHAT) {
            BanSystem.getInstance().addCachedMutedPlayerNames(UUIDFetcher.getName(player));
            metricsAdapter.addCustomChart(new SimplePie("punishments", () -> {
                return "Mute";
            }));
        } else {
            BanSystem.getInstance().addCachedBannedPlayerNames(UUIDFetcher.getName(player));
            metricsAdapter.addCustomChart(new SimplePie("punishments", () -> {
                return "Ban";
            }));
        }
        return new Ban(id, player, type, reason, creator, null, new Date(System.currentTimeMillis()), time);
    }

    @Override
    public void unBan(Ban ban, String unBanner, String reason) throws SQLException, ExecutionException, InterruptedException {
        sqlite.update("DELETE FROM `bans` WHERE id = '" + ban.getId() + "';");
        sqlite.update("INSERT INTO `unbans` (`id`, `player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + ban.getId() + "', '" + ban.getPlayer() + "', '" + unBanner + "', datetime('now', 'localtime'), '" + reason + "','" + ban.getType() +"');");
        if(ban.getType() == Type.NETWORK)
            BanSystem.getInstance().removeCachedBannedPlayerNames(UUIDFetcher.getName(ban.getPlayer()));
        else
            BanSystem.getInstance().removeCachedMutedPlayerNames(UUIDFetcher.getName(ban.getPlayer()));
    }

    @Override
    public void unBan(Ban ban, UUID unBanner, String reason) throws SQLException, ExecutionException, InterruptedException {
        unBan(ban, unBanner.toString(), reason);
    }

    @Override
    public void unBan(Ban ban, String unBanner) throws SQLException, ExecutionException, InterruptedException {
        unBan(ban, unBanner.toString(), "");
    }

    @Override
    public void unBan(Ban ban, UUID unBanner) throws SQLException, ExecutionException, InterruptedException {
        unBan(ban, unBanner.toString(), "");
    }

    public void deleteHistory(UUID player) throws SQLException {
        sqlite.update("DELETE FROM `banhistories` WHERE player = '" + player + "';");
        sqlite.update("DELETE FROM `kicks` WHERE player = '" + player + "';");
        sqlite.update("DELETE FROM `unbans` WHERE player = '" + player + "';");
        sqlite.update("DELETE FROM `logs` WHERE target = '" + player + "' AND action='Deleted History';");

    }

    public void setIP(UUID player, InetAddress address) throws SQLException {
        sqlite.update("UPDATE `bans` SET ip='" + address.getHostAddress() + "' WHERE (ip IS NULL or ip = '') AND player = '" + player + "';");
        sqlite.update("UPDATE `banhistories` SET ip='" + address.getHostAddress() + "' WHERE (ip IS NULL or ip = '') AND player = '" + player + "';");
    }

    @Override
    public void saveBedrockUser(UUID uuid, String username) throws SQLException {
        sqlite.update("INSERT INTO `bedrockplayer` (`username`, `uuid`) VALUES ('" + username + "', '" + uuid + "')");
    }

    public String getBanReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public Long getEnd(UUID player, Type type) throws SQLException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT duration FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            Long duration = resultSet.getLong("duration");

            return (duration == -1) ? duration : getCreationDate(player, type) + duration ;
        }
        return null;
    }

    public String getBanner(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT creator FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("creator");
        }
        return null;
    }

    public Long getRemainingTime(UUID player, Type type) throws SQLException, ParseException {
        return (getEnd(player, type) == -1) ? -1 : getEnd(player, type) - System.currentTimeMillis();
    }

    public String getReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    public int getLevel(UUID player, String reason) throws UnknownHostException, SQLException {
        int lvl = 0;
        if(hasHistory(player, reason)) {
            ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "' AND reason = '" + reason + "';");
            while (resultSet.next()) {
                lvl ++;
            }
        }
        return lvl;
    }

    public Long getCreationDate(UUID player, Type type) throws SQLException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT creationdate FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        while (resultSet.next()) {
            return df.parse(resultSet.getString("creationdate")).getTime();
        }
        return null;
    }

    public List<History> getHistory(UUID player) throws UnknownHostException, SQLException, ExecutionException, InterruptedException, ParseException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        List<History> list = new ArrayList<>();
        while (resultSet.next()) {
            InetAddress ip;
            if(resultSet.getString("ip") == null || resultSet.getString("ip").isEmpty()) {
                ip = null;
            } else {
                ip = InetAddress.getByName(resultSet.getString("ip"));
            }

            list.add(new History(HistoryType.BAN,
                    UUID.fromString(resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    simpleDateFormat.parse(resultSet.getString("creationdate")).getTime(),
                    resultSet.getLong("duration"),
                    Type.valueOf(resultSet.getString("type")),
                    ip,
                    resultSet.getString("id")));
        }

        resultSet = sqlite.getResult("SELECT * FROM `kicks` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            HistoryType historyType = HistoryType.KICK;
            if(resultSet.getString("reason") != null && !resultSet.getString("reason").isEmpty())
                historyType = HistoryType.KICKWITHREASON;
            list.add(new History(historyType,
                    UUID.fromString(resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    simpleDateFormat.parse(resultSet.getString("creationdate")).getTime(),
                    null,
                    null,
                    null,
                    null));
        }
        resultSet = sqlite.getResult("SELECT * FROM `unbans` WHERE player = '" + player + "';");
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
                    simpleDateFormat.parse(resultSet.getString("creationdate")).getTime(),
                    null,
                    type,
                    null,
                    resultSet.getString("id")));
        }
        resultSet = sqlite.getResult("SELECT * FROM logs WHERE target='" + player + "' AND action='Deleted History';");
        while (resultSet.next()) {
            list.add(new History(
                    HistoryType.CLEAR,
                    player,
                    resultSet.getString("creator"),
                    null,
                    simpleDateFormat.parse(resultSet.getString("creationdate")).getTime(),
                    null,
                    null,
                    null,
                    null));
        }

        return list;
    }

    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bans` WHERE ip = '" + address.getHostAddress() + "';");
        List<UUID> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(UUID.fromString(resultSet.getString("player")));
        }
        return list;
    }

    @Override
    public String getSavedBedrockUsername(UUID player) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bedrockplayer` WHERE uuid = '" + player + "';");
        while (resultSet.next()) {
            return resultSet.getString("username");
        }
        return null;
    }

    @Override
    public UUID getSavedBedrockUUID(String username) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bedrockplayer` WHERE username = '" + username + "';");
        while (resultSet.next()) {
            return UUID.fromString(resultSet.getString("uuid"));
        }
        return null;
    }

    public boolean hasHistory(UUID player) throws UnknownHostException, SQLException, ParseException, ExecutionException, InterruptedException {
        return !getHistory(player).isEmpty();
    }

    public boolean hasHistory(UUID player, String reason) throws UnknownHostException, SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `banhistories` WHERE player='" + player + "' AND reason='" + reason + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSavedBedrockPlayer(UUID player) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bedrockplayer` WHERE uuid = '" + player + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSavedBedrockPlayer(String username) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bedrockplayer` WHERE username = '" + username + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isBanned(UUID player, Type type) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' and type = '" + type.toString() + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    public boolean isSetIP(UUID player) throws SQLException {
        ResultSet resultSet = sqlite.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' AND (ip is null or ip = '');");
        while (resultSet.next()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isMaxBanLvl(String id, int lvl) {
        int maxLvl = 0;

        for (Object key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.parseInt(key.toString()) > maxLvl) maxLvl = Integer.parseInt(key.toString());
        }
        return lvl >= maxLvl;
    }

    @Override
    public int getMaxLvl(String id) {
        return config.getSection("IDs." + id + ".lvl").getKeys().size();
    }

    @Override
    public String generateNewID() throws SQLException, ExecutionException, InterruptedException {
        String uuid = UUID.randomUUID().toString();
        String id = "";
        int i = 0;
        for(String character :  uuid.split("")) {
            if(i >= 5) {
                break;
            }
            id = id + character;
            i++;
        }
        ResultSet rs = sqlite.getResult("SELECT id FROM `banhistories` WHERE id='" + id + "'");
        while(rs.next()) {
            return generateNewID();
        }

        return id;
    }
}
