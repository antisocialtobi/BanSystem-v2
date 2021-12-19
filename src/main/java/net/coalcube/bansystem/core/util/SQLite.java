package net.coalcube.bansystem.core.util;

import java.io.File;
import java.sql.*;

public class SQLite implements Database {

    private Connection con;
    private final File database;

    public SQLite(File database) {
        this.database = database;
    }

    @Override
    public void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Fehler beim Laden des JDBC-Treibers");
            e.printStackTrace();
        }

        con = DriverManager.getConnection("jdbc:sqlite:" + database.getPath());
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
    public void createTables(Config config) throws SQLException {
        update("CREATE TABLE IF NOT EXISTS `bans` " +
                "( `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL );");

        update("CREATE TABLE IF NOT EXISTS `banhistories` " +
                "( `player` VARCHAR(36) NOT NULL ," +
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

        if(!config.getBoolean("needReason.Unban") && !config.getBoolean("needReason.Unmute")) {
            update("CREATE TABLE IF NOT EXISTS `unbans` " +
                    "( `player` VARCHAR(36) NOT NULL ," +
                    " `unbanner` VARCHAR(36) NOT NULL ," +
                    " `creationdate` DATETIME NOT NULL ," +
                    " `type` VARCHAR(20) NOT NULL );");
        } else {
            update("CREATE TABLE IF NOT EXISTS `unbans` " +
                    "( `player` VARCHAR(36) NOT NULL ," +
                    " `unbanner` VARCHAR(36) NOT NULL ," +
                    " `creationdate` DATETIME NOT NULL ," +
                    " `reason` VARCHAR(1000) NOT NULL ," +
                    " `type` VARCHAR(20) NOT NULL );");

            if(!hasUnbanreason()) {
                update("ALTER TABLE `unbans` \n" +
                        "ADD reason varchar(100);");
            }

        }
    }

    private boolean hasUnbanreason() throws SQLException {
        ResultSet rs = getResult("PRAGMA table_info('unbans');");

        while (rs.next()) {
            String name = rs.getString("name");
            if(name.equals("reason")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isConnected() {
        return (con != null);
    }
}
