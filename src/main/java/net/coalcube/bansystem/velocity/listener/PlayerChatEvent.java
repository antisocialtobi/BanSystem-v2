package net.coalcube.bansystem.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PlayerChatEvent {
    private HashMap<UUID, ScheduledTask> cooldownTask = new HashMap<UUID, ScheduledTask>();
    private HashMap<UUID, Long> reamingTime = new HashMap<>();

    private final ProxyServer server;
    private final BanManager banManager;
    private final YamlDocument config;
    private final BlacklistUtil blacklistUtil;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    public PlayerChatEvent(ProxyServer server, BanManager banManager, YamlDocument config, Database sql, BlacklistUtil blacklistUtil, ConfigurationUtil configurationUtil) {
        this.server = server;
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.blacklistUtil = blacklistUtil;
        this.configurationUtil = configurationUtil;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerChat(com.velocitypowered.api.event.player.PlayerChatEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        String msg = e.getMessage();
        boolean startsWithBlockedCommnad = false;

        if (config.getBoolean("mysql.enable") && !sql.isConnected()) {
            try {
                sql.connect();
            } catch (SQLException ex) {
                p.sendMessage(Component.text(configurationUtil.getMessage("NoDBConnection")));
                return;
            }
        }
        for (String s : config.getStringList("mute.blockedCommands")) {
            if (msg.startsWith(s) || msg.contains(s) || msg.equalsIgnoreCase(s)) {
                startsWithBlockedCommnad = true;
                break;
            }
        }
        if (startsWithBlockedCommnad || !msg.startsWith("/")) {
            try {
                Ban mute = banManager.getBan(uuid, Type.CHAT);
                if (mute != null) {
                    if (mute.getEnd() > System.currentTimeMillis()
                            || mute.getEnd() == -1) {
                        e.setResult(com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult.denied());
                        p.sendMessage(Component.text(configurationUtil.getMessage("Ban.Chat.Screen")
                                .replaceAll("%reason%", mute.getReason())
                                .replaceAll("%id%", mute.getId())
                                .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                        .getFormattedRemainingTime(mute.getRemainingTime()))));
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unBan(mute, "CONSOLE", "Strafe abgelaufen");
                        } else {
                            banManager.unBan(mute, "CONSOLE");
                        }

                        banManager.log("Unmuted Player", "CONSOLE", uuid.toString(), "Autounmute");

                        server.getConsoleCommandSource().sendMessage(Component.text(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                .replaceAll("%player%", p.getUsername())));
                        for (Player all : server.getAllPlayers()) {
                            if (all.hasPermission("system.ban")) {
                                all.sendMessage(Component.text(
                                        configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                        .replaceAll("%player%", p.getUsername())));
                            }
                        }
                    }
                }
            } catch (SQLException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
        }
        if(!p.hasPermission("bansys.bypasschatfilter") && e.getResult().isAllowed()) {
            if (config.getBoolean("blacklist.words.enable")) {
                if ((msg.startsWith("/") && config.getBoolean("blacklist.words.checkcommands.enable") &&
                        blacklistUtil.hasBlockedWordsContains(msg)) || (!msg.startsWith("/") &&
                        blacklistUtil.hasBlockedWordsContains(msg))) {

                    e.setResult(com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult.denied());

                    if (config.getBoolean("blacklist.words.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.words.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (!banManager.isMaxBanLvl(id, banManager.getLevel(uuid, reason)))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = banManager.getMaxLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        Ban ban = banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", "CONSOLE", uuid.toString(), "Autoban, Type: " + type + ", Chatmessage: " + msg);

                        if (type.equals(Type.NETWORK)) {
                            String banscreen = BanSystem.getInstance().getBanScreen();
                            banscreen = banscreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                            banscreen = banscreen.replaceAll("%reason%", reason);
                            banscreen = banscreen.replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(duration));
                            banscreen = banscreen.replaceAll("%creator%", BanSystem.getInstance().getConsole().getName());
                            banscreen = banscreen.replaceAll("%enddate%", enddate);
                            banscreen = banscreen.replaceAll("%lvl%", String.valueOf(lvl));
                            banscreen = banscreen.replaceAll("&", "§");
                            banscreen = banscreen.replaceAll("%id%", ban.getId());

                            p.disconnect(Component.text(banscreen));
                        } else {
                            p.sendMessage(Component.text(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%id%", ban.getId())
                                    .replaceAll("%lvl%", String.valueOf(lvl))));
                        }

                        BanSystem.getInstance().sendConsoleMessage(
                                configurationUtil.getMessage("blacklist.notify.words.autoban")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason
                                        .replaceAll("%id%", ban.getId())
                                        .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                .getTimeFormatUtil().getFormattedRemainingTime(duration))));

                        for (Player all : server.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != p)) {
                                all.sendMessage(Component.text(configurationUtil.getMessage("blacklist.notify.words.autoban")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                .getTimeFormatUtil().getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId())));
                            }
                        }
                    } else {
                        for (Player all : server.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != p)) {
                                all.sendMessage(Component.text(configurationUtil.getMessage("blacklist.notify.words.warning")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)));
                            }
                        }
                    }
                }
            }
            if (config.getBoolean("blacklist.ads.enable")) {
                if (blacklistUtil.hasAdContains(msg)) {
                    e.setResult(com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult.denied());
                    if (config.getBoolean("blacklist.ads.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.ads.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (!banManager.isMaxBanLvl(id, banManager.getLevel(uuid, reason)))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = banManager.getMaxLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        Ban ban = banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", "CONSOLE",
                                uuid.toString(), "Autoban, Type: " + type + ", Chatmessage: " + msg);

                        if (type.equals(Type.NETWORK)) {
                            String banscreen = BanSystem.getInstance().getBanScreen();
                            banscreen = banscreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                            banscreen = banscreen.replaceAll("%reason%", reason);
                            banscreen = banscreen.replaceAll("%reamingtime%", BanSystem.getInstance()
                                    .getTimeFormatUtil().getFormattedRemainingTime(duration));
                            banscreen = banscreen.replaceAll("%creator%", BanSystem.getInstance().getConsole().getName());
                            banscreen = banscreen.replaceAll("%enddate%", enddate);
                            banscreen = banscreen.replaceAll("%lvl%", String.valueOf(lvl));
                            banscreen = banscreen.replaceAll("&", "§");
                            banscreen = banscreen.replaceAll("%id%", ban.getId());

                            p.disconnect(Component.text(banscreen));
                        } else {
                            p.sendMessage(Component.text(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                            .getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(lvl))
                                    .replaceAll("%id%", ban.getId())));
                        }
                        BanSystem.getInstance().sendConsoleMessage(
                                configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId()));
                        for (Player all : server.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != p)) {
                                all.sendMessage(Component.text(configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId())));
                            }
                        }
                    } else {
                        for (Player all : server.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != p)) {
                                all.sendMessage(Component.text(configurationUtil.getMessage("blacklist.notify.ads.warning")
                                        .replaceAll("%player%", p.getUsername())
                                        .replaceAll("%message%", msg)));
                            }
                        }
                    }
                }
            }
        }
        if(e.getResult().isAllowed() && config.getBoolean("chatdelay.enable")
                && !p.hasPermission("bansys.bypasschatdelay")
                && !msg.startsWith("/")) {
            if(cooldownTask.containsKey(uuid)) {
                long tmpReamingTime = reamingTime.get(uuid);
                tmpReamingTime = tmpReamingTime - System.currentTimeMillis();
                if(tmpReamingTime < 0) {
                    tmpReamingTime = 0;
                }
                String humanReadableReamingTime = BanSystem.getInstance().getTimeFormatUtil()
                        .getFormattedRemainingTime(tmpReamingTime);

                e.setResult(com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult.denied());
                p.sendMessage(Component.text(configurationUtil.getMessage("chatdelay")
                        .replaceAll("%reamingtime%", humanReadableReamingTime)));
            } else {
                reamingTime.put(uuid, System.currentTimeMillis() + config.getInt("chatdelay.delay") * 1000);
                cooldownTask.put(uuid, server.getScheduler().buildTask(BanSystem.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        cooldownTask.remove(uuid);
                        reamingTime.remove(uuid);
                    }
                }).delay(config.getInt("chatdelay.delay"), TimeUnit.SECONDS).schedule());
            }
        }
    }
}
