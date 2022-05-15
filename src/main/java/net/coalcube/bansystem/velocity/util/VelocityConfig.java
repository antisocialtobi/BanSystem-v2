package net.coalcube.bansystem.velocity.util;

import net.coalcube.bansystem.core.util.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VelocityConfig implements Config {

    public VelocityConfig(com.velocitypowered.api.plugin.annotation.DataDirectory)
    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public boolean getBoolean(String key) {
        return false;
    }

    @Override
    public Config getSection(String key) {
        return null;
    }

    @Override
    public List<String> getKeys() {
        return null;
    }

    @Override
    public long getLong(String key) {
        return 0;
    }

    @Override
    public List<String> getStringList(String key) {
        return null;
    }

    @Override
    public void set(String key, Object o) {

    }

    @Override
    public void save(File f) throws IOException {

    }

    @Override
    public int getInt(String key) {
        return 0;
    }
}
