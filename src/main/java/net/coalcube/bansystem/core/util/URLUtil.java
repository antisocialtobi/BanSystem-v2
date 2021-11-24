package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class URLUtil {

    private final  Config messages;
    private final Config config;

    public URLUtil(Config messages, Config config) {
        this.messages = messages;
        this.config = config;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    public boolean isVPN(String ip) throws IOException {
        if (!ip.equals("127.0.0.1") || !ip.equals(BanSystem.getInstance().getConfiguration().getString("VPN.serverIP"))) {
            JSONObject jsonObject;
            if(config.getString("VPNCheck.key").isEmpty()) {
                jsonObject = readJsonFromUrl("https://vpnapi.io/api/" + ip);
            } else {
                jsonObject = readJsonFromUrl("https://vpnapi.io/api/" + ip + "?key=" + config.getString("VPNCheck.key"));
            }

            if(jsonObject.has("security")) {
                JSONObject structure = (JSONObject) jsonObject.get("security");
                return Boolean.getBoolean(structure.get("vpn").toString());
            } else {
                BanSystem.getInstance().getConsole().sendMessage(messages.getString("prefix")
                                + "§cBei der VPN Abfrage ist ein Fehler aufgetreten: "
                                + jsonObject.getString("message"));
            }
            return false;
        }
        return false;
    }
}
