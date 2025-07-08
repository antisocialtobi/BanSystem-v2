package net.coalcube.bansystem.spigot.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompleteUtil {

    public static List<String> completePlayerNames(CommandSender sender, String[] args) {
        final List<String> result = new ArrayList<>();
        if (args.length > 0) {
            final String start = args[(args.length - 1)];
            for (final Player p : Bukkit.getOnlinePlayers()) {
                final String lowercase = p.getName().toLowerCase();
                if (lowercase.startsWith(start)) {
                    result.add(p.getName());
                }
            }
        } else {
            for (final Player p : Bukkit.getOnlinePlayers()) {
                result.add(p.getName());
            }
        }
        return result;
    }

}