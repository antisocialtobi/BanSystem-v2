package net.coalcube.bansystem.core.util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class IDManager {

    private final Config config;
    private final Database database;
    private final File configFile;

    public IDManager(Config config, Database database, File configfile) {
        this.config = config;
        this.database = database;
        this.configFile = configfile;
    }

    public void createID(String id, String reason, boolean onlyAdmin, long duration, Type type, String creator) throws SQLException, IOException {
        config.set("IDs." + id + ".reason", reason);
        config.set("IDs." + id + ".onlyAdmins", onlyAdmin);
        config.set("IDs." + id + ".lvl.1.type", type.toString());
        config.set("IDs." + id + ".lvl.1.duration", duration);
        config.save(configFile);
        if(isMySQLused())
            database.update("INSERT INTO `ids` (`id`, `reason`, `lvl`, `duration`, `onlyadmin`, `type`, `creationdate`, `creator`) " +
                "VALUES ('" + id + "', '" + reason + "', '1', '" + duration + "', '" + onlyAdmin + "', '" + type + "', NOW(), '" + creator + "');");
    }

    public void deleteID(String id) throws SQLException, IOException {
        config.set("IDs." + id, null);
        config.save(configFile);
        if(isMySQLused())
            database.update("DELETE FROM `ids` WHERE id='" + id + "';");
    }

    public void addLvl(String id, long duration, Type type, String creator) throws IOException, SQLException {
        int lvl = getHighestLvl(id) + 1;

        config.set("IDs." + id + ".lvl." + lvl + ".type", type);
        config.set("IDs." + id + ".lvl." + lvl + ".duration", duration);
        config.save(configFile);
        if(isMySQLused()) {
            database.update("INSERT INTO `ids` (`id`, `reason`, `lvl`, `duration`, `onlyadmin`, `type`, `creationdate`, `creator`) " +
                    "VALUES ('" + id + "', '" + getReason(id) + "', '" + lvl + "', '" + duration + "', " +
                    "'" + getOnlyAdmins(id) + "', '" + type + "', NOW(), '" + creator + "')");
        }
    }

    public void removeLvl(String id, String lvl) throws IOException, SQLException {
        config.set("IDs." + id + ".lvl." + lvl, null);
        config.save(configFile);
        if(isMySQLused())
            database.update("DELETE FROM `ids` WHERE id='" + id + "' AND lvl='" + lvl + "';");
    }

    public void setLvlDuration(String id, String lvl, long duration) throws IOException, SQLException {
        config.set("IDs." + id + ".lvl." + lvl + ".duration", duration);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET duration='" + duration + "' WHERE id='" + id + "' AND lvl='" + lvl + "'");
    }

    public void setLvlType(String id, String lvl, Type type) throws IOException, SQLException {
        config.set("IDs." + id + ".lvl." + lvl + ".type", type);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET type='" + type + "' WHERE id='" + id + "' AND lvl='" + lvl + "'");
    }

    public void setOnlyAdmins(String id, boolean onlyAdmins) throws IOException, SQLException {
        config.set("IDs." + id + ".onlyAdmins", onlyAdmins);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET onlyadmin='" + onlyAdmins + "' WHERE id='" + id + "'");
    }

    public String getReason(String id) {
        return config.getString("IDs." + id + ".reason");
    }

    public boolean getOnlyAdmins(String id) {
        return config.getBoolean("IDs." + id + ".onlyAdmins");
    }

    public Type getType(String id, String lvl) {
        return Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
    }

    public long getDuration(String id, String lvl) {
        return config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
    }

    public boolean existsID(String id) {
        return config.getSection("IDs").getKeys().contains(id);
    }

    public boolean existsLvl(String id, String lvl) {
        return config.getSection("IDs." + id + ".lvl").getKeys().contains(lvl);
    }

    private boolean isMySQLused() {
        return config.getBoolean("mysql.enable");
    }

    private int getHighestLvl(String id) {
        int count = 0;
        for (String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
            count++;
        }
        return count;
    }
}
