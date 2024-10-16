package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.ban.BanManager;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;

public class StatisticServerSocket {

    private final int PORT = 8501;
    private final String HOST = "coalcube.net";

    private final BanManager banManager;

    public StatisticServerSocket(BanManager banManager) {
        this.banManager = banManager;
    }

    public void register(String environment, String serverVersion, String pluginVersion) {
        try {
            Socket socket = new Socket(HOST, PORT);
            OutputStream output = socket.getOutputStream();

            JSONObject json = new JSONObject();

            URL whatismyip = new URL("http://v4.ident.me/");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            String ip = in.readLine();

            json.put("IP", ip);
            json.put("environment", environment);
            json.put("serverVersion", serverVersion);
            json.put("pluginVersion", pluginVersion);

            output.write(json.toString().getBytes());
            output.close();
            socket.close();
        } catch (IOException e) {
            // no exception
        }
    }
}
