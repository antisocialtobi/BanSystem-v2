package net.coalcube.bansystem.core.util;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

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
                "VALUES ('" + id + "', '" + reason + "', '1', '" + duration + "', '" + onlyAdmin + "', '" + type.toString() + "', NOW(), '" + creator + "');");
    }

    public void deleteID(String id) throws SQLException, IOException {
        config.set("IDs." + id, null);
        config.save(configFile);
        if(isMySQLused())
            database.update("DELETE FROM `ids` WHERE id='" + id + "';");
    }

    public void addLvl(String id, long duration, Type type, String creator) throws IOException, SQLException {
        int lvl = getHighestLvl(id) + 1;

        config.set("IDs." + id + ".lvl." + lvl + ".type", type.toString());
        config.set("IDs." + id + ".lvl." + lvl + ".duration", duration);
        config.save(configFile);
        if(isMySQLused()) {
            database.update("INSERT INTO `ids` (`id`, `reason`, `lvl`, `duration`, `onlyadmin`, `type`, `creationdate`, `creator`) " +
                    "VALUES ('" + id + "', '" + getReason(id) + "', '" + lvl + "', '" + duration + "', " +
                    "'" + getOnlyAdmins(id) + "', '" + type.toString() + "', NOW(), '" + creator + "')");
        }
    }

    public void removeLvl(String id, String lvl) throws IOException, SQLException, ExecutionException, InterruptedException {
        config.set("IDs." + id + ".lvl." + lvl, null);
        config.save(configFile);
        if(isMySQLused())
            database.update("DELETE FROM `ids` WHERE id='" + id + "' AND lvl='" + lvl + "';");

        reassignLvls(id);
    }

    public void setLvlDuration(String id, String lvl, long duration) throws IOException, SQLException {
        config.set("IDs." + id + ".lvl." + lvl + ".duration", duration);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET duration='" + duration + "' WHERE id='" + id + "' AND lvl='" + lvl + "'");
    }

    public void setLvlType(String id, String lvl, Type type) throws IOException, SQLException {
        config.set("IDs." + id + ".lvl." + lvl + ".type", type.toString());
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET type='" + type.toString() + "' WHERE id='" + id + "' AND lvl='" + lvl + "'");
    }

    public void setOnlyAdmins(String id, boolean onlyAdmins) throws IOException, SQLException {
        config.set("IDs." + id + ".onlyAdmins", onlyAdmins);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET onlyadmin='" + onlyAdmins + "' WHERE id='" + id + "'");
    }

    public void setReason(String id, String reason) throws IOException, SQLException {
        config.set("IDs." + id + ".reason", reason);
        config.save(configFile);
        if(isMySQLused())
            database.update("UPDATE `ids` SET reason='" + reason + "' WHERE id='" + id + "'");
    }

    public void reassignLvls(String id) throws SQLException, ExecutionException, InterruptedException, IOException {

        ArrayList<String> lvls = new ArrayList();
        HashMap<String, Type> type = new HashMap();
        HashMap<String, Long> duration = new HashMap();

        for(String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
            lvls.add(lvl);
            type.put(lvl, Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type")));
            duration.put(lvl, config.getLong("IDs." + id + ".lvl." + lvl + ".duration"));
        }
        config.set("IDs." + id + ".lvl", null);

        int count = 1;

        for(String lvl : lvls) {
            Type tmpType = type.get(lvl);
            long tmpDuration = duration.get(lvl);

            config.set("IDs." + id + ".lvl." + count + ".duration", tmpDuration);
            config.set("IDs." + id + ".lvl." + count + ".type", tmpType.toString());

            count++;
        }

        config.save(configFile);

        if(isMySQLused()) {
            ResultSet rs = database.getResult("SELECT * FROM `ids` WHERE id='" + id + "' ORDER BY lvl ASC;");
            int count2 = 1;

            while (rs.next()) {
                String lvl = String.valueOf(rs.getInt("lvl"));
                String tmpReason = rs.getString("reason");
                long tmpDuration = rs.getLong("duration");
                Boolean tmpOnlyAdmins = rs.getBoolean("onlyadmin");
                Type tmpType = Type.valueOf(rs.getString("type"));
                Date tmpCreationDate = rs.getDate("creationdate");
                String tmpCreator = rs.getString("creator");

                database.update("DELETE FROM `ids` WHERE id='" + id + "' AND lvl='" + lvl + "';");
                database.update("INSERT INTO `ids` (`id`, `reason`, `lvl`, `duration`, `onlyadmin`, `type`, `creationdate`, `creator`) " +
                        "VALUES ('" + id + "', '" + tmpReason + "', '" + count2 + "', '" + tmpDuration + "', '"
                        + tmpOnlyAdmins + "', '" + tmpType.toString() + "', '" + tmpCreationDate + "', '" + tmpCreator + "');");

                count++;
            }
        }

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

    public int getLastLvl(String id) {
        int lastLvl = 0;
        for(String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if(lastLvl < Integer.valueOf(lvl))
                lastLvl = Integer.valueOf(lvl);
        }
        return lastLvl;
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
