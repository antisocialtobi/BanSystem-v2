package net.coalcube.bansystem.core.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;

public class UUIDFetcher {

    /**
     * Date when name changes were introduced
     *
     * @see UUIDFetcher#getUUIDAt(String, long)
     */
    public static final long FEBRUARY_2015 = 1422748800000L;

    private static Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d";
    private static final String NAME_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

    private static Map<String, UUID> uuidCache = new HashMap<>();
    private static Map<UUID, String> nameCache = new HashMap<>();

    private static ExecutorService pool = Executors.newCachedThreadPool();

    private String name;
    private UUID id;

    /**
     * Fetches the uuid asynchronously and passes it to the consumer
     *
     * @param name   The name
     * @param action Do what you want to do with the uuid her
     */
    public static void getUUID(String name, Consumer<UUID> action) {
        UUIDFetcher.pool.execute(() -> action.accept(UUIDFetcher.getUUID(name)));
    }

    /**
     * Fetches the uuid synchronously and returns it
     *
     * @param name The name
     * @return The uuid
     */
    public static UUID getUUID(String name) {
        return UUIDFetcher.getUUIDAt(name, System.currentTimeMillis());
    }

    /**
     * Fetches the uuid synchronously for a specified name and time and passes the
     * result to the consumer
     *
     * @param name      The name
     * @param timestamp Time when the player had this name in milliseconds
     * @param action    Do what you want to do with the uuid her
     */
    public static void getUUIDAt(String name, long timestamp, Consumer<UUID> action) {
        UUIDFetcher.pool.execute(() -> action.accept(UUIDFetcher.getUUIDAt(name, timestamp)));
    }

    /**
     * Fetches the uuid synchronously for a specified name and time
     *
     * @param name      The name
     * @param timestamp Time when the player had this name in milliseconds
     * @see UUIDFetcher#FEBRUARY_2015
     */
    public static UUID getUUIDAt(String name, long timestamp) {
        name = name.toLowerCase();
        if (UUIDFetcher.uuidCache.containsKey(name)) {
            return UUIDFetcher.uuidCache.get(name);
        }
        try {
            final HttpURLConnection connection = (HttpURLConnection) new URL(
                    String.format(UUIDFetcher.UUID_URL, name, timestamp / 1000)).openConnection();
            connection.setReadTimeout(5000);
            final UUIDFetcher data = UUIDFetcher.gson.fromJson(
                    new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDFetcher.class);

            UUIDFetcher.uuidCache.put(name, data.id);
            UUIDFetcher.nameCache.put(data.id, data.name);

            return data.id;
        } catch (final Exception e) {
        }
        return null;
    }

    /**
     * Fetches the name asynchronously and passes it to the consumer
     *
     * @param uuid   The uuid
     * @param action Do what you want to do with the name her
     */
    public static void getName(UUID uuid, Consumer<String> action) {
        UUIDFetcher.pool.execute(() -> {
            action.accept(UUIDFetcher.getName(uuid));
        });
    }

    /**
     * Fetches the name synchronously and returns it
     *
     * @param uuid The uuid
     * @return The name
     */
    public static String getName(UUID uuid) {

        if(nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = readJsonFromUrl(NAME_URL.replaceAll("%s", UUIDTypeAdapter.fromUUID(uuid)));
        } catch (IOException e) {
            return null;
        }
        if(jsonObject != null && jsonObject.has("name")) {
            String name = jsonObject.getString("name");
            nameCache.put(uuid, name);
            return name;
        }

        return null;

//        if (UUIDFetcher.nameCache.containsKey(uuid)) {
//            return UUIDFetcher.nameCache.get(uuid);
//        }
//        try {
//            final HttpURLConnection connection = (HttpURLConnection) new URL(
//                    String.format(UUIDFetcher.NAME_URL, UUIDTypeAdapter.fromUUID(uuid))).openConnection();
//            connection.setReadTimeout(5000);
//            final UUIDFetcher[] nameHistory = UUIDFetcher.gson.fromJson(
//                    new BufferedReader(new InputStreamReader(connection.getInputStream())), UUIDFetcher[].class);
//            if(nameHistory == null)
//                return null;
//            final UUIDFetcher currentNameData = nameHistory[nameHistory.length - 1];
//
//            UUIDFetcher.uuidCache.put(currentNameData.name.toLowerCase(), uuid);
//            UUIDFetcher.nameCache.put(uuid, currentNameData.name);
//
//            return currentNameData.name;
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }

    }

    public static void clearCache() {
        nameCache.clear();
        uuidCache.clear();
    }

    private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            if(jsonText == null)
                return null;
            JSONObject jsonObject = new JSONObject(jsonText);
            return jsonObject;
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        if(sb.length() == 0){
            return null;
        }
        return sb.toString();
    }
}