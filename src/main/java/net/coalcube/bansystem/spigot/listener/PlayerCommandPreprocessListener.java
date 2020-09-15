package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.BanManager;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.Type;
import net.md_5.bungee.api.ProxyServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlayerCommandPreprocessListener implements Listener {

    private static BanManager banManager;
    private static Config config, messages;
    private static List<String> blockedCommands;

    public PlayerCommandPreprocessListener(BanManager banManager, Config config, Config messages, List<String> blockedCommands) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
        this.blockedCommands = blockedCommands;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if(BanSystem.getInstance().getSQL().isConnected()) {
            Player p = e.getPlayer();
            String msg = e.getMessage();
            boolean startsWithBlockedCommnad = false;

            for(Object s : blockedCommands) {
                if(msg.startsWith(s.toString())) {
                    startsWithBlockedCommnad = true;
                }
            }
            try {
                if(banManager.isBanned(p.getUniqueId(), Type.CHAT)) {
                    if(banManager.getEnd(p.getUniqueId(), Type.CHAT) > System.currentTimeMillis()
                            || banManager.getEnd(p.getUniqueId(), Type.CHAT) == -1) {
                        if (startsWithBlockedCommnad) {
                            e.setCancelled(true);

                            String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                    banManager.getRemainingTime(p.getUniqueId(), Type.CHAT));

                            for (String message : messages.getStringList("Ban.Chat.Screen")) {
                                p.sendMessage(message
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%reason%", banManager.getReason(p.getUniqueId(), Type.CHAT))
                                        .replaceAll("%reamingtime%", reamingTime)
                                        .replaceAll("&", "§"));
                            }
                        }
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName());
                        }
                        banManager.log("Unmuted Player", ProxyServer.getInstance().getConsole().getName(), p.getUniqueId().toString(), "Autounmute");
                        Bukkit.getConsoleSender().sendMessage(messages.getString("Ban.Chat.autounmute")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("%player%", p.getDisplayName())
                                .replaceAll("&", "§"));
                        for(Player all : Bukkit.getOnlinePlayers()) {
                            if(all.hasPermission("bansys.notify")) {
                                all.sendMessage(messages.getString("Ban.Chat.autounmute")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("&", "§"));

                            }
                        }
                    }
                }
            } catch (SQLException | IOException | ParseException throwables) {
                throwables.printStackTrace();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            } catch (ExecutionException executionException) {
                executionException.printStackTrace();
            }
        }
    }
}
