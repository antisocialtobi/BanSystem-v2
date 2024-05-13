package net.coalcube.bansystem.velocity.util;

import net.coalcube.bansystem.core.util.Config;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VelocityConfig implements Config {

    private final File CONFIG_FILE;
    private Yaml yaml;
    private Map<String, Object> yamldata;
    private String sectionPath;

    public VelocityConfig(File configFile) {
        this.CONFIG_FILE = configFile;
        yaml = new Yaml();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        yamldata = yaml.load(inputStream);
    }

    public VelocityConfig(File configFile, String sectionPath) {
        this.CONFIG_FILE = configFile;
        yaml = new Yaml();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        yamldata = yaml.load(inputStream);
    }
    @Override
    public String getString(String key) {
        return (String) getParameterValue(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return (Boolean) getParameterValue(key);
    }

    @Override
    public Config getSection(String key) {


        return new VelocityConfig(CONFIG_FILE, key);
    }

    @Override
    public List<String> getKeys() {
        String[] keys = sectionPath.split("\\."); // Split the path by dot
        Map<?, ?> section = yamldata;
        for (String key : keys) {
            if (section instanceof Map) {
                section = ((Map<?, ?>) section);
            } else {
                System.out.println("Key not found or reached a non-map value");
                return null;
            }
        }
        System.out.println("getKeys() section: " + section);
        return (List<String>) new ArrayList<>(section.keySet());
    }

    @Override
    public long getLong(String key) {
        return (Long) getParameterValue(key);
    }

    @Override
    public List<String> getStringList(String key) {
        return (List<String>) getParameterValue(key);
    }

    @Override
    public void set(String key, Object o) {
        try {
            setParameterValue(key, o);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void save(File f) throws IOException {
        // Not necessary
    }

    @Override
    public int getInt(String key) {
        return (int) getParameterValue(key);
    }

    @Override
    public Object get(String path) {
        return getParameterValue(path);
    }

    private Object getParameterValue(String path) {
        System.out.println("String path: " + path);
        String[] keys = path.split("\\."); // Split the path by dot
        int i = 1;
        Object value = yamldata;
        for (String key : keys) {
            System.out.println("key " + i + ": " + key);
            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(key);
            } else {
                System.out.println("Key not found or reached a non-map value. \n");
                return null;
            }
            i++;
        }
        System.out.println("value: " + value + "\n");
        return value;
    }

    private void setParameterValue(String path, Object value) throws IOException {
        // Set parameter by path
        String[] keys = path.split("\\.");
        for (int i = 0; i < keys.length - 1; i++) {
            if (!yamldata.containsKey(keys[i]) || !(yamldata.get(keys[i]) instanceof Map)) {
                yamldata.put(keys[i], new LinkedHashMap<String, Object>());
            }
            yamldata = (Map<String, Object>) yamldata.get(keys[i]);
        }
        yamldata.put(keys[keys.length - 1], value);

        // Write updated content back to the file
        Writer writer = new FileWriter(CONFIG_FILE);
        yaml.dump(yamldata, writer);
        writer.close();
    }
}
