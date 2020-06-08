package net.coalcube.bansystem.bungee.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.BanManager;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.MySQL;
import net.coalcube.bansystem.core.util.Type;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.sql.SQLException;

public class ChatListener implements Listener {

    private final BanManager banManager;
    private final Config config;
    private final Config messages;
    private final MySQL mySQL;

    public ChatListener(BanManager banManager, Config config, Config messages, MySQL mySQL) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
        this.mySQL = mySQL;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(ChatEvent e) {
        if(mySQL.isConnected()) {
            ProxiedPlayer p = (ProxiedPlayer) e.getSender();
            String msg = e.getMessage();
            boolean startsWithBlockedCommnad = false;

            for(String s : config.getStringList("mute.blockedCommands")) {
                if (msg.startsWith(s) || msg.contains(s) || msg.equalsIgnoreCase(s)) {
                    startsWithBlockedCommnad = true;
                    break;
                }
            }
            if(startsWithBlockedCommnad || !msg.startsWith("/")) {
                try {
                    if(banManager.isBanned(p.getUniqueId(), Type.CHAT)) {
                        if(banManager.getEnd(p.getUniqueId(), Type.CHAT) > System.currentTimeMillis()
                                || banManager.getEnd(p.getUniqueId(), Type.CHAT) == -1) {
                            e.setCancelled(true);
                            for (String message : messages.getStringList("Ban.Chat.Screen")) {
                                p.sendMessage(message
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%reason%", banManager.getReason(p.getUniqueId(), Type.CHAT))
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(banManager.getRemainingTime(p.getUniqueId(), Type.CHAT))
                                                .replaceAll("&", "§")));
                            }
                        } else {
                            if(config.getBoolean("needReason.Unmute")) {
                                banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                            } else {
                                banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName());
                            }

                            ProxyServer.getInstance().getConsole().sendMessage(messages.getString("Ban.Chat.autounmute")
                                    .replaceAll("%P%", messages.getString("%prefix%"))
                                    .replaceAll("%player%", p.getDisplayName())
                                    .replaceAll("&", "§"));
                            for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                if(all.hasPermission("system.ban")) {
                                    all.sendMessage(messages.getString("Ban.Chat.autounmute")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("&", "§"));
                                }
                            }
                        }
                    }
                } catch (SQLException | IOException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
    }
}
