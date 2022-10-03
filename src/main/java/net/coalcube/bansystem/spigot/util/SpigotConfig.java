package net.coalcube.bansystem.spigot.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import net.coalcube.bansystem.core.util.Config;

public class SpigotConfig implements Config {

    private final FileConfiguration config;

    public SpigotConfig(FileConfiguration config) {
        this.config = config;
    }

    @Override
    public String getString(String key) {
        return config.getString(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    @Override
    public Config getSection(String key) {
        return new SpigotConfigurationSection(config.getConfigurationSection(key));
    }

    @Override
    public List<String> getKeys() {
        List<String> res = new ArrayList<>(config.getKeys(false));
        return res;
    }

    @Override
    public long getLong(String key) {
        return config.getLong(key);
    }

    @Override
    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }

    @Override
    public void set(String key, Object o) {
        config.set(key, o);
    }

    @Override
    public void save(File f) throws IOException {
        config.save(f);
    }

    @Override
    public int getInt(String key) {
        return config.getInt(key);
    }

    @Override
    public Object get(String path) {
        return config.get(path);
    }

}