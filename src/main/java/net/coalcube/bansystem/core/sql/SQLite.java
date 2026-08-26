package net.coalcube.bansystem.core.sql;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;

import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SQLite implements Database {

    private final File database;
    private Connection con;

    public SQLite(File database) {
        this.database = database;
    }

    @Override
    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:" + database.getPath());
        } catch (ClassNotFoundException e) {
            System.err.println("Fehler beim Laden des JDBC-Treibers");
            e.printStackTrace();
        }

    }

    @Override
    public void disconnect() throws SQLException {
        con.close();
        con = null;
    }

    @Override
    public void update(String qry) throws SQLException {
        PreparedStatement preparedStatement = con.prepareStatement(qry);
        preparedStatement.execute();
    }

    @Override
    public ResultSet getResult(String qry) throws SQLException {
        Statement stmt = con.createStatement();

        return stmt.executeQuery(qry);
    }

    @Override
    public void createTables(YamlDocument config) throws SQLException {
        update("CREATE TABLE IF NOT EXISTS `bans` " +
                "( `id` VARCHAR(16) ," +
                " `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL );");

        update("CREATE TABLE IF NOT EXISTS `banhistories` " +
                "( `id` VARCHAR(16) NOT NULL , " +
                " `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL );");

//        update("CREATE TABLE IF NOT EXISTS `ids` " +
//                "( `id` INT NOT NULL ," +
//                " `reason` VARCHAR(100) NOT NULL ," +
//                " `lvl` INT NOT NULL ," +
//                " `duration` DOUBLE NOT NULL ," +
//                " `onlyadmin` BOOLEAN NOT NULL ," +
//                " `type` VARCHAR(100) NOT NULL ," +
//                " `creationdate` DATETIME NOT NULL ," +
//                " `creator` VARCHAR(100) NOT NULL );");

//        update("CREATE TABLE IF NOT EXISTS `web_accounts` " +
//                "( `user` VARCHAR(100) NOT NULL ," +
//                " `password` VARCHAR(200) NOT NULL ," +
//                " `creationdate` DATETIME NOT NULL );");

        update("CREATE TABLE IF NOT EXISTS `kicks` " +
                "( `player` VARCHAR(100) NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL );");

        update("CREATE TABLE IF NOT EXISTS `logs` " +
                "( `id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                " `action` VARCHAR(100) NOT NULL ," +
                " `target` VARCHAR(100) NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ," +
                " `note` VARCHAR(500) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL);");

        update("CREATE TABLE IF NOT EXISTS `bedrockplayer` " +
                "( `username` VARCHAR(64) NOT NULL ," +
                " `uuid` VARCHAR(64) NOT NULL );");

        update("CREATE TABLE IF NOT EXISTS `unbans` " +
                "(`id` VARCHAR(16) NOT NULL ," +
                " `player` VARCHAR(36) NOT NULL ," +
                " `unbanner` VARCHAR(36) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `reason` VARCHAR(1000) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL );");
    }
//
//    private boolean hasUnbanreason() throws SQLException {
//        ResultSet rs = getResult("PRAGMA table_info('unbans');");
//
//        while (rs.next()) {
//            String name = rs.getString("name");
//            if(name.equals("reason")) {
//                return true;
//            }
//        }
//        return false;
//    }

    @Override
    public boolean isConnected() {
        return (con != null);
    }

    @Override
    public void updateTables() throws SQLException, ExecutionException, InterruptedException {
        BanSystem banSystem = BanSystem.getInstance();
        String prefix = banSystem.getConfigurationUtil().getMessage("prefix");

        boolean banIDs = false;
        boolean banHistoryIDs = false;
        boolean unbansIDs = false;
        boolean unbanReason = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        BanManager banManager = BanSystem.getInstance().getBanManager();

        ResultSet rs_bans = getResult("select name from pragma_table_info('bans') WHERE name='id';");
        while (rs_bans.next()) {
            banIDs = true;
        }

        ResultSet rs_banhistories = getResult("select name from pragma_table_info('banhistories') WHERE name='id';");
        while (rs_banhistories.next()) {
            banHistoryIDs = true;
        }

        ResultSet rs_unbans = getResult("select name from pragma_table_info('unbans') WHERE name='id';");
        while (rs_unbans.next()) {
            unbansIDs = true;
        }

        ResultSet rs_unbansreason = getResult("select name from pragma_table_info('unbans') WHERE name='reason';");
        while (rs_unbansreason.next()) {
            unbanReason = true;
        }


        if (!banHistoryIDs) {
            update("ALTER TABLE `banhistories` ADD COLUMN `id` VARCHAR(16);");
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §ebanhistories §7wurde geupdated.");
        }

        if (!banIDs) {
            update("ALTER TABLE `bans` ADD COLUMN `id` VARCHAR(16);");
            ResultSet rs = getResult("SELECT * FROM `bans`;");

            while (rs.next()) {
                String id = banManager.generateNewID();
                String player = rs.getString("player");
                Type type = Type.valueOf(rs.getString("type"));

                update("UPDATE `bans` SET id='" + id + "' WHERE player='" + player + "' AND type='" + type + "';");
            }
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §ebans §7wurde geupdated.");
        }

        if (!unbansIDs) {
            update("ALTER TABLE `unbans` ADD COLUMN `id` VARCHAR(16);");
            ResultSet rs = getResult("SELECT * FROM `unbans`;");

            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player"));

                update("UPDATE `bans` SET id='" + banManager.generateNewID() + "' WHERE player='" + player
                        + "' AND creationdate='" + rs.getTimestamp("creationdate") + "';");
            }
            banSystem.sendConsoleMessage(prefix + "§7Tabelle §eunbans §7wurde geupdated.");
        }

        if (!banHistoryIDs) {
            ResultSet rs = getResult("SELECT * FROM `banhistories`;");

            while (rs.next()) {
                UUID player = UUID.fromString(rs.getString("player"));
                Type type = Type.valueOf(rs.getString("type"));
                String creationDate = rs.getString("creationDate");
                Ban ban = banManager.getBan(player, type);
                String id = banManager.generateNewID();

                if (ban != null && Objects.equals(creationDate, dateFormat.format(ban.getCreationdate()))) {
                    update("UPDATE `banhistories` SET id='" + ban.getId() + "' WHERE player='" + player
                            + "' AND type='" + type + "';");
                } else {
                    update("UPDATE `banhistories` SET id='" + id + "' WHERE player='" + player
                            + "' AND type='" + type + "' AND creationdate='" + rs.getString("creationdate") + "';");
                }
            }
        }
        if (!unbanReason) {
            update("ALTER TABLE `unbans` ADD COLUMN `reason` VARCHAR(1000);");
        }
    }
}
