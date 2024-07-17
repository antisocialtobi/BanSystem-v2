package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PlayerConnectionListener implements Listener {

    private final BanManager banManager;
    private final YamlDocument config;
    private final String banScreenRow;
    private final Plugin instance;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;

    public PlayerConnectionListener(BanManager banManager, YamlDocument config, String banScreen, Plugin instance, URLUtil urlUtil, ConfigurationUtil configurationUtil) {
        this.banManager = banManager;
        this.config = config;
        this.banScreenRow = banScreen;
        this.instance = instance;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        boolean isCancelled = false;
        UUID uuid = e.getUniqueId();
        Database sql = BanSystem.getInstance().getSQL();
        if(!sql.isConnected()) {
            try {
                sql.connect();
            } catch (SQLException ex) {
                return;
            }
        }

        if (BanSystem.getInstance().getSQL().isConnected()) {
            try {
                if (UUIDFetcher.getName(uuid) == null && !banManager.isSavedBedrockPlayer(uuid)) {
                    if (org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid) != null) {
                        banManager.saveBedrockUser(uuid, e.getName());
                    }
                }

                Ban ban = banManager.getBan(uuid, Type.NETWORK);
                if (ban != null) {
                    if (ban.getEnd() > System.currentTimeMillis()
                            || ban.getEnd() == -1) {
                        // disallow connecting when user is banned

                        String reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(ban.getRemainingTime());

                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                        String enddate = simpleDateFormat.format(new Date(ban.getEnd()));

                        String banScreen = banScreenRow
                                .replaceAll("%reason%", ban.getReason())
                                .replaceAll("%creator%", ban.getCreator())
                                .replaceAll("%enddate%", enddate)
                                .replaceAll("%reamingtime%", reamingTime)
                                .replaceAll("&", "§")
                                .replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid, ban.getReason())))
                                .replaceAll("%id%", ban.getId());
                        if (!config.getBoolean("Ban.KickDelay.enable")) e.disallow(Result.KICK_BANNED, banScreen);
                        isCancelled = true;

                        if (!banManager.isSetIP(uuid)) {
                            banManager.setIP(uuid, e.getAddress());
                        }
                    } else {
                        // autounban
                        if (config.getBoolean("needReason.Unban")) {
                            banManager.unBan(ban, Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                        } else {
                            banManager.unBan(ban, Bukkit.getConsoleSender().getName());
                        }
                        banManager.log("Unbanned Player", Bukkit.getConsoleSender().getName(), uuid.toString(), "Autounban");
                        Bukkit.getConsoleSender()
                                .sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                        .replaceAll("%player%", e.getName()));
                        for (Player all : Bukkit.getOnlinePlayers()) {
                            if (all.hasPermission("bansys.notify")) {
                                all.sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                        .replaceAll("%player%", e.getName()));
                            }
                        }
                    }
                }
            } catch (SQLException | UnknownHostException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
            if (!isCancelled) {
                if (config.getBoolean("VPN.enable")) {
                    try {
                        if (urlUtil.isVPN(e.getAddress().getHostAddress())) {
                            if (config.getBoolean("VPN.autoban.enable")) {

                                int lvl = 0;
                                String id = config.getString("VPN.autoban.ID");
                                String reason = config.getString("IDs." + id + ".reason");
                                try {
                                    if (banManager.hasHistory(e.getUniqueId(), reason)) {
                                        if (!banManager.isMaxBanLvl(id, banManager.getLevel(uuid, reason))) {
                                            lvl = (byte) (banManager.getLevel(uuid, reason)) + 1;
                                        } else {
                                            lvl = banManager.getMaxLvl(id);
                                        }
                                    } else {
                                        lvl = 1;
                                    }
                                } catch (UnknownHostException | SQLException | InterruptedException | ExecutionException unknownHostException) {
                                    unknownHostException.printStackTrace();
                                }
                                long time = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                                Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));

                                try {
                                    banManager.ban(uuid, time, Bukkit.getConsoleSender().getName(),
                                            type, reason, e.getAddress());
                                    banManager.log("Banned Player", Bukkit.getConsoleSender().getName(),
                                            uuid.toString(), "VPN Autoban");
                                } catch (IOException | SQLException | ExecutionException | InterruptedException ioException) {
                                    ioException.printStackTrace();
                                }
                            } else {
                                for (Player all : Bukkit.getOnlinePlayers()) {
                                    all.sendMessage(configurationUtil.getMessage("VPN.warning")
                                            .replaceAll("%player%", e.getName()));
                                }
                            }
                        }
                    } catch (IOException ex) {
                        BanSystem.getInstance().getConsole().sendMessage(
                                configurationUtil.getMessage("prefix") + "§cBei der VPN Abfrage ist ein Fehler aufgetreten: " + ex.getMessage());
                        BanSystem.getInstance().getConsole().sendMessage(configurationUtil.getMessage("prefix")
                                + "§cVersuche, falls noch nicht vorhanden, einen API Code für die VPN Api einzutragen " +
                                "indem du auf der seite §ehttps://vpnapi.io/ §cdir einen Acoount erstellst. Falls dies " +
                                "nicht funktioniert, wende dich bitte an den Support unter §ehttps://discord.gg/PfQTqhfjgA§c.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        try {
            Ban ban = banManager.getBan(p.getUniqueId(), Type.NETWORK);
            if (ban != null) {
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
        if (p.getUniqueId().equals(UUID.fromString("617f0c2b-6014-47f2-bf89-fade1bc9bb59"))) {
            for (Player all : Bukkit.getOnlinePlayers()) {
                if (all.hasPermission("bansys.notify")) {
                    all.sendMessage(configurationUtil.getMessage("prefix") + "§cDer Entwickler §e"
                            + p.getDisplayName() + " §cist gerade gejoint.");
                }
            }
            BanSystem.getInstance().getConsole().sendMessage(configurationUtil.getMessage("prefix")
                    + "§cDer Entwickler §e" + p.getDisplayName() + " §cist gerade gejoint.");
            p.sendMessage(configurationUtil.getMessage("prefix") + "§cDieser Server benutzt das Bansystem Version §e"
                    + BanSystem.getInstance().getVersion() + " §cauf §eSpigot");
        }
        if (p.hasPermission("bansys.ban.admin")) {
            try {
                if (new UpdateChecker(65863).checkForUpdates()) {

                    p.sendMessage(BanSystemSpigot.prefix + "§cEin neues Update ist verfügbar.");
                    p.sendMessage(BanSystemSpigot.prefix
                            + "§7Lade es dir unter §ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ " +
                            "§7runter um aktuell zu bleiben.");

                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        try {
            Ban ban = banManager.getBan(p.getUniqueId(), Type.NETWORK);
            if (ban != null) {
                e.setJoinMessage(null);
                new BukkitRunnable() {

                    @Override
                    public void run() {

                        String reamingTime;
                        try {
                            reamingTime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                    ban.getRemainingTime());
                            YamlDocument messages = configurationUtil.getMessagesConfig();
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                            String enddate = simpleDateFormat.format(new Date(ban.getEnd()));

                            String banScreen = banScreenRow
                                    .replaceAll("%P%", configurationUtil.getMessage("prefix"))
                                    .replaceAll("%reason%", ban.getReason())
                                    .replaceAll("%reamingtime%", reamingTime)
                                    .replaceAll("%creator%", ban.getCreator())
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid,
                                            ban.getReason())))
                                    .replaceAll("&", "§")
                                    .replaceAll("%id%", ban.getId());
                            p.kickPlayer(banScreen);
                        } catch (SQLException | InterruptedException | ExecutionException | UnknownHostException throwables) {
                            throwables.printStackTrace();
                        }

                    }
                }.runTaskLater(BanSystemSpigot.getPlugin(), 20L * config.getInt("Ban.KickDelay.inSecconds"));
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }

        try {
            Ban ban = banManager.getBan(p.getUniqueId(), Type.NETWORK);
            if (!banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress()).isEmpty() &&
                    !p.hasPermission("bansys.ban") && !banManager.getBannedPlayersWithSameIP(
                    p.getAddress().getAddress()).contains(p.getUniqueId()) && ban == null) {
                StringBuilder bannedPlayerName = new StringBuilder();
                boolean rightType = true;
                List<UUID> banned;
                int ipAutoBanID = config.getInt("IPautoban.banid");
                String ipAutoBanReason = config.getString("IDs." + ipAutoBanID + ".reason");
                int ipAutoBanLvl = 0;

                try {
                    if (!banManager.isMaxBanLvl(String.valueOf(ipAutoBanID), banManager.getLevel(uuid, ipAutoBanReason))) {
                        ipAutoBanLvl = banManager.getLevel(uuid, ipAutoBanReason) + 1;
                    } else
                        ipAutoBanLvl = banManager.getMaxLvl(String.valueOf(ipAutoBanID));


                    banned = banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress());
                    for (UUID id : banned) {
                        String name;
                        if (UUIDFetcher.getName(id) != null) {
                            name = UUIDFetcher.getName(id);
                        } else if (banManager.isSavedBedrockPlayer(id)) {
                            name = banManager.getSavedBedrockUsername(id);
                        } else {
                            name = id.toString();
                        }
                        if (banManager.getBan(p.getUniqueId(), Type.CHAT) != null)
                            rightType = false;
                        if (bannedPlayerName.length() == 0) {
                            bannedPlayerName = new StringBuilder(name);
                        } else {
                            bannedPlayerName.append(", ").append(name);
                        }
                    }
                } catch (SQLException | UnknownHostException throwables) {
                    throwables.printStackTrace();
                }

                long ipAutoBanDuration = config.getLong("IDs." + ipAutoBanID + ".lvl." + ipAutoBanLvl + ".duration");
                Type ipAutoBanType = Type.valueOf(config.getString("IDs." + ipAutoBanID + ".lvl." + ipAutoBanLvl + ".type"));

                if (!rightType && config.getBoolean("IPautoban.onlyNetworkBans")) {
                    return;
                }
                if (config.getBoolean("IPautoban.enable")) {
                    try {
                        banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType,
                                ipAutoBanReason, p.getAddress().getAddress());
                        banManager.log("Banned Player", Bukkit.getConsoleSender().getName(), uuid.toString(), "Same IP Autoban");
                    } catch (IOException | SQLException ioException) {
                        ioException.printStackTrace();
                    }
                    Bukkit.getConsoleSender()
                            .sendMessage(configurationUtil.getMessage("autoban.ip.notify") + bannedPlayerName
                                    + " §cwurde automatisch gebannt für §e"
                                    + config.getString("IDs." + config.getInt("IPautoban.banid") + ".reason")
                                    + "§c.");
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(configurationUtil.getMessage("ip.autoban")
                                    .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                    .replaceAll("%reason%", ipAutoBanReason));
                        }
                    }
                    String banScreen = BanSystem.getInstance().getBanScreen();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                    String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + ipAutoBanDuration));

                    banScreen = banScreen.replaceAll("%reason%", ban.getReason());
                    banScreen = banScreen.replaceAll("%reamingtime%",
                            BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                    ban.getRemainingTime()));
                    banScreen = banScreen.replaceAll("%creator%", Bukkit.getConsoleSender().getName());
                    banScreen = banScreen.replaceAll("%enddate%", enddate);
                    banScreen = banScreen.replaceAll("%lvl%", String.valueOf(ipAutoBanLvl));
                    banScreen = banScreen.replaceAll("%P%", configurationUtil.getMessage("prefix"));
                    banScreen = banScreen.replaceAll("&", "§");
                    banScreen = banScreen.replaceAll("%id%", ban.getId());
                    p.kickPlayer(banScreen);
                } else {
                    BanSystem.getInstance().sendConsoleMessage(configurationUtil.getMessage("ip.warning")
                            .replaceAll("%player%", p.getDisplayName())
                            .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                    for (Player all : Bukkit.getOnlinePlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(configurationUtil.getMessage("ip.warning")
                                    .replaceAll("%player%", p.getDisplayName())
                                    .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                        }
                    }
                }
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }
    }
}