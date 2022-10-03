package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.BanManager;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.Type;
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
    private static Config config;
    private static List<String> blockedCommands;
    private static ConfigurationUtil configurationUtil;

    public PlayerCommandPreprocessListener(BanManager banManager, Config config, List<String> blockedCommands, ConfigurationUtil configurationUtil) {
        PlayerCommandPreprocessListener.banManager = banManager;
        PlayerCommandPreprocessListener.config = config;
        PlayerCommandPreprocessListener.blockedCommands = blockedCommands;
        PlayerCommandPreprocessListener.configurationUtil = configurationUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
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

                            p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", banManager.getReason(p.getUniqueId(), Type.CHAT))
                                    .replaceAll("%reamingtime%", reamingTime));
                        }
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unMute(p.getUniqueId(), Bukkit.getConsoleSender().getName());
                        }
                        banManager.log("Unmuted Player", Bukkit.getConsoleSender().getName(), p.getUniqueId().toString(), "Autounmute");
                        Bukkit.getConsoleSender().sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                .replaceAll("%player%", p.getDisplayName()));
                        for(Player all : Bukkit.getOnlinePlayers()) {
                            if(all.hasPermission("bansys.notify")) {
                                all.sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                        .replaceAll("%player%", p.getDisplayName()));

                            }
                        }
                    }
                }
            } catch (SQLException | IOException | ParseException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
