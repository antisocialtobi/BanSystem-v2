package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class AsyncPlayerChatListener implements Listener {

    private HashMap<UUID, BukkitTask> cooldownTask = new HashMap<UUID, BukkitTask>();
    private HashMap<UUID, Long> reamingTime = new HashMap<>();

    private final BanManager banManager;
    private final YamlDocument config;
    private final Database sql;
    private final BlacklistUtil blacklistUtil;
    private final ConfigurationUtil configurationUtil;

    public AsyncPlayerChatListener(YamlDocument config, BanManager banManager, Database sql, BlacklistUtil blacklistUtil, ConfigurationUtil configurationUtil) {
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.blacklistUtil = blacklistUtil;
        this.configurationUtil = configurationUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) throws IOException, SQLException, ExecutionException, InterruptedException {
        if(!sql.isConnected()) {
            try {
                sql.connect();
            } catch (SQLException ex) {
                return;
            }
        }
        if (!(config.getBoolean("mysql.enable") && !sql.isConnected())) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
            Player p = e.getPlayer();
            String msg = e.getMessage();
            UUID uuid = p.getUniqueId();
            Ban mute = banManager.getBan(uuid, Type.CHAT);
            if (mute != null) {
                if (mute.getEnd() > System.currentTimeMillis()
                        || mute.getEnd() == -1) {
                    e.setCancelled(true);
                    p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                            .replaceAll("%P%", BanSystemSpigot.prefix)
                            .replaceAll("%reason%", mute.getReason())
                            .replaceAll("%reamingtime%",
                                    BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                            mute.getRemainingTime()))
                            .replaceAll("&", "§")
                            .replaceAll("%id%", mute.getId()));
                } else {
                    try {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unBan(mute, Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unBan(mute, Bukkit.getConsoleSender().getName());
                        }
                        banManager.log("Unmuted Player", Bukkit.getConsoleSender().getName(), p.getUniqueId().toString(), "Autounmute");
                    } catch (SQLException ioException) {
                        ioException.printStackTrace();
                    }

                    Bukkit.getConsoleSender().sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                            .replaceAll("%player%", p.getDisplayName()));
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("system.ban")) {
                            all.sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                    .replaceAll("%player%", p.getDisplayName()));
                        }
                    }
                }
            }

            if (!p.hasPermission("bansys.bypasschatfilter") && mute == null) {
                if (config.getBoolean("blacklist.words.enable")) {
                    if (blacklistUtil.hasBlockedWordsContains(msg)) {
                        e.setCancelled(true);
                        if (config.getBoolean("blacklist.words.autoban.enable")) {
                            String id = String.valueOf(config.getInt("blacklist.words.autoban.id"));
                            String reason = config.getString("IDs." + id + ".reason");
                            int lvl;
                            if (!banManager.isMaxBanLvl(id, banManager.getLevel(p.getUniqueId(), reason)))
                                lvl = banManager.getLevel(p.getUniqueId(), reason) + 1;
                            else
                                lvl = banManager.getMaxLvl(id);
                            Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                            if(duration != -1) duration = duration * 1000;
                            Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                            String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                            Ban ban = banManager.ban(p.getUniqueId(), duration, BanSystem.getInstance().getConsole().getName(), type, reason);
                            banManager.log("Banned Player", Bukkit.getConsoleSender().getName(),
                                    p.getUniqueId().toString(), "Autoban, Type: " + type + ", Chatmessage: " + msg);
                            if (type.equals(Type.NETWORK)) {
                                String banscreen = BanSystem.getInstance().getBanScreen();
                                banscreen = banscreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                                banscreen = banscreen.replaceAll("%reason%", reason);
                                banscreen = banscreen.replaceAll("%reamingtime%", BanSystem.getInstance()
                                        .getTimeFormatUtil().getFormattedRemainingTime(duration));
                                banscreen = banscreen.replaceAll("%creator%", BanSystem.getInstance().getConsole().getName());
                                banscreen = banscreen.replaceAll("%enddate%", enddate);
                                banscreen = banscreen.replaceAll("%lvl%", String.valueOf(lvl));
                                banscreen = banscreen.replaceAll("%id%", ban.getId());
                                banscreen = banscreen.replaceAll("&", "§");

                                p.kickPlayer(banscreen);
                            } else {
                                p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                        .replaceAll("%enddate%", enddate)
                                        .replaceAll("%lvl%", String.valueOf(lvl))
                                        .replaceAll("%id%", ban.getId()));

                            }

                            BanSystem.getInstance().sendConsoleMessage(
                                    configurationUtil.getMessage("blacklist.notify.words.autoban")
                                    .replaceAll("%player%", p.getDisplayName())
                                    .replaceAll("%message%", msg)
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance()
                                            .getTimeFormatUtil().getFormattedRemainingTime(duration)));
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                if (all.hasPermission("bansys.notify") && (all != p)) {
                                    all.sendMessage(configurationUtil.getMessage("blacklist.notify.words.autoban")
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("%message%", msg)
                                            .replaceAll("%reason%", reason)
                                            .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                    .getTimeFormatUtil().getFormattedRemainingTime(duration)));
                                }
                            }
                        } else {
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                if (all.hasPermission("bansys.notify") && (all != p)) {
                                    all.sendMessage(configurationUtil.getMessage("blacklist.notify.words.warning")
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("%message%", msg));
                                }
                            }
                        }
                    }
                }
                if (config.getBoolean("blacklist.ads.enable")) {
                    if (blacklistUtil.hasAdContains(msg)) {
                        e.setCancelled(true);
                        if (config.getBoolean("blacklist.ads.autoban.enable")) {
                            String id = String.valueOf(config.getInt("blacklist.ads.autoban.id"));
                            String reason = config.getString("IDs." + id + ".reason");
                            int lvl;
                            if (!banManager.isMaxBanLvl(id, banManager.getLevel(p.getUniqueId(), reason)))
                                lvl = banManager.getLevel(p.getUniqueId(), reason) + 1;
                            else
                                lvl = banManager.getMaxLvl(id);
                            Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                            if(duration != -1) duration = duration * 1000;
                            Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                            String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                            Ban ban = banManager.ban(p.getUniqueId(), duration, BanSystem.getInstance().getConsole().getName(),
                                    type, reason);
                            banManager.log("Banned Player", Bukkit.getConsoleSender().getName(),
                                    p.getUniqueId().toString(), "Autoban, Type: " + type + ", Chatmessage: " + msg);
                            if (type.equals(Type.NETWORK)) {
                                String banscreen = BanSystem.getInstance().getBanScreen();
                                banscreen = banscreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                                banscreen = banscreen.replaceAll("%reason%", reason);
                                banscreen = banscreen.replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                        .getFormattedRemainingTime(duration));
                                banscreen = banscreen.replaceAll("%creator%", BanSystem.getInstance().getConsole().getName());
                                banscreen = banscreen.replaceAll("%enddate%", enddate);
                                banscreen = banscreen.replaceAll("%lvl%", String.valueOf(lvl));
                                banscreen = banscreen.replaceAll("&", "§");
                                banscreen = banscreen.replaceAll("%id%", ban.getId());

                                p.kickPlayer(banscreen);
                            } else {
                                p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                        .replaceAll("%enddate%", enddate)
                                        .replaceAll("%lvl%", String.valueOf(lvl))
                                        .replaceAll("%id%", ban.getId()));

                            }

                            BanSystem.getInstance().sendConsoleMessage(
                                    configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("%message%", msg)
                                            .replaceAll("%reason%", reason)
                                            .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                    .getTimeFormatUtil().getFormattedRemainingTime(duration))
                                            .replaceAll("%id%", ban.getId()));
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                if (all.hasPermission("bansys.notify") && (all != p)) {
                                    all.sendMessage(configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("%message%", msg)
                                            .replaceAll("%reason%", reason)
                                            .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                    .getTimeFormatUtil().getFormattedRemainingTime(duration))
                                            .replaceAll("%id%", ban.getId()));
                                }
                            }
                        } else {
                            for (Player all : Bukkit.getOnlinePlayers()) {
                                if (all.hasPermission("bansys.notify") && (all != p)) {
                                    all.sendMessage(configurationUtil.getMessage("blacklist.notify.ads.warning")
                                            .replaceAll("%player%", p.getDisplayName())
                                            .replaceAll("%message%", msg));
                                }
                            }
                        }
                    }
                }
            }
            if(!e.isCancelled() && config.getBoolean("chatdelay.enable")
                    && !p.hasPermission("bansys.bypasschatdelay")
                    && !msg.startsWith("/")) {
                if(cooldownTask.containsKey(uuid)) {
                    long tmpReamingTime = reamingTime.get(uuid);
                    tmpReamingTime = tmpReamingTime - System.currentTimeMillis();
                    if(tmpReamingTime < 0) {
                        tmpReamingTime = 0;
                    }
                    String humanReadableReamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(tmpReamingTime);

                    e.setCancelled(true);
                    p.sendMessage(configurationUtil.getMessage("chatdelay")
                            .replaceAll("%reamingtime%", humanReadableReamingTime));
                } else {
                    reamingTime.put(uuid, System.currentTimeMillis() + config.getInt("chatdelay.delay") * 1000);
                    cooldownTask.put(uuid, Bukkit.getScheduler().runTaskLaterAsynchronously(BanSystemSpigot.getPlugin(), () -> {
                        cooldownTask.remove(uuid);
                        reamingTime.remove(uuid);
                    }, config.getInt("chatdelay.delay") * 20L));
                }
            }
        }
    }
}