package net.coalcube.bansystem.core.sql;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.util.Config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

public interface Database {

    void connect() throws SQLException;
    void disconnect() throws SQLException;
    void update(String qry) throws SQLException;
    ResultSet getResult(String qry) throws SQLException, ExecutionException, InterruptedException;
    void createTables(YamlDocument config) throws SQLException, ExecutionException, InterruptedException;
    boolean isConnected();
     void updateTables() throws SQLException, ExecutionException, InterruptedException;
}
