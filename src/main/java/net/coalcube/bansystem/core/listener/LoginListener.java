package net.coalcube.bansystem.core.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponent;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import org.bstats.charts.SimplePie;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class LoginListener {

    private final BanSystem bansystem;
    private final BanManager banManager;
    private final ConfigurationUtil configurationUtil;
    private final YamlDocument config;
    private final Database sql;
    private final IDManager idManager;
    private final URLUtil urlUtil;
    private final TextComponent textComponent;
    private final SimpleDateFormat simpleDateFormat;
    private final MetricsAdapter metricsAdapter;
    private final Map<String, Boolean> vpnIpCache;

    public LoginListener(BanSystem bansystem,
                         BanManager banManager,
                         ConfigurationUtil configurationUtil,
                         Database sql,
                         IDManager idManager,
                         URLUtil urlUtil,
                         TextComponent textComponent) {
        this.bansystem = bansystem;
        this.banManager = banManager;
        this.configurationUtil = configurationUtil;
        this.sql = sql;
        this.idManager = idManager;
        this.urlUtil = urlUtil;
        this.textComponent = textComponent;
        this.metricsAdapter = bansystem.getMetricsAdapter();

        this.simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
        this.config = configurationUtil.getConfig();
        this.vpnIpCache = new HashMap<>();

    }

    public Event onJoin(UUID uuid, String name, InetAddress inetAddress) {
        Event e = new Event();
        String ip = inetAddress.getHostAddress();
        Ban ban = null;

        if (!sql.isConnected()) {
            return e;
        }

        try {
            if (!banManager.isSavedBedrockPlayer(uuid) && UUIDFetcher.getName(uuid) == null) {
                if (org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid) != null) {
                    banManager.saveBedrockUser(uuid, name);
                }
            }
            ban = banManager.getBan(uuid, Type.NETWORK);
        } catch (SQLException | ExecutionException | InterruptedException ex) {
            ex.printStackTrace();
        }

        if (ban != null) {
            try {
                if (ban.getEnd() > System.currentTimeMillis()
                        || ban.getEnd() == -1) {
                    String banScreen = BanSystem.getInstance().getBanScreen();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                    String enddate = simpleDateFormat.format(new Date(ban.getEnd()));
                    try {
                        e.setCancelReason(banScreen
                                .replaceAll("%reason%", ban.getReason())
                                .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                        .getFormattedRemainingTime(ban.getRemainingTime()))
                                .replaceAll("%creator%", ban.getCreator())
                                .replaceAll("%enddate%", enddate)
                                .replaceAll("&", "§")
                                .replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid, ban.getReason())))
                                .replaceAll("%id%", ban.getId()));
                    } catch (UnknownHostException unknownHostException) {
                        unknownHostException.printStackTrace();
                    }
                    e.setCancelled(true);
                    // user.disconnect(component);
                    if (!banManager.isSetIP(uuid)) {
                        banManager.setIP(uuid, inetAddress);
                    }
                } else {
                    if (config.getBoolean("needReason.Unban")) {
                        banManager.unBan(ban, bansystem.getConsole().getName(), "Strafe abgelaufen");
                    } else {
                        banManager.unBan(ban, bansystem.getConsole().getName());
                    }
                    banManager.log("Unbanned Player", bansystem.getConsole().getName(),
                            uuid.toString(), "Autounban; banID: " + ban.getId());
                    bansystem.getConsole()
                            .sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                    .replaceAll("%player%", name));
                    for (User all : bansystem.getAllPlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                    .replaceAll("%player%", name));
                        }
                    }
                }
            } catch (SQLException | InterruptedException | ExecutionException throwables) {
                throwables.printStackTrace();
            }
        }
        return e;
    }

    public Event onPostJoin(User user, InetAddress inetAddress) throws SQLException, ExecutionException, InterruptedException {
        Event event = new Event();
        String ip = inetAddress.getHostAddress();
        if(!sql.isConnected()) {
            return event;
        }
        if (user == null) {
            return event;
        }
        UUID uuid = user.getUniqueId();
        Ban ban = banManager.getBan(uuid, Type.NETWORK);
        if (config.getBoolean("VPN.enable")) {
            try {
                if (!vpnIpCache.containsKey(ip)) {
                    boolean isVPN = urlUtil.isVPN(ip);
                    vpnIpCache.put(ip, isVPN);
                }
                if (vpnIpCache.get(ip)) {
                    metricsAdapter.addCustomChart(new SimplePie("automations", () -> {
                        return "VPN detected";
                    }));
                    if (config.getBoolean("VPN.autoban.enable")) {
                        try {
                            int id = config.getInt("VPN.autoban.ID");
                            String reason = config.getString("IDs." + id + ".reason");
                            int lvl;
                            if (idManager.getLastLvl(String.valueOf(id)) > banManager.getLevel(uuid, reason)) {
                                lvl = banManager.getLevel(uuid, reason) + 1;
                            } else
                                lvl = idManager.getLastLvl(String.valueOf(id));

                            long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                            Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));

                            ban = banManager.ban(uuid, duration, bansystem.getConsole().getName(), type, reason);
                            banManager.log("Banned Player", bansystem.getConsole().getName(), user.getUniqueId().toString(),
                                    "VPN Autoban; " +
                                            "banID: " + ban.getId() + "; " +
                                            "reason: " + ban.getReason() + "; " +
                                            "Type: " + type + "; " +
                                            "duration: " + ban.getDuration() + ";");

                            String endDate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));
                            String banScreen = BanSystem.getInstance().getBanScreen()
                                    .replaceAll("%reason%", ban.getReason())
                                    .replaceAll("%reamingtime%",
                                            BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                                    ban.getRemainingTime()))
                                    .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                                    .replaceAll("%enddate%", endDate)
                                    .replaceAll("%lvl%", String.valueOf(lvl))
                                    .replaceAll("&", "§")
                                    .replaceAll("%id%", ban.getId());
                            event.setCancelReason(banScreen);
                            event.setCancelled(true);
                            user.disconnect(banScreen);

                            String notify = configurationUtil.getMessage("Ban.notify")
                                    .replaceAll("%player%", Objects.requireNonNull(user.getDisplayName()))
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                            .getFormattedRemainingTime(duration))
                                    .replaceAll("%banner%", bansystem.getConsole().getName())
                                    .replaceAll("%enddate%", endDate)
                                    .replaceAll("%type%", type.toString())
                                    .replaceAll("%id%", ban.getId());

                            for (User all : bansystem.getAllPlayers()) {
                                if (all.hasPermission("bansys.notify") && (all.getUniqueId() != user.getUniqueId())) {
                                    all.sendMessage(notify);
                                }
                            }

                            bansystem.sendConsoleMessage(notify);

                        } catch (IOException | SQLException | InterruptedException |
                                 ExecutionException ioException) {
                            ioException.printStackTrace();
                        }
                    } else {
                        for (User all : bansystem.getAllPlayers()) {
                            if (all.hasPermission("bansys.notify") && (all.getUniqueId() != user.getUniqueId())) {
                                all.sendMessage(configurationUtil.getMessage("VPN.warning")
                                        .replaceAll("%player%", user.getDisplayName()));
                            }
                        }
                        bansystem.sendConsoleMessage(configurationUtil.getMessage("VPN.warning")
                                .replaceAll("%player%", user.getDisplayName()));
                    }
                }
            } catch (IOException ex) {
                bansystem.sendConsoleMessage(
                        configurationUtil.getMessage("prefix") + "§cBei der VPN Abfrage ist ein Fehler aufgetreten: " + ex.getMessage());
                bansystem.sendConsoleMessage(configurationUtil.getMessage("prefix")
                        + "§cVersuche, falls noch nicht vorhanden, einen API Code für die VPN Api einzutragen " +
                        "indem du auf der seite §ehttps://vpnapi.io/ §cdir einen Acoount erstellst. Falls dies " +
                        "nicht funktioniert, wende dich bitte an den Support unter §ehttps://discord.gg/PfQTqhfjgA§c.");
            }
        }

        if (user.hasPermission("bansys.ban.admin")) {
            try {
                if (config.getBoolean("updateCheck")) {
                    if (bansystem.isUpdateAvailable()) {
                        textComponent.sendUpdateMessage(user);
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        try {
            List<UUID> playersWithSameIP = banManager.getBannedPlayersWithSameIP(user.getAddress());
            if (!playersWithSameIP.isEmpty() && !user.hasPermission("bansys.ban")
                    && !playersWithSameIP.contains(user.getUniqueId())) {
                StringBuilder bannedPlayerName = new StringBuilder();
                boolean rightType = true;
                int ipAutoBanID = config.getInt("IPautoban.banid");
                String ipAutoBanReason = config.getString("IDs." + ipAutoBanID + ".reason");
                int ipAutoBanLvl = 0;

                try {
                    if (idManager.getLastLvl(String.valueOf(ipAutoBanID)) > banManager.getLevel(uuid, ipAutoBanReason)) {
                        ipAutoBanLvl = banManager.getLevel(uuid, ipAutoBanReason) + 1;
                    } else
                        ipAutoBanLvl = idManager.getLastLvl(String.valueOf(ipAutoBanID));

                    Ban mute = banManager.getBan(user.getUniqueId(), Type.CHAT);
                    for (UUID id : playersWithSameIP) {
                        if (mute != null)
                            rightType = false;
                        if (bannedPlayerName.length() == 0) {
                            bannedPlayerName = new StringBuilder(Objects.requireNonNull(UUIDFetcher.getName(id)));
                        } else {
                            bannedPlayerName.append(", ").append(UUIDFetcher.getName(id));
                        }
                    }
                } catch (SQLException | UnknownHostException throwables) {
                    throwables.printStackTrace();
                }

                long ipAutoBanDuration = config.getLong("IDs." + ipAutoBanID + ".lvl." + ipAutoBanLvl + ".duration");
                Type ipAutoBanType = Type.valueOf(config.getString("IDs." + ipAutoBanID + ".lvl." + ipAutoBanLvl + ".type"));

                if (!rightType && config.getBoolean("IPautoban.onlyNetworkBans")) {
                    return event;
                }
                metricsAdapter.addCustomChart(new SimplePie("automations", () -> {
                    return "Alt account detected";
                }));
                if (config.getBoolean("IPautoban.enable")) {
                    try {
                        ban = banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType, ipAutoBanReason, user.getAddress());
                        banManager.log("Banned Player", BanSystem.getInstance().getConsole().getName(), uuid.toString(),
                                "Same IP Autoban; " +
                                        "banID: " + ban.getId() + "; " +
                                        "reason: " + ban.getReason() + "; " +
                                        "Type: " + ipAutoBanType + "; " +
                                        "duration: " + ban.getDuration() + ";");
                    } catch (IOException | SQLException ioException) {
                        ioException.printStackTrace();
                    }
                    BanSystem.getInstance().sendConsoleMessage(configurationUtil.getMessage("ip.autoban")
                            .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                            .replaceAll("%reason%", ipAutoBanReason));
                    for (User all : bansystem.getAllPlayers()) {
                        if (all.hasPermission("bansys.notify") && all != user) {
                            all.sendMessage(configurationUtil.getMessage("ip.autoban")
                                    .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                    .replaceAll("%reason%", ipAutoBanReason));
                        }
                    }

                    String endDate = simpleDateFormat.format(new Date(System.currentTimeMillis() + ipAutoBanDuration));
                    String banScreen = BanSystem.getInstance().getBanScreen()
                            .replaceAll("%reason%", ban.getReason())
                            .replaceAll("%reamingtime%",
                                    BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                            ban.getRemainingTime()))
                            .replaceAll("%creator%", BanSystem.getInstance().getConsole().getName())
                            .replaceAll("%enddate%", endDate)
                            .replaceAll("%lvl%", String.valueOf(ipAutoBanLvl))
                            .replaceAll("&", "§")
                            .replaceAll("%id%", ban.getId());
                    event.setCancelReason(banScreen);
                    event.setCancelled(true);
                    user.disconnect(banScreen);
                } else {
                    BanSystem.getInstance().sendConsoleMessage(
                            configurationUtil.getMessage("ip.warning")
                                    .replaceAll("%player%", user.getDisplayName())
                                    .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                    for (User all : bansystem.getAllPlayers()) {
                        if (all.hasPermission("bansys.notify")) {
                            all.sendMessage(configurationUtil.getMessage("ip.warning")
                                    .replaceAll("%player%", user.getDisplayName())
                                    .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                        }
                    }
                }
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }
        return event;
    }
}
