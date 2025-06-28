package net.coalcube.bansystem.core.sql;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class MySQL implements Database {

    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private final int port;

    private Connection con;

    public MySQL(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true", username, password);
        } catch (ClassNotFoundException e) {
            System.err.println("Fehler beim Laden des JDBC-Treibers");
            e.printStackTrace();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public ResultSet getResult(String s) throws SQLException, ExecutionException, InterruptedException {
        if(!isConnected()) {
            connect();
        }
        final FutureTask<ResultSet> task = new FutureTask<>(() -> {
            PreparedStatement stmt = con.prepareStatement(s);
            return stmt.executeQuery();
        });
        task.run();

        return task.get();

    }

    @Override
    public void update(String qry) throws SQLException {
        if(!isConnected()) {
            connect();
        }
        new FutureTask<>(() -> {
            try {
                PreparedStatement preparedStatement = con.prepareStatement(qry);
                preparedStatement.execute();
                preparedStatement.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }, 1).run();
    }

    public void importFromOldBanDatabase() throws SQLException, ParseException, ExecutionException, InterruptedException {
        int count = 0;
        HashMap<Integer, UUID> bannedPlayer = new HashMap<>();
        HashMap<Integer, String> reason = new HashMap<>();
        HashMap<Integer, String> creator = new HashMap<>();
        HashMap<Integer, Type> type = new HashMap<>();
        HashMap<Integer, String> ip = new HashMap<>();
        HashMap<Integer, Long> end = new HashMap<>();

        ResultSet resultSet = getResult("SELECT * FROM ban");
        while (resultSet.next()) {
            UUID player = UUID.fromString(resultSet.getString("UUID"));

            bannedPlayer.put(count, player);
            reason.put(count, resultSet.getString("Grund"));
            creator.put(count, resultSet.getString("Ersteller"));
            type.put(count, Type.valueOf(resultSet.getString("Type")));
            ip.put(count, resultSet.getString("IP"));
            end.put(count, resultSet.getLong("Ende"));

            count++;
        }
        update("DROP TABLE `ban`;");

        SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        for(int i = 0; i<count; i++) {
            ResultSet resultSet1 = getResult("SELECT * FROM `banhistory` WHERE UUID='" + bannedPlayer.get(i) + "' AND Ende='" + end.get(i) + "'");
            while (resultSet1.next()) {

                long duration = resultSet1.getLong("duration");
                duration = (duration != -1 ? duration*1000 : duration);

                update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                        "VALUES ('" + bannedPlayer.get(i) + "', '" + duration + "', '"
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(parser.parse(resultSet1.getString("Erstelldatum"))) + "', '" + creator.get(i) + "', '"
                        + reason.get(i) + "', '" + ip.get(i) + "', '" + type.get(i) + "');");
            }
        }
    }

//    public void importFromOldBanHistoriesDatabase() throws SQLException, UnknownHostException, ParseException, ExecutionException, InterruptedException {
//        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
//        ArrayList<History> histories = new ArrayList<>();
//        ResultSet resultSet = getResult("SELECT * FROM banhistory");
//
//        while (resultSet.next()) {
//            UUID player = UUID.fromString(resultSet.getString("UUID"));
//            long duration = resultSet.getLong("duration");
//            InetAddress ip = null;
//            if(resultSet.getString("IP") != null) {
//                ip = InetAddress.getByName(resultSet.getString("IP"));
//            }
//
//            History history = new History(
//                    player,
//                    resultSet.getString("Ersteller"),
//                    resultSet.getString("Grund"),
//                    df.parse(resultSet.getString("Erstelldatum")).getTime(),
//                    (duration != -1 ? duration*1000 : duration),
//                    Type.valueOf(resultSet.getString("Type")),
//                    ip);
//
//            histories.add(history);
//        }
//        update("DROP TABLE `banhistory`;");
//
//        for(History history : histories) {
//            update("INSERT INTO `banhistories` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
//                    "VALUES ('" + history.getPlayer() + "', '" + history.getDuration() + "', '"
//                    + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(history.getCreateDate()) + "', '" + history.getCreator() + "', '"
//                    + history.getReason() + "', '" + history.getIp() + "', '" + history.getType() + "');");
//        }
//    }

    @Override
    public void createTables(YamlDocument config) throws SQLException, ExecutionException, InterruptedException {
        update("CREATE TABLE IF NOT EXISTS `bans` " +
                "( `id` VARCHAR(16) NOT NULL , " +
                " `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL )" +
                " ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `banhistories` " +
                "( `id` VARCHAR(16) NOT NULL , " +
                " `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `ids` " +
                "( `id` INT NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `lvl` INT NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `onlyadmin` BOOLEAN NOT NULL ," +
                " `type` VARCHAR(100) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ) ENGINE = InnoDB;");

//        update("CREATE TABLE IF NOT EXISTS `web_accounts` " +
//                "( `user` VARCHAR(100) NOT NULL ," +
//                " `password` VARCHAR(200) NOT NULL ," +
//                " `creationdate` DATETIME NOT NULL ) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `kicks` " +
                "( `player` VARCHAR(100) NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `logs` " +
                "( `id` INT NOT NULL AUTO_INCREMENT ," +
                " `action` VARCHAR(100) NOT NULL ," +
                " `target` VARCHAR(100) NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ," +
                " `note` VARCHAR(500) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " PRIMARY KEY (`ID`)) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `bedrockplayer` " +
                "( `username` VARCHAR(64) NOT NULL ," +
                " `uuid` VARCHAR(64) NOT NULL ) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `unbans` " +
                "( `id` VARCHAR(16) NOT NULL , " +
                " `player` VARCHAR(36) NOT NULL ," +
                " `unbanner` VARCHAR(36) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `reason` VARCHAR(1000) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL )" +
                " ENGINE = InnoDB;");

        if(!hasUnbanreason()) {
            update("ALTER TABLE `unbans` \n" +
                    "ADD reason varchar(100) NOT NULL \n" +
                    "AFTER unbanner;");
        }
    }

    public void syncIDs(YamlDocument config) throws SQLException, ExecutionException, InterruptedException {
        for(Object id : config.getSection("IDs").getKeys()) {
            if(!isIDexists(id.toString())) {
                for (Object lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
                    update("INSERT INTO ids " +
                            "VALUES ('" + id + "', '" +
                            config.getString("IDs." + id + ".reason") + "', '" +
                            lvl + "', '" +
                            config.getLong("IDs." + id + ".lvl." + lvl + ".duration") + "', " +
                            config.getBoolean("IDs." + id + ".onlyAdmins") + ", '" +
                            config.getString("IDs." + id + ".lvl." + lvl + ".type") + "', NOW(), 'configsync');");
                }
            }
            for(Object lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
                if(!isLvlSync(id.toString(), lvl.toString(), config)) {
                    update("DELETE FROM ids WHERE id='" + id + "' AND lvl='" + lvl + "';");
                    update("INSERT INTO ids " +
                            "VALUES ('" + id + "', '" +
                            config.getString("IDs." + id + ".reason") + "', '" +
                            lvl + "', '" +
                            config.getLong("IDs." + id + ".lvl." + lvl + ".duration") + "', " +
                            config.getBoolean("IDs." + id + ".onlyAdmins") + ", '" +
                            config.getString("IDs." + id + ".lvl." + lvl + ".type") + "', NOW(), 'configsync');");
                }
            }

            if(!isIDsync(id.toString(), config) || !isIDexists(id.toString())) {
                if(isIDexists(id.toString()) && isIDfromConfig(id.toString())) {
                    update("DELETE FROM ids WHERE id='" + id + "';");
                }
                for(Object lvl :  config.getSection("IDs." + id + ".lvl").getKeys()) {
                    update("INSERT INTO ids " +
                            "VALUES ('" + id + "', '" +
                            config.getString("IDs."+ id + ".reason") + "', '" +
                            lvl + "', '" +
                            config.getLong("IDs." + id + ".lvl." + lvl + ".duration") + "', " +
                            config.getBoolean("IDs."+ id + ".onlyAdmins") + ", '" +
                            config.getString("IDs."+ id + ".lvl." + lvl + ".type") + "', NOW(), 'configsync');");
                }
            }
        }

        ResultSet resultSet = getResult("SELECT * FROM `ids`;");

        while (resultSet.next()) {
            if(!config.getSection("IDs").getKeys().contains(resultSet.getString("id")) &&
                    resultSet.getString("creator").equals("configsync")) {
                update("DELETE FROM ids WHERE id='" + resultSet.getString("id") + "';");
            }
            if(!resultSet.getString("creator").equals("configsync")) {
                config.set("IDs." + resultSet.getInt("id") + ".reason", resultSet.getString("reason"));
                config.set("IDs." + resultSet.getInt("id") + ".onlyAdmins", resultSet.getBoolean("onlyadmin"));
                config.set("IDs." + resultSet.getInt("id") + ".lvl." + resultSet.getInt("lvl")
                        + ".type", resultSet.getString("type"));
                config.set("IDs." + resultSet.getInt("id") + ".lvl." + resultSet.getInt("lvl")
                        + ".duration", resultSet.getDouble("duration"));
            }
        }
    }

    private boolean isIDsync(String id, YamlDocument config) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = getResult("SELECT * FROM `ids` WHERE id='" + id + "'");

        while (resultSet.next()) {
            if(!(resultSet.getLong("duration") ==
                    config.getLong("IDs." + id + ".lvl." + resultSet.getString("lvl") + ".duration"))) {
                return false;
            }

            if(!(resultSet.getString("type").equals(
                    config.getString("IDs." + id + ".lvl." + resultSet.getString("lvl") + ".type")))) {
                return false;
            }

            if(!(resultSet.getString("reason").equals(config.getString("IDs." + id + ".reason"))))
                return false;

            if(!(resultSet.getBoolean("onlyadmin") == config.getBoolean("IDs." + id + ".onlyAdmins")))
                return false;
        }
        return true;
    }

    private boolean isLvlSync(String id, String lvl, YamlDocument config) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = getResult("SELECT * FROM `ids` WHERE id='" + id + "' AND lvl='" + lvl + "'");

        while (resultSet.next()) {
            if(!(resultSet.getLong("duration") ==
                    config.getLong("IDs." + id + ".lvl." + lvl + ".duration")))
                return false;

            if(!(resultSet.getString("type").equals(
                    config.getString("IDs." + id + ".lvl." + lvl + ".type"))))
                return false;

        }
        return true;
    }


    private boolean isIDexists(String id) throws SQLException, ExecutionException, InterruptedException {
        ResultSet rs = getResult("SELECT * FROM `ids` WHERE id='" + id + "';");
        while (rs.next()) {
            return true;
        }
        return false;
    }

    private boolean isIDfromConfig(String id) throws SQLException, ExecutionException, InterruptedException {
        ResultSet resultSet = getResult("SELECT `creator` FROM `ids` WHERE id='" + id + "' AND NOT creator='configsync';");

        while (resultSet.next()) {
            return false;
        }
        return true;
    }

    private boolean hasUnbanreason() throws SQLException, ExecutionException, InterruptedException {

        ResultSet rs = getResult("SHOW COLUMNS FROM `unbans` WHERE Field='reason';");

        while (rs.next()) {
            return true;
        }
        return false;
    }

    public boolean isOldDatabase() {
        try {
            ResultSet resultSet = getResult("SHOW COLUMNS FROM ban WHERE field='UUID';");
            while (resultSet.next()) {
                return true;
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            return false;
        }
        return false;
    }

    @Override
    public void disconnect() throws SQLException {
        con.close();
        con = null;
    }

    @Override
    public boolean isConnected() {
        try {
            return (con != null && con.isValid(5));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateTables() throws SQLException, ExecutionException, InterruptedException {
        BanSystem banSystem = BanSystem.getInstance();
        String prefix = banSystem.getConfigurationUtil().getMessage("prefix");

        boolean banIDs = false;
        boolean banHistoryIDs = false;
        boolean unbansIDs = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        BanManager banManager = BanSystem.getInstance().getBanManager();

        ResultSet rs_bans = getResult("SHOW COLUMNS FROM `bans` LIKE 'id';");
        while (rs_bans.next()) {
            banIDs = true;
        }

        ResultSet rs_banhistories = getResult("SHOW COLUMNS FROM `banhistories` LIKE 'id';");
        while (rs_banhistories.next()) {
            banHistoryIDs = true;
        }

        ResultSet rs_unbans = getResult("SHOW COLUMNS FROM `unbans` LIKE 'id';");
        while (rs_unbans.next()) {
            unbansIDs = true;
        }


        if(!banHistoryIDs) {
            update("ALTER TABLE `banhistories` ADD `id` VARCHAR(16) FIRST;");
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §ebanhistories §7wurde geupdated.");
        }

        if(!banIDs) {
            update("ALTER TABLE `bans` ADD `id` VARCHAR(16) FIRST;");
            ResultSet rs = getResult("SELECT * FROM `bans`;");

            while (rs.next()) {
                String id = banManager.generateNewID();
                String player = rs.getString("player");
                Type type = Type.valueOf(rs.getString("type"));

                update("UPDATE `bans` SET id='" + id + "' WHERE player='" + player + "' AND type='" + type + "';");
            }
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §ebans §7wurde geupdated.");
        }

        if(!unbansIDs) {
            update("ALTER TABLE `unbans` ADD `id` VARCHAR(16) FIRST;");
            ResultSet rs = getResult("SELECT * FROM `unbans`;");

            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player"));

                update("UPDATE `bans` SET id='" + banManager.generateNewID() + "' WHERE player='" + player
                        + "' AND creationdate='" + rs.getTimestamp("creationdate") + "';");
            }
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §eunbans §7wurde geupdated.");
        }

        if(!banHistoryIDs) {
            ResultSet rs = getResult("SELECT * FROM `banhistories`;");

            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player"));
                Type type = Type.valueOf(rs.getString("type"));
                Ban ban = banManager.getBan(player, type);
                String id = banManager.generateNewID();

                if(ban != null) {
                    update("UPDATE `bans` SET id='" + ban.getId() + "' WHERE player='" + player + "' AND type='" + type + "';");
                } else {
                    update("UPDATE `bans` SET id='" + id + "' WHERE player='" + player + "' AND type='" + type
                            + "' AND creationdate='" + rs.getString("creationdate") + "';");
                }
            }
        }
    }
}
