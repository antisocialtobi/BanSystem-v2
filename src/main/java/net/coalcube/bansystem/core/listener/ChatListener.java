package net.coalcube.bansystem.core.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import org.bstats.charts.SimplePie;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class ChatListener {

    private final BanSystem bansystem;
    private final BanManager banManager;
    private final ConfigurationUtil configurationUtil;
    private final YamlDocument config;
    private final Database sql;
    private final BlacklistUtil blacklistUtil;
    private final IDManager idManager;
    private final MetricsAdapter metricsAdapter;
    private final boolean chatDelayEnabled;
    private final int chatDelay;

    private List<String> blockedCommands;
    private HashMap<UUID, Long> chatDelayedPlayer;


    public ChatListener(BanSystem bansystem,
                        BanManager banManager,
                        ConfigurationUtil configurationUtil,
                        Database sql,
                        BlacklistUtil blacklistUtil,
                        IDManager idManager) {
        this.bansystem = bansystem;
        this.banManager = banManager;
        this.configurationUtil = configurationUtil;
        this.sql = sql;
        this.blacklistUtil = blacklistUtil;
        this.idManager = idManager;
        this.metricsAdapter = bansystem.getMetricsAdapter();

        config = configurationUtil.getConfig();
        blockedCommands = new ArrayList<>();
        chatDelayedPlayer = new HashMap<>();
        chatDelay = config.getInt("chatdelay.delay");
        chatDelayEnabled = config.getBoolean("chatdelay.enable");

        blockedCommands.addAll(config.getStringList("mute.blockedCommands"));
    }

    public Event onChat(User sender, String message) throws SQLException, IOException, ExecutionException, InterruptedException {
        Event event = new Event();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
        UUID uuid = sender.getUniqueId();
        boolean startsWithBlockedCommnad = false;

        // Check SQL connection
        if (!sql.isConnected()) {
            sender.sendMessage(configurationUtil.getMessage("NoDBConnection"));
            return event;
        }

        // Check if the message starts with a blocked command
        for (String s : blockedCommands) {
            if (message.startsWith(s) || message.contains(s) || message.equalsIgnoreCase(s)) {
                startsWithBlockedCommnad = true;
                break;
            }
        }


        if (startsWithBlockedCommnad || !message.startsWith("/")) {
                Ban ban = banManager.getBan(uuid, Type.CHAT);
                if (ban != null) {
                    if (ban.getEnd() > System.currentTimeMillis()
                            || ban.getEnd() == -1) {
                        event.setCancelled(true);
                        sender.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                .replaceAll("%reason%", ban.getReason())
                                .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                        .getFormattedRemainingTime(ban.getRemainingTime()))
                                .replaceAll("%id%", ban.getId()));
                        bansystem.getConsole().sendMessage("§8[§c§lMUTED§r§8] §f" + sender.getDisplayName() + "§f: " + message );
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unBan(ban, bansystem.getConsole().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unBan(ban, bansystem.getConsole().getName());
                        }

                        banManager.log("Unmuted Player", bansystem.getConsole().getName(), uuid.toString(), "Autounmute; banID: " + ban.getId());

                        bansystem.getConsole().sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                .replaceAll("%player%", sender.getDisplayName()));
                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("system.ban")) {
                                all.sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                        .replaceAll("%player%", sender.getDisplayName()));
                            }
                        }
                    }
                }
        }
        if(!sender.hasPermission("bansys.bypasschatfilter") && !event.isCancelled()) {
            if (config.getBoolean("blacklist.words.enable")) {
                if ((message.startsWith("/") && config.getBoolean("blacklist.words.checkcommands.enable") &&
                        blacklistUtil.hasBlockedWordsContains(message)) || (!message.startsWith("/") &&
                        blacklistUtil.hasBlockedWordsContains(message))) {

                    event.setCancelled(true);

                    if (config.getBoolean("blacklist.words.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.words.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (idManager.getLastLvl(id) < banManager.getLevel(uuid, reason))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = idManager.getLastLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        Ban ban = banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", bansystem.getConsole().getName(), uuid.toString(),
                                "Autoban; banID: " + ban.getId() + "; " +
                                     "reason: " + ban.getReason() + "; " +
                                     "Type: " + type + "; " +
                                     "duration: " + ban.getDuration() + "; " +
                                     "Chatmessage: " + message);

                        metricsAdapter.addCustomChart(new SimplePie("automations", () -> {
                            return "Blacklist mute word";
                        }));

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

                            sender.disconnect(banscreen);
                        } else {
                            sender.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(lvl))
                                    .replaceAll("%id%", ban.getId()));
                        }

                        BanSystem.getInstance().sendConsoleMessage(
                                configurationUtil.getMessage("blacklist.notify.words.autoban")
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                .getTimeFormatUtil().getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId()));

                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != sender)) {
                                all.sendMessage(configurationUtil.getMessage("blacklist.notify.words.autoban")
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                .getTimeFormatUtil().getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId()));
                            }
                        }
                    } else {
                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != sender)) {
                                all.sendMessage(configurationUtil.getMessage("blacklist.notify.words.warning")
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message));
                            }
                        }
                    }
                }
            }
            if (config.getBoolean("blacklist.ads.enable")) {
                if (blacklistUtil.hasAdContains(message)) {
                    event.setCancelled(true);
                    if (config.getBoolean("blacklist.ads.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.ads.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (banManager.getLevel(uuid, reason) < idManager.getLastLvl(id))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = idManager.getLastLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        Ban ban = banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", bansystem.getConsole().getName(), uuid.toString(),
                                "Autoban; banID: " + ban.getId() + "; " +
                                        "reason: " + ban.getReason() + "; " +
                                        "Type: " + type + "; " +
                                        "duration: " + ban.getDuration() + "; " +
                                        "Chatmessage: " + message);

                        metricsAdapter.addCustomChart(new SimplePie("automations", () -> {
                            return "Blacklist mute ad";
                        }));

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

                            sender.disconnect(banscreen);
                        } else {
                            sender.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
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
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId()));
                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != sender)) {
                                all.sendMessage(configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%id%", ban.getId()));
                            }
                        }
                    } else {
                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != sender)) {
                                all.sendMessage(configurationUtil.getMessage("blacklist.notify.ads.warning")
                                        .replaceAll("%player%", sender.getDisplayName())
                                        .replaceAll("%message%", message));
                            }
                        }
                    }
                }
            }
        }

        // Chat cooldown
        if(!event.isCancelled() && chatDelayEnabled
                && !sender.hasPermission("bansys.bypasschatdelay")
                && !message.startsWith("/")) {
            if(chatDelayedPlayer.containsKey(uuid)) {
                long tmpReamingTime = chatDelayedPlayer.get(uuid);
                tmpReamingTime = tmpReamingTime - System.currentTimeMillis();
                if(tmpReamingTime <= 0) {
                    chatDelayedPlayer.remove(uuid);

                } else {
                    String humanReadableReamingTime = BanSystem.getInstance().getTimeFormatUtil()
                            .getFormattedRemainingTime(tmpReamingTime);

                    event.setCancelled(true);
                    sender.sendMessage(configurationUtil.getMessage("chatdelay")
                            .replaceAll("%reamingtime%", humanReadableReamingTime));
                }
            } else {
                chatDelayedPlayer.put(uuid, System.currentTimeMillis() + chatDelay * 1000L);
            }
        }
        return event;
    }
}
