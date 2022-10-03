package net.coalcube.bansystem.bungee.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.coalcube.bansystem.core.util.Config;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;


public class BungeeConfig implements Config {

    private final Configuration config;

    public BungeeConfig(Configuration config) {
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
        return new BungeeConfig(config.getSection(key));
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(config.getKeys());
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
        ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, f);
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
