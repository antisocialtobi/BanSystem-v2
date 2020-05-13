package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.BanManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.coalcube.bansystem.core.util.Type;
import net.coalcube.bansystem.spigot.BanSystemSpigot;

import java.io.IOException;

public class ChatListener implements Listener {

    private static BanManager bm = BanSystemSpigot.getBanmanager();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if(BanSystemSpigot.mysql.isConnected()) {
            Player p = e.getPlayer();
            String msg = e.getMessage();
            if(msg.startsWith("/msg") || !msg.startsWith("/")) {
                if(bm.isBanned(p.getUniqueId(), Type.CHAT)) {
                    if(bm.getEnd(p.getUniqueId(), Type.CHAT) > System.currentTimeMillis() || bm.getEnd(p.getUniqueId(), Type.CHAT) == -1) {
                        e.setCancelled(true);
                        for (String message : BanSystemSpigot.messages.getStringList("Ban.Chat.Screen")) {
                            p.sendMessage(message.replaceAll("%P%", BanSystemSpigot.PREFIX).replaceAll("%reason%", bm.getReason(p.getUniqueId(), Type.CHAT))
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(p.getUniqueId(), Type.CHAT)))
                                    .replaceAll("&", "§"));
                        }
                    } else {
                        try {
                            bm.unmute(p.getUniqueId(), BanSystem.getInstance().getConsole().getName());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        Bukkit.getConsoleSender().sendMessage(BanSystemSpigot.messages.getString("Ban.Chat.autounmute").replaceAll("%P%", BanSystemSpigot.PREFIX).replaceAll("%player%", p.getDisplayName()).replaceAll("&", "§"));
                        for(Player all : Bukkit.getOnlinePlayers()) {
                            if(all.hasPermission("system.ban")) {
                                all.sendMessage(BanSystemSpigot.messages.getString("Ban.Chat.autounmute")
                                        .replaceAll("%P%", BanSystemSpigot.PREFIX)
                                        .replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                }
            }
        }
    }
}