package net.coalcube.bansystem.core.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Database {

    void connect() throws SQLException;
    void disconnect() throws SQLException;
    void update(String qry) throws SQLException;
    ResultSet getResult(String qry) throws SQLException;
    void createTables(Config config) throws SQLException;
    boolean isConnected();

}
