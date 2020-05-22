package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

public class URLUtil {

    public static String ReadURL(String URL) {
        String re = "";
        try {
            java.net.URL url = new URL(URL);
            Reader is = new InputStreamReader(url.openStream());
            BufferedReader in = new BufferedReader(is);
            String s;
            while ((s = in.readLine()) != null) {
                re = re + " " + s;
            }
            in.close();
        } catch (MalformedURLException e) {
            System.out.println("MalformedURLException: " + e);
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
        return re;
    }

    public static boolean isVPN(String IP) {
        if (!IP.equals("127.0.0.1") || !IP.equals(BanSystem.getInstance().getConfiguration().getString("VPN.serverIP"))) {
            if (ReadURL("http://tutorialwork.epizy.com/api/checkvpn.php?ip=" + IP).equals("yes")) {
                return true;
            }
            return false;
        }
        return false;
    }
}
