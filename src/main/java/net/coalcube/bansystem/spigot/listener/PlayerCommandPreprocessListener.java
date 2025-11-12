package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlayerCommandPreprocessListener implements Listener {

    private final BanManager banManager;
    private final YamlDocument config;
    private final List<String> blockedCommands;
    private final ConfigurationUtil configurationUtil;

    public PlayerCommandPreprocessListener(BanManager banManager, YamlDocument config, List<String> blockedCommands, ConfigurationUtil configurationUtil) {
        this.banManager = banManager;
        this.config = config;
        this.blockedCommands = blockedCommands;
        this.configurationUtil = configurationUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        if (!BanSystem.getInstance().getSQL().isConnected()) {
            try {
                BanSystem.getInstance().getSQL().connect();
            } catch (SQLException ex) {
                return;
            }
        }
        if (BanSystem.getInstance().getSQL().isConnected()) {
            Player p = e.getPlayer();
            String msg = e.getMessage();
            boolean startsWithBlockedCommnad = false;

            for (Object s : blockedCommands) {
                if (msg.startsWith(s.toString())) {
                    startsWithBlockedCommnad = true;
                }
            }
            try {
                Ban mute = banManager.getBan(p.getUniqueId(), Type.CHAT);
                if (mute != null) {
                    if (mute.getEnd() > System.currentTimeMillis()
                            || mute.getEnd() == -1) {
                        if (startsWithBlockedCommnad) {
                            e.setCancelled(true);

                            String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                    mute.getRemainingTime());

                            p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", mute.getReason())
                                    .replaceAll("%reamingtime%", reamingTime)
                                    .replaceAll("%id%", mute.getId()));
                        }
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unBan(mute, Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unBan(mute, Bukkit.getConsoleSender().getName());
                        }
                        banManager.log("Unmuted Player", Bukkit.getConsoleSender().getName(), p.getUniqueId().toString(),
                                "Autounmute; banID: " + mute.getId());
                        Bukkit.getConsoleSender().sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                .replaceAll("%player%", p.getDisplayName()));
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (all.hasPermission("bansys.notify")) {
                                all.sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                        .replaceAll("%player%", p.getDisplayName()));

                            }
                        }
                    }
                }
            } catch (SQLException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
        }
    }
}
