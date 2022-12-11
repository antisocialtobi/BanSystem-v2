package net.coalcube.bansystem.bungee.listener;

import net.coalcube.bansystem.bungee.BanSystemBungee;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ChatListener implements Listener {

    private HashMap<UUID, ScheduledTask> cooldownTask = new HashMap<UUID, ScheduledTask>();
    private HashMap<UUID, Long> reamingTime = new HashMap<>();

    private final BanManager banManager;
    private final Config config;
    private final Config blacklist;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    public ChatListener(BanManager banManager, Config config, Database sql, Config blacklist, ConfigurationUtil configurationUtil) {
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.blacklist = blacklist;
        this.configurationUtil = configurationUtil;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
        ProxiedPlayer p = (ProxiedPlayer) e.getSender();
        UUID uuid = p.getUniqueId();
        String msg = e.getMessage();
        boolean startsWithBlockedCommnad = false;

        if (config.getBoolean("mysql.enable") && !sql.isConnected()) {
            p.sendMessage(configurationUtil.getMessage("NoDBConnection"));
            return;
        }
        for (String s : config.getStringList("mute.blockedCommands")) {
            if (msg.startsWith(s) || msg.contains(s) || msg.equalsIgnoreCase(s)) {
                startsWithBlockedCommnad = true;
                break;
            }
        }
        if (startsWithBlockedCommnad || !msg.startsWith("/")) {
            try {
                if (banManager.isBanned(uuid, Type.CHAT)) {
                    if (banManager.getEnd(uuid, Type.CHAT) > System.currentTimeMillis()
                            || banManager.getEnd(uuid, Type.CHAT) == -1) {
                        e.setCancelled(true);
                        p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                .replaceAll("%reason%", banManager.getReason(uuid, Type.CHAT))
                                .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                        .getFormattedRemainingTime(banManager.getRemainingTime(uuid, Type.CHAT))));
                    } else {
                        if (config.getBoolean("needReason.Unmute")) {
                            banManager.unMute(uuid, ProxyServer.getInstance().getConsole().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unMute(uuid, ProxyServer.getInstance().getConsole().getName());
                        }

                        banManager.log("Unmuted Player", ProxyServer.getInstance().getConsole().getName(), uuid.toString(), "Autounmute");

                        ProxyServer.getInstance().getConsole().sendMessage(configurationUtil.getMessage("Ban.Chat.autounmute.success")
                                .replaceAll("%player%", p.getDisplayName()));
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                            if (all.hasPermission("system.ban")) {
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
        if(!p.hasPermission("bansys.bypasschatfilter") && !e.isCancelled()) {
            if (config.getBoolean("blacklist.words.enable")) {
                if (hasBlockedWordsContains(msg)) {

                    e.setCancelled(true);
                    if (config.getBoolean("blacklist.words.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.words.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (!isMaxBanLvl(id, banManager.getLevel(uuid, reason)))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = getMaxLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", ProxyServer.getInstance().getConsole().getName(), uuid.toString(), "Autoban, Type: " + type + ", Chatmessage: " + msg);

                        if (type.equals(Type.NETWORK)) {
                            String banscreen = BanSystem.getInstance().getBanScreen();
                            banscreen = banscreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                            banscreen = banscreen.replaceAll("%reason%", reason);
                            banscreen = banscreen.replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(duration));
                            banscreen = banscreen.replaceAll("%creator%", BanSystem.getInstance().getConsole().getName());
                            banscreen = banscreen.replaceAll("%enddate%", enddate);
                            banscreen = banscreen.replaceAll("%lvl%", String.valueOf(lvl));
                            banscreen = banscreen.replaceAll("&", "§");

                            p.disconnect(banscreen);
                        } else {
                            p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(lvl)));
                        }

                        BanSystem.getInstance().sendConsoleMessage(
                                configurationUtil.getMessage("blacklist.notify.words.autoban")
                                        .replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance()
                                                .getTimeFormatUtil().getFormattedRemainingTime(duration)));

                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
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
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
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
                if (hasAdContains(msg)) {
                    e.setCancelled(true);
                    if (config.getBoolean("blacklist.ads.autoban.enable")) {
                        String id = String.valueOf(config.getInt("blacklist.ads.autoban.id"));
                        String reason = config.getString("IDs." + id + ".reason");
                        int lvl;
                        if (!isMaxBanLvl(id, banManager.getLevel(uuid, reason)))
                            lvl = banManager.getLevel(uuid, reason) + 1;
                        else
                            lvl = getMaxLvl(id);
                        Long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                        if(duration != -1) duration = duration * 1000;
                        Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));

                        banManager.ban(uuid, duration, BanSystem.getInstance().getConsole().getName(), type, reason);

                        banManager.log("Banned Player", ProxyServer.getInstance().getConsole().getName(),
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

                            p.disconnect(banscreen);
                        } else {
                            p.sendMessage(configurationUtil.getMessage("Ban.Chat.Screen")
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                            .getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(lvl)));
                        }
                        BanSystem.getInstance().sendConsoleMessage(
                                configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                        .replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration)));
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all != p)) {
                                all.sendMessage(configurationUtil.getMessage("blacklist.notify.ads.autoban")
                                        .replaceAll("%player%", p.getDisplayName())
                                        .replaceAll("%message%", msg)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration)));
                            }
                        }
                    } else {
                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
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
                String humanReadableReamingTime = BanSystem.getInstance().getTimeFormatUtil()
                        .getFormattedRemainingTime(tmpReamingTime);

                e.setCancelled(true);
                p.sendMessage(configurationUtil.getMessage("chatdelay")
                        .replaceAll("%reamingtime%", humanReadableReamingTime));
            } else {
                reamingTime.put(uuid, System.currentTimeMillis() + config.getInt("chatdelay.delay") * 1000);
                cooldownTask.put(uuid, ProxyServer.getInstance().getScheduler().schedule(BanSystemBungee.getInstance(), () -> {
                    cooldownTask.remove(uuid);
                    reamingTime.remove(uuid);
                }, config.getInt("chatdelay.delay"), TimeUnit.SECONDS));
            }
        }
    }

    private boolean hasBlockedWordsContains(String message) {
        message = message.trim();
        message = message.replaceAll("AE", "Ä");
        message = message.replaceAll("OE", "Ö");
        message = message.replaceAll("UE", "Ü");
        message = message.replaceAll("Ä", "AE");
        message = message.replaceAll("Ö", "OE");
        message = message.replaceAll("Ü", "UE");
        message = message.replaceAll("Punkt", ".");
        message = message.replaceAll("Point", ".");
        message = message.replaceAll("0", "O");
        message = message.replaceAll("1", "I");
        message = message.replaceAll("3", "E");
        message = message.replaceAll("4", "A");
        message = message.replaceAll("5", "S");
        message = message.replaceAll("8", "B");
        String[] trimmed = message.split(" ");

        for (String word : blacklist.getStringList("Words")) {
            if (message.contains(word) || message.equalsIgnoreCase(word) || message.toUpperCase().equals(word) || message.toLowerCase().equals(word))
                return true;

            for(String pice : trimmed) {
                if(pice.equalsIgnoreCase(word))
                    return true;
            }
        }
        return false;
    }

    private boolean hasAdContains(String message) {
        message = message.trim();
        message = message.replaceAll("0", "O");
        message = message.replaceAll("1", "I");
        message = message.replaceAll("3", "E");
        message = message.replaceAll("4", "A");
        message = message.replaceAll("5", "S");
        message = message.replaceAll("8", "B");
        message = message.replaceAll("AE", "Ä");
        message = message.replaceAll("OE", "Ö");
        message = message.replaceAll("UE", "Ü");
        message = message.replaceAll("Ä", "AE");
        message = message.replaceAll("Ö", "OE");
        message = message.replaceAll("Ü", "UE");
        message = message.replaceAll("Punkt", ".");
        message = message.replaceAll("Point", ".");

        String[] trimmed = message.split(" ");

        for(String ad : blacklist.getStringList("Ads")) {
            if(message.contains(ad) || message.equalsIgnoreCase(ad) || message.toUpperCase().equals(ad) || message.toLowerCase().equals(ad))
                return true;

            for(String pice : trimmed) {
                if(pice.equalsIgnoreCase(ad))
                    return true;
            }
        }
        return false;
    }
    private boolean isMaxBanLvl(String id, int lvl) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.parseInt(key) > maxLvl) maxLvl = Integer.parseInt(key);
        }
        return lvl >= maxLvl;
    }

    private int getMaxLvl(String id) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.parseInt(key) > maxLvl) maxLvl = Integer.parseInt(key);
        }
        return maxLvl;
    }

}
