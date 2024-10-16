package net.coalcube.bansystem.velocity.util;

import net.coalcube.bansystem.core.util.Config;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class VelocityConfig implements Config {

    private final File CONFIG_FILE;
    private Yaml yaml;
    private Map<String, Object> yamldata;
    private String sectionPath;
    private InputStream inputStream;

    public VelocityConfig(File configFile) {
        this.CONFIG_FILE = configFile;
        yaml = new Yaml();
        try {
            inputStream = Files.newInputStream(configFile.toPath());
            yamldata = yaml.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VelocityConfig(File configFile, String sectionPath) {
        this.CONFIG_FILE = configFile;
        this.sectionPath = sectionPath;
        yaml = new Yaml();
        try {
            inputStream = Files.newInputStream(configFile.toPath());
            yamldata = yaml.load(inputStream);
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
        List<String> sectionList = new ArrayList<>();
        int i=0;
        for (String key : keys) {
            i++;

            if(i == keys.length) {
                for(String sectionkey : ((Map<String, Object>)section.get(key)).keySet()) {
                    sectionList.add(sectionkey);
                }
            } else {
                if(section.get(key) instanceof Map) {
                    section = (Map<String, Object>) section.get(key);
                } else {
                    System.err.println("Key not found or reached a non-map value");
                    return null;
                }
            }
        }
        return sectionList;
    }

    @Override
    public long getLong(String key) {
        return Long.parseLong(Objects.requireNonNull(getParameterValue(key)).toString());
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
        String[] keys = path.split("\\.");
        Object value = null;
        int i=1;
        for(String key : keys) {
            if(i == 1) {
                value = yamldata.get(keys[0]);
            } else {
                if (value instanceof Map) {
                    value = ((Map<?, ?>) value).get(key);
                } else if (value != null && keys.length == i) {
                    // Value have
                    return value;
                } else {
                    // Value is null
                    return null;
                }
            }
            i++;
        }
        return value;
    }

    private void setParameterValue(String path, Object value) throws IOException {
        // Set parameter by path
        String[] keys = path.split("\\.");
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> tmpYamlData1 = new LinkedHashMap<>();
        Map<String, Object> tmpYamlData2 = new LinkedHashMap<>();
        Map<String, Object> lastMap = new LinkedHashMap<>();

        if(keys.length == 1) {
            root.put(path, lastMap);
        } else {
            for(String entry : yamldata.keySet()) {
                if(entry.equalsIgnoreCase(keys[0])) {
                    // path: VPN.autoban.ID value: 12
                    int i=1;
                    for(String key : keys) {
                        System.out.println("key: " + key);
                        if(i==1) {
                            lastMap = yamldata;
                        }
                        if(i==keys.length) {
                            lastMap.put(key, value);
                        } else {
                            if(lastMap.get(key) != null) {
                                lastMap = (Map<String, Object>) lastMap.get(key);
                            } else {
                                lastMap = new LinkedHashMap<>();
                            }
                        }
                        System.out.println("lastMap: " + lastMap);
                        i++;
                    }

                    String newPath = path;
                    for(int y=keys.length-1; y>0;y--) {
                        String currentKey;
                        System.out.println("y: " + y);
                        if(newPath.contains(".")) {
                            newPath = newPath.substring(0, newPath.lastIndexOf("."));
                        }
                        System.out.println("newPath: " + newPath);
                        if(newPath.contains(".")) {
                            currentKey = newPath.substring(newPath.lastIndexOf(".") + 1, newPath.length());
                        } else {
                            currentKey = newPath;
                        }

                        if(getParameterValue(newPath) == null) {
                            // bansys ids edit 1 add lvl 1123 CHAT
                            System.out.println("currentKey: " + currentKey);
                            System.out.println("keys[keys.length-2]: " + keys[keys.length-2]);
                            if(keys[keys.length-2].equalsIgnoreCase(currentKey)) {
                                tmpYamlData1 = lastMap;
                                System.out.println("tmpYamlData1 = lastMap");
                            } else {
                                System.out.println("tmpYamlData1 = new LinkedHashMap<>()");
                                tmpYamlData1 = new LinkedHashMap<>();
                            }
                        } else {
                            tmpYamlData1 = (Map<String, Object>) getParameterValue(newPath);
                        }
                        System.out.println("tmpYamlData1: " + tmpYamlData1);

                        if(!tmpYamlData2.isEmpty()) {
                            for(String keyset : tmpYamlData2.keySet()) {
                                tmpYamlData1.put(keyset, tmpYamlData2.get(keyset));
                            }
                            System.out.println("puttet tmpYamlData2 onto tmp1");
                            System.out.println("tmpYamlData1: " + tmpYamlData1);
                        }

                        if(newPath.contains(".")) {
                            tmpYamlData2 = new LinkedHashMap<>();
                            tmpYamlData2.put(currentKey, tmpYamlData1);
                            System.out.println("tmpYamlData2 path: " + currentKey);
                            System.out.println("tmpYamlData2: " + tmpYamlData2);
                        } else {
                            System.out.println("putted tmpYamlData1 to root.");
                            root.put(keys[0], tmpYamlData1);
                        }
                    }

                    //root.put(keys[0], tmpYamlData2);
                } else {
                    System.out.println("Unchanged Value set. Entry: " + entry);
                    root.put(entry, yamldata.get(entry));
                }
            }
        }

        // Write updated content back to the file
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Writer writer = new FileWriter(CONFIG_FILE);
        yaml.dump(root, writer);
        writer.close();
    }
}
