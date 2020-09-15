package net.coalcube.bansystem.core.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public interface Database {

    void connect() throws SQLException;
    void disconnect() throws SQLException;
    void update(String qry) throws SQLException;
    ResultSet getResult(String qry) throws SQLException, ExecutionException, InterruptedException;
    void createTables(Config config) throws SQLException, ExecutionException, InterruptedException;
    boolean isConnected();

}
