package net.coalcube.bansystem.spigot.listener;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
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

import net.coalcube.bansystem.spigot.BanSystemSpigot;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PlayerConnectionListener implements Listener {

    private BanManager banManager;
    private Config config, messages;
    private String banScreenRow;
    private Plugin instance;

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

        if (BanSystem.getInstance().getSQL().isConnected()) {
            try {
                if (banManager.isBanned(e.getUniqueId(), Type.NETWORK)) {
                    if (banManager.getEnd(e.getUniqueId(), Type.NETWORK) > System.currentTimeMillis()
                            || banManager.getEnd(e.getUniqueId(), Type.NETWORK) == -1) {
                        // disallow connecting when user is banned

                        String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(e.getUniqueId(), Type.NETWORK));

                        String banScreen = banScreenRow
                                .replaceAll("%Reason%", banManager.getReason(e.getUniqueId(), Type.NETWORK))
                                .replaceAll("%ReamingTime%", reamingTime)
                                .replaceAll("&", "§");
                        if (!config.getBoolean("Ban.KickDelay.enable")) e.disallow(Result.KICK_BANNED, banScreen);
                        isCancelled = true;

                       if (banManager.isSetIP(e.getUniqueId())) {
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
            } catch (SQLException | ParseException throwables) {
                throwables.printStackTrace();
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
                                                lvl = (byte) (banManager.getLevel(e.getUniqueId(), reason) + 1);
                                            } else {
                                                lvl = (byte) banManager.getLevel(e.getUniqueId(), reason);
                                            }
                                        } else {
                                            lvl = 1;
                                        }
                                    } catch (UnknownHostException | SQLException unknownHostException) {
                                        unknownHostException.printStackTrace();
                                    }
                                    long time = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                                    Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));


                                    try {
                                        banManager.ban(e.getUniqueId(), time, Bukkit.getConsoleSender().getName(),
                                                type, reason, e.getAddress());
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
                        if (BanSystemSpigot.getBannedIPs().containsKey(e.getAddress())) {
                            String names = "";
                            boolean rightType = false;
                            ArrayList<UUID> banned = new ArrayList<>();
                            try {
                                banned.addAll(banManager.getBannedPlayersWithSameIP(e.getAddress()));
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }

                            for (UUID uuid : banned) {
                                try {
                                    if (banManager.isBanned(uuid, Type.NETWORK))
                                        rightType = true;
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                                if (names.length() == 0) {
                                    names = UUIDFetcher.getName(uuid);
                                } else {
                                    names = (names + ", " + UUIDFetcher.getName(uuid));
                                }
                            }
                            if (rightType) {
                                if (config.getBoolean("IPautoban.enable")) {


                                    int lvl = 0;
                                    String id = config.getString("IPautoban.banid");
                                    String reason = config.getString("IDs." + id + ".reason");
                                    try {
                                        if (!isMaxBanLvl(id, banManager.getLevel(e.getUniqueId(), reason)))
                                            lvl = banManager.getLevel(e.getUniqueId(), reason);
                                        else
                                            lvl = getMaxLvl(id);
                                    } catch (UnknownHostException | SQLException unknownHostException) {
                                        unknownHostException.printStackTrace();
                                    }
                                    long time = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                                    Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));


                                    try {
                                        banManager.ban(e.getUniqueId(), time, Bukkit.getConsoleSender().getName(),
                                                type, reason, e.getAddress());
                                    } catch (IOException | SQLException ioException) {
                                        ioException.printStackTrace();
                                    }


                                    Bukkit.getConsoleSender().sendMessage(BanSystemSpigot.prefix + "§cDer 2. Account von §e"
                                            + names + " §cwurde automatisch gebannt für §e"
                                            + config.getString(
                                            "IDs." + config.getInt("IPautoban.banid") + ".reason")
                                            + "§c.");
                                    for (Player all : Bukkit.getOnlinePlayers()) {
                                        if (all.hasPermission("bansys.notify")) {
                                            all.sendMessage(BanSystemSpigot.prefix + "§cDer 2. Account von §e" + names
                                                    + " §cwurde automatisch gebannt für §e"
                                                    + config.getString("IDs."
                                                    + config.getInt("IPautoban.banid") + ".reason")
                                                    + "§c.");
                                        }
                                    }
                                    String reamingTime = null;
                                    try {
                                        reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banManager.getRemainingTime(e.getUniqueId(), Type.NETWORK));
                                    } catch (SQLException | ParseException throwables) {
                                        throwables.printStackTrace();
                                    }

                                    String component = null;
                                    try {
                                        component = banScreenRow
                                                .replaceAll("%Reason%", banManager.getReason(e.getUniqueId(), Type.NETWORK))
                                                .replaceAll("%ReamingTime%", reamingTime);
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                    if (!config.getBoolean("Ban.KickDelay.enable"))
                                        e.disallow(Result.KICK_BANNED, component);
                                } else {
                                    Bukkit.getConsoleSender().sendMessage(BanSystemSpigot.prefix + "§e" + e.getName()
                                            + " §cist womöglich ein 2. Account von §e" + names);
                                    for (Player all : Bukkit.getOnlinePlayers()) {
                                        if (all.hasPermission("bansys.notify")) {
                                            all.sendMessage(BanSystemSpigot.prefix + "§e" + e.getName()
                                                    + " §cist womöglich ein 2. Account von §e" + names);
                                        }
                                    }
                                }
                            }
                        }
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
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
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
            if (config.getBoolean("Ban.KickDelay.enable") && banManager.isBanned(p.getUniqueId(), Type.NETWORK)) {
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
                        }

                    }
                }.runTaskLater(BanSystemSpigot.getPlugin(), 20 * config.getInt("Ban.KickDelay.inSecconds"));
            }
        } catch (SQLException throwables) {
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
        if (lvl >= maxLvl) {
            return true;
        }
        return false;
    }

    private int getMaxLvl(String id) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.parseInt(key) > maxLvl) maxLvl = Integer.parseInt(key);
        }
        return maxLvl;
    }
}