package net.coalcube.bansystem.core.util;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface Config {

    String getString(String key);

    boolean getBoolean(String key);

    Config getSection(String key);

    List<String> getKeys();

    long getLong(String key);

    List<String> getStringList(String key);

    void set(String key, Object o);

    void save(File f) throws IOException;

    int getInt(String key);

    Object get(String path);

}