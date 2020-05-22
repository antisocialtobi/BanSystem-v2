package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MySQL {

    private String host, database, username, password;
    private int port;

    private Connection con;

    public MySQL(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() throws SQLException {
        con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true", username, password);
    }

    public ResultSet getResult(String s) throws SQLException {
        Statement stmt = con.createStatement();

        return stmt.executeQuery(s);
    }

    public void update(String qry) throws SQLException {
        PreparedStatement preparedStatement = con.prepareStatement(qry);
        preparedStatement.execute();
    }

    public void createTables(Config config) throws SQLException {
        update("CREATE TABLE IF NOT EXISTS `bans` " +
                "( `player` VARCHAR(36) NOT NULL ," +
                " `duration` DOUBLE NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ," +
                " `creator` VARCHAR(36) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `ip` VARCHAR(100) NOT NULL ," +
                " `type` VARCHAR(20) NOT NULL )" +
                " ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `banhistories` " +
                "( `player` VARCHAR(36) NOT NULL ," +
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

        update("CREATE TABLE IF NOT EXISTS `web_accounts` " +
                "( `user` VARCHAR(100) NOT NULL ," +
                " `password` VARCHAR(200) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ) ENGINE = InnoDB;");

        update("CREATE TABLE IF NOT EXISTS `kicks` " +
                "( `player` VARCHAR(100) NOT NULL ," +
                " `creator` VARCHAR(100) NOT NULL ," +
                " `reason` VARCHAR(100) NOT NULL ," +
                " `creationdate` DATETIME NOT NULL ) ENGINE = InnoDB;");

        if(!config.getBoolean("needReason.Unban") && !config.getBoolean("needReason.Unmute")) {
            update("CREATE TABLE IF NOT EXISTS `unbans` " +
                    "( `player` VARCHAR(36) NOT NULL ," +
                    " `unbanner` VARCHAR(36) NOT NULL ," +
                    " `creationdate` DATETIME NOT NULL ," +
                    " `type` VARCHAR(20) NOT NULL )" +
                    " ENGINE = InnoDB;");
        } else {
            update("CREATE TABLE IF NOT EXISTS `unbans` " +
                    "( `player` VARCHAR(36) NOT NULL ," +
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
    }

    public void syncIDs(Config config) throws SQLException {
        for(String id : config.getSection("IDs").getKeys()) {
            boolean isIDexists = isIDexists(id);
            if(!isIDexists) {
                for(String lvl :  config.getSection("IDs." + id + ".lvl").getKeys()) {
                    update("INSERT INTO ids " +
                            "VALUES ('" + id + "', '" +
                            config.getString("IDs."+ id + ".reason") + "', '" +
                            lvl + "', '" +
                            config.getLong("IDs." + id + ".lvl." + lvl + ".duration") + "', " +
                            config.getBoolean("IDs."+ id + ".onlyAdmins") + ", '" +
                            config.getString("IDs."+ id + ".lvl." + lvl + ".type") + "', NOW(), 'configsync');");
                }
            }

            for(String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
                if(!isLvlSync(id, lvl, config)) {

                }
            }

            if(!isIDsync(id, config) || !isIDexists) {
                if(isIDexists && isIDfromConfig(id)) {
                    update("DELETE FROM ids WHERE id='" + id + "';");
                }
                for(String lvl :  config.getSection("IDs." + id + ".lvl").getKeys()) {
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

    private boolean isIDsync(String id, Config config) throws SQLException {
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

    private boolean isLvlSync(String id, String lvl, Config config) throws SQLException {
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


    private boolean isIDexists(String id) throws SQLException {
        ResultSet rs = getResult("SELECT * FROM `ids` WHERE id='" + id + "';");
        while (rs.next()) {
            return true;
        }
        return false;
    }

    private boolean isIDfromConfig(String id) throws SQLException {
        ResultSet resultSet = getResult("SELECT `creator` FROM `ids` WHERE id='" + id + "' AND NOT creator='configsync';");

        while (resultSet.next()) {
            return false;
        }
        return true;
    }

    private boolean hasUnbanreason() throws SQLException {

        ResultSet rs = getResult("SHOW COLUMNS FROM `unbans` WHERE Field='reason';");

        while (rs.next()) {
            return true;
        }
        return false;
    }

    public void disconnect() throws SQLException {
        con.close();
        con = null;
    }

    public boolean isConnected() {
        return (con != null);
    }
}
