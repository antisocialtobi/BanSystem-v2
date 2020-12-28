package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PlayerConnectionListener implements Listener {

    private final BanManager banManager;
    private final Config config;
    private final Config messages;
    private final String banScreenRow;
    private final Plugin instance;

    public PlayerConnectionListener(BanManager banManager, Config config, Config messages, String banScreen, Plugin instance) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
        this.banScreenRow = banScreen;
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(PlayerPreLoginEvent e) {
        boolean isCancelled = false;
        UUID uuid = e.getUniqueId();

        if (BanSystem.getInstance().getSQL().isConnected()) {
            try {
                if (banManager.isBanned(e.getUniqueId(), Type.NETWORK)) {
                    if (banManager.getEnd(e.getUniqueId(), Type.NETWORK) > System.currentTimeMillis()
                            || banManager.getEnd(e.getUniqueId(), Type.NETWORK) == -1) {
                        // disallow connecting when user is banned

                        String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(e.getUniqueId(), Type.NETWORK));

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                        String enddate = simpleDateFormat.format(new Date(banManager.getEnd(e.getUniqueId(), Type.NETWORK)));

                        String banScreen = banScreenRow
                                .replaceAll("%reason%", banManager.getReason(e.getUniqueId(), Type.NETWORK))
                                .replaceAll("%creator", banManager.getBanner(e.getUniqueId(), Type.NETWORK))
                                .replaceAll("%enddate%", enddate)
                                .replaceAll("%reamingtime%", reamingTime)
                                .replaceAll("&", "§")
                                .replaceAll("%lvl%", String.valueOf(banManager.getLevel(e.getUniqueId(), banManager.getReason(e.getUniqueId(), Type.NETWORK))));
                        if (!config.getBoolean("Ban.KickDelay.enable")) e.disallow(Result.KICK_BANNED, banScreen);
                        isCancelled = true;

                        if (!banManager.isSetIP(e.getUniqueId())) {
                            banManager.setIP(e.getUniqueId(), e.getAddress());
                        }
                    } else {
                        // autounban
                        try {
                            if (config.getBoolean("needReason.Unban")) {
                                banManager.unBan(e.getUniqueId(), Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                            } else {
                                banManager.unBan(e.getUniqueId(), Bukkit.getConsoleSender().getName());
                            }
                            banManager.log("Unbanned Player", Bukkit.getConsoleSender().getName(), e.getUniqueId().toString(), "Autounban");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        Bukkit.getConsoleSender()
                                .sendMessage(messages.getString("Ban.Network.autounban")
                                        .replaceAll("%P%", BanSystemSpigot.prefix).replaceAll("%player%", e.getName())
                                        .replaceAll("&", "§"));
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (all.hasPermission("bansys.notify")) {
                                all.sendMessage(messages.getString("Ban.Network.autounban")
                                        .replaceAll("%P%", BanSystemSpigot.prefix).replaceAll("%player%", e.getName())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                }
            } catch (SQLException | ParseException | UnknownHostException throwables) {
                throwables.printStackTrace();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            } catch (ExecutionException executionException) {
                executionException.printStackTrace();
            }
            if (!isCancelled) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(instance, new Runnable() {

                    @Override
                    public void run() {
                        if (config.getBoolean("VPN.enable")) {
                            if (URLUtil.isVPN(e.getAddress().getAddress().toString().replaceAll("/", ""))) {
                                if (config.getBoolean("VPN.autoban.enable")) {

                                    int lvl = 0;
                                    String id = config.getString("VPN.autoban.ID");
                                    String reason = config.getString("IDs." + id + ".reason");
                                    try {
                                        if (banManager.hasHistory(e.getUniqueId(), reason)) {
                                            if (!isMaxBanLvl(id, banManager.getLevel(e.getUniqueId(), reason))) {
                                                lvl = (byte) (banManager.getLevel(e.getUniqueId(), reason))+1;
                                            } else {
                                                lvl = getMaxLvl(id);
                                            }
                                        } else {
                                            lvl = 1;
                                        }
                                    } catch (UnknownHostException | SQLException unknownHostException) {
                                        unknownHostException.printStackTrace();
                                    } catch (InterruptedException interruptedException) {
                                        interruptedException.printStackTrace();
                                    } catch (ExecutionException executionException) {
                                        executionException.printStackTrace();
                                    }
                                    long time = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                                    Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));


                                    try {
                                        banManager.ban(e.getUniqueId(), time, Bukkit.getConsoleSender().getName(),
                                                type, reason, e.getAddress());
                                        banManager.log("Banned Player", Bukkit.getConsoleSender().getName(), e.getUniqueId().toString(), "VPN Autoban");
                                    } catch (IOException | SQLException ioException) {
                                        ioException.printStackTrace();
                                    }
                                } else {
                                    for (Player all : Bukkit.getOnlinePlayers()) {
                                        all.sendMessage(messages.getString("VPN.warning")
                                                .replaceAll("%P%", BanSystemSpigot.prefix)
                                                .replaceAll("%player%", e.getName()).replaceAll("&", "§"));
                                    }
                                }
                            }
                        }

                        // seccond accounts

                    }
                }, 20 * 1);
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        try {
            if (banManager.isBanned(p.getUniqueId(), Type.NETWORK)) {
                e.setQuitMessage(null);
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (p.hasPermission("bansys.ban.admin")) {
            try {
                if (new UpdateChecker(65863).checkForUpdates()) {

                    p.sendMessage(BanSystemSpigot.prefix + "§cEin neues Update ist verfügbar.");

                    TextComponent comp = new TextComponent();
                    comp.setText(BanSystemSpigot.prefix
                            + "§7Lade es dir unter §ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
                    comp.setClickEvent(new ClickEvent(Action.OPEN_URL,
                            "https://www.spigotmc.org/resources/bansystem-mit-ids.65863/"));
                    comp.setHoverEvent(new HoverEvent(
                            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("Klicke um zur Webseite zu gelangen").create()));

                    p.spigot().sendMessage(comp);
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        try {
            if (banManager.isBanned(p.getUniqueId(), Type.NETWORK)) {
                e.setJoinMessage(null);
                new BukkitRunnable() {

                    @Override
                    public void run() {

                        String reamingTime = null;
                        try {
                            reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(p.getUniqueId(), Type.NETWORK));

                            String banScreen = banScreenRow
                                    .replaceAll("%Reason%", banManager.getReason(p.getUniqueId(), Type.NETWORK))
                                    .replaceAll("%ReamingTime%", reamingTime).replaceAll("&", "§");
                            p.kickPlayer(banScreen);
                        } catch (SQLException | ParseException throwables) {
                            throwables.printStackTrace();
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        } catch (ExecutionException executionException) {
                            executionException.printStackTrace();
                        }

                    }
                }.runTaskLater(BanSystemSpigot.getPlugin(), 20 * config.getInt("Ban.KickDelay.inSecconds"));
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }

        try {
            if (!banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress()).isEmpty() &&
                    !p.hasPermission("bansys.ban") && !banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress()).contains(p.getUniqueId())) {
                String bannedPlayerName = "";
                boolean rightType = true;
                List<UUID> banned;
                int ipAutoBanID = config.getInt("IPautoban.banid");
                String ipAutoBanReason = config.getString("IDs." + ipAutoBanID + ".reason");
                int ipAutoBanLvl = 0;

                try {
                    if (!isMaxBanLvl(String.valueOf(ipAutoBanID), banManager.getLevel(uuid, ipAutoBanReason))) {
                        ipAutoBanLvl = banManager.getLevel(uuid, ipAutoBanReason) + 1;
                    } else
                        ipAutoBanLvl = getMaxLvl(String.valueOf(ipAutoBanID));


                    banned = banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress());
                    for (UUID id : banned) {
                        if (banManager.isBanned(p.getUniqueId(), Type.CHAT))
                            rightType = false;
                        if (bannedPlayerName.length() == 0) {
                            bannedPlayerName = UUIDFetcher.getName(id);
                        } else {
                            bannedPlayerName += ", " + UUIDFetcher.getName(id);
                        }
                    }
                } catch (SQLException | UnknownHostException throwables) {
                    throwables.printStackTrace();
                }

                long ipAutoBanDuration = config.getLong("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".duration");
                Type ipAutoBanType = Type.valueOf(config.getString("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".type"));

                if (!rightType && config.getBoolean("IPautoban.onlyNetworkBans")) {
                    return;
                }
                if (config.getBoolean("IPautoban.enable")) {
                    try {
                        banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType, ipAutoBanReason, p.getAddress().getAddress());
                        banManager.log("Banned Player", Bukkit.getConsoleSender().getName(), uuid.toString(), "Same IP Autoban");
                    } catch (IOException | SQLException ioException) {
                        ioException.printStackTrace();
                    }
                    Bukkit.getConsoleSender()
                            .sendMessage(messages.getString("autoban.ip.notify") + bannedPlayerName + " §cwurde automatisch gebannt für §e"
                                    + config.getString("IDs." + config.getInt("IPautoban.banid") + ".reason")
                                    + "§c.");
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(messages.getString("ip.autoban")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%bannedaccount%", bannedPlayerName)
                                    .replaceAll("&", "§")
                                    .replaceAll("%reason%", ipAutoBanReason));
                        }
                    }
                    String banScreen = BanSystem.getInstance().getBanScreen();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                    String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + ipAutoBanDuration));
                    try {
                        banScreen = banScreen.replaceAll("%reason%", banManager.getReason(uuid, Type.NETWORK));
                        banScreen = banScreen.replaceAll("%reamingtime%",
                                        BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                                banManager.getRemainingTime(uuid, Type.NETWORK)));
                        banScreen = banScreen.replaceAll("%creator", Bukkit.getConsoleSender().getName());
                        banScreen = banScreen.replaceAll("%enddate%", enddate);
                        banScreen = banScreen.replaceAll("%lvl%", String.valueOf(ipAutoBanLvl));
                    } catch (SQLException | ParseException throwables) {
                        throwables.printStackTrace();
                    }
                    p.kickPlayer(banScreen);
                } else {
                    Bukkit.getConsoleSender()
                            .sendMessage(messages.getString("prefix") + "§e" + p.getDisplayName()
                                    + " §cist womöglich ein 2. Account von §e" + bannedPlayerName);
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(messages.getString("prefix") + "§e" + p.getDisplayName()
                                    + " §cist womöglich ein 2. Account von §e" + bannedPlayerName);
                        }
                    }
                }
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }

    }

    private boolean isMaxBanLvl(String id, int lvl) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.valueOf(key) > maxLvl) {
                maxLvl = Integer.valueOf(key);
            }
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