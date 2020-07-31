package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.coalcube.bansystem.spigot.BanSystemSpigot;

import java.io.IOException;
import java.sql.SQLException;

public class AsyncPlayerChatListener implements Listener {

    private final BanManager banManager;
    private final Config config, messages;
    private final MySQL mysql;

    public AsyncPlayerChatListener(Config config, Config messages, BanManager banManager, MySQL mysql) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
        this.mysql = mysql;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!(config.getBoolean("mysql.enable") && !mysql.isConnected())) {
            Player p = e.getPlayer();
            try {
                if (banManager.isBanned(p.getUniqueId(), Type.CHAT)) {
                    if (banManager.getEnd(p.getUniqueId(), Type.CHAT) > System.currentTimeMillis()
                            || banManager.getEnd(p.getUniqueId(), Type.CHAT) == -1) {
                        e.setCancelled(true);
                        for (String message : messages.getStringList("Ban.Chat.Screen")) {
                            p.sendMessage(message.replaceAll("%P%", BanSystemSpigot.prefix)
                                    .replaceAll("%reason%", banManager.getReason(p.getUniqueId(), Type.CHAT))
                                    .replaceAll("%reamingtime%",
                                            BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(p.getUniqueId(), Type.CHAT)))
                                    .replaceAll("&", "§"));
                        }
                    } else {
                        try {
                            if(config.getBoolean("needReason.Unmute")) {
                                banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                            } else {
                                banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName());
                            }
                        } catch (IOException | SQLException ioException) {
                            ioException.printStackTrace();
                        }

                        Bukkit.getConsoleSender()
                                .sendMessage(messages.getString("Ban.Chat.autounmute")
                                        .replaceAll("%P%", BanSystemSpigot.prefix).replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("&", "§"));
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (all.hasPermission("system.ban")) {
                                all.sendMessage(messages.getString("Ban.Chat.autounmute")
                                        .replaceAll("%P%", BanSystemSpigot.prefix).replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}