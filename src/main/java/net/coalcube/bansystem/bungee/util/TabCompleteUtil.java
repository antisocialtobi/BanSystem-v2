package net.coalcube.bansystem.bungee.util;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TabCompleteUtil {

    public static List<String> completePlayerNames(CommandSender sender, String[] args) {
        final List<String> result = new ArrayList<>();
        if (args.length > 0) {
            final String start = args[(args.length - 1)];
            for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                final String lowercase = p.getName().toLowerCase();
                if (lowercase.startsWith(start)) {
                    result.add(p.getName());
                }
            }
        } else {
            for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                result.add(p.getName());
            }
        }
        return result;
    }

}