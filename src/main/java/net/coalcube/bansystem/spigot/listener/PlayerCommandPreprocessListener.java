package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.ban.Type;
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
    private static YamlDocument config;
    private static List<String> blockedCommands;
    private static ConfigurationUtil configurationUtil;

    public PlayerCommandPreprocessListener(BanManager banManager, YamlDocument config, List<String> blockedCommands, ConfigurationUtil configurationUtil) {
        PlayerCommandPreprocessListener.banManager = banManager;
        PlayerCommandPreprocessListener.config = config;
        PlayerCommandPreprocessListener.blockedCommands = blockedCommands;
        PlayerCommandPreprocessListener.configurationUtil = configurationUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if(!BanSystem.getInstance().getSQL().isConnected()) {
            try {
                BanSystem.getInstance().getSQL().connect();
            } catch (SQLException ex) {
                return;
            }
        }
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
                Ban mute = banManager.getBan(p.getUniqueId(), Type.CHAT);
                if(mute != null) {
                    if(mute.getEnd() > System.currentTimeMillis()
                            || mute.getEnd() == -1) {
                        if (startsWithBlockedCommnad) {
                            e.setCancelled(true);

                            String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                    mute.getRemainingTime());

                            p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", mute.getReason())
                                    .replaceAll("%reamingtime%", reamingTime));
                        }
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unBan(p.getUniqueId(), Bukkit.getConsoleSender().getName(), Type.CHAT, "Strafe abgelaufen");
                        } else {
                            banManager.unBan(p.getUniqueId(), Bukkit.getConsoleSender().getName(), Type.CHAT);
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
            } catch (SQLException | IOException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
