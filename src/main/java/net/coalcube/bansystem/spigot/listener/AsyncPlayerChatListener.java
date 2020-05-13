package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.BanManager;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.Type;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.coalcube.bansystem.spigot.BanSystemSpigot;

import java.io.IOException;

public class AsyncPlayerChatListener implements Listener {

    private static BanManager banManager;
    private static Config config, messages;


    public AsyncPlayerChatListener(Config config, Config messages, BanManager banManager) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (BanSystemSpigot.mysql.isConnected()) {
            Player p = e.getPlayer();
            if (banManager.isBanned(p.getUniqueId(), Type.CHAT)) {
                if (banManager.getEnd(p.getUniqueId(), Type.CHAT) > System.currentTimeMillis()
                        || banManager.getEnd(p.getUniqueId(), Type.CHAT) == -1) {
                    e.setCancelled(true);
                    for (String message : messages.getStringList("Ban.Chat.Screen")) {
                        p.sendMessage(message.replaceAll("%P%", BanSystemSpigot.PREFIX)
                                .replaceAll("%reason%", banManager.getReason(p.getUniqueId(), Type.CHAT))
                                .replaceAll("%reamingtime%",
                                        BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(p.getUniqueId(), Type.CHAT)))
                                .replaceAll("&", "§"));
                    }
                } else {
                    try {
                    if(config.getBoolean("needReason.Unmute")) {
                        banManager.unmute(p.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                    } else {
                        banManager.unmute(p.getUniqueId(), Bukkit.getConsoleSender().getName());
                    }
                    } catch (IOException ioException) {


                        ioException.printStackTrace();
                    }

                    Bukkit.getConsoleSender()
                            .sendMessage(messages.getString("Ban.Chat.autounmute")
                                    .replaceAll("%P%", BanSystemSpigot.PREFIX).replaceAll("%player%", p.getDisplayName())
                                    .replaceAll("&", "§"));
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("system.ban")) {
                            all.sendMessage(messages.getString("Ban.Chat.autounmute")
                                    .replaceAll("%P%", BanSystemSpigot.PREFIX).replaceAll("%player%", p.getDisplayName())
                                    .replaceAll("&", "§"));
                        }
                    }
                }
            }
        }
    }
}