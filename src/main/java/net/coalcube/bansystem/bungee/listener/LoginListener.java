package net.coalcube.bansystem.bungee.listener;

import net.coalcube.bansystem.bungee.BanSystemBungee;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LoginListener implements Listener {

    private final BanManager banManager;
    private final Config config;
    private final Config messages;
    private final MySQL mysql;
    private final List<InetAddress> bannedAddresses;

    public LoginListener(BanManager banManager, Config config, Config messages, MySQL mysql, List<InetAddress> bannedAddresses) {
        this.banManager = banManager;
        this.config = config;
        this.messages = messages;
        this.mysql = mysql;
        this.bannedAddresses = bannedAddresses;
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onLogin(LoginEvent e) {
        if (mysql.isConnected()) {
            e.registerIntent(BanSystemBungee.getInstance());
            new Thread(() -> {
                PendingConnection con = e.getConnection();
                UUID uuid = con.getUniqueId();

                try {
                    if (banManager.isBanned(uuid, Type.NETWORK)) {
                        try {
                            if (banManager.getEnd(uuid, Type.NETWORK) > System
                                    .currentTimeMillis()
                                    || banManager.getEnd(uuid, Type.NETWORK) == -1) {
                                String banscreen = "";
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                                String enddate = simpleDateFormat.format(new Date(banManager.getEnd(uuid, Type.NETWORK)));

                                for (String screen : messages.getStringList("Ban.Network.Screen")) {
                                    try {
                                        screen.replaceAll("%Reason%", banManager.getReason(uuid, Type.NETWORK));
                                        screen.replaceAll("%ReamingTime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(banManager.getRemainingTime(uuid, Type.NETWORK)));
                                        screen.replaceAll("%creator", banManager.getBanner(uuid, Type.NETWORK));
                                        screen.replaceAll("%enddate%", enddate);
                                        screen.replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid, banManager.getReason(uuid, Type.NETWORK))));
                                        screen.replaceAll("&", "§");
                                    } catch (SQLException | UnknownHostException throwables) {
                                        throwables.printStackTrace();
                                    }

                                    banscreen += screen+"\n";
                                }
                                e.setCancelReason(banscreen);
                                e.setCancelled(true);
                                // p.disconnect(component);
                                if (!banManager.isSetIP(e.getConnection().getUniqueId())) {
                                    banManager.setIP(e.getConnection().getUniqueId(), con.getAddress().getAddress());
                                }
                            } else {
                                try {
                                    if(config.getBoolean("needReason.Unmute")) {
                                        banManager.unBan(uuid, Bukkit.getConsoleSender().getName(), "Strafe abgelaufen");
                                    } else {
                                        banManager.unBan(uuid, Bukkit.getConsoleSender().getName());
                                    }
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                                ProxyServer.getInstance().getConsole()
                                        .sendMessage(messages.getString("Ban.Network.autounban")
                                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("%player%", con.getName()).replaceAll("&", "§"));
                                for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                    if (all.hasPermission("bansys.notify")) {
                                        all.sendMessage(messages.getString("Ban.Network.autounban")
                                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("%player%", con.getName()).replaceAll("&", "§"));
                                    }
                                }
                            }
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                if (!e.isCancelled()) {
                    ProxyServer.getInstance().getScheduler().schedule(BanSystemBungee.getInstance(), () -> {
                        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(e.getConnection().getName());
                        if (p instanceof ProxiedPlayer) {

                            if (config.getBoolean("VPN.enable")) {
                                if (URLUtil
                                        .isVPN(p.getAddress().getAddress().toString().replaceAll("/", ""))) {
                                    if (config.getBoolean("VPN.autoban.enable")) {
                                        try {
                                            banManager.ban(e.getConnection().getUniqueId(),
                                                    config.getLong("IDs." + config.getInt("VPN.autoban.ID") + ".lvl."
                                                            + banManager.getLevel(uuid, config.getString("IDs."
                                                            + config.getInt("VPN.autoban.ID") + ".reason")) + ".duration"),
                                                    BanSystem.getInstance().getConsole().getDisplayName(),
                                                    Type.valueOf(config.getString("IDs." + config.getInt("VPN.autoban.ID") + ".lvl."
                                                            + banManager.getLevel(uuid, config.getString("IDs."
                                                            + config.getInt("VPN.autoban.ID") + ".reason")) + ".type")),
                                                    config.getString("IDs." + config.getInt("VPN.autoban.ID") + ".reason"));
                                        } catch (IOException ioException) {
                                            ioException.printStackTrace();
                                        } catch (SQLException throwables) {
                                            throwables.printStackTrace();
                                        }
                                    } else {
                                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                            all.sendMessage(messages.getString("VPN.warning")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%player%", p.getDisplayName())
                                                    .replaceAll("&", "§"));
                                        }
                                    }
                                }
                            }

                            if (p.hasPermission("bansys.ban.admin")) {
                                try {
                                    if (new UpdateChecker(65863).checkForUpdates()) {
                                        TextComponent comp = new TextComponent(messages.getString("prefix")
                                                + "§7Lade es dir unter §ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");

                                        p.sendMessage(new TextComponent(
                                                messages.getString("prefix") + "§cEin neues Update ist verfügbar."));

                                        comp.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                                                "https://www.spigotmc.org/resources/bansystem-mit-ids.65863/"));
                                        comp.setHoverEvent(new HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                new ComponentBuilder("Klicke um zur Webseite zu gelangen")
                                                        .create()));

                                        p.sendMessage(comp);
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }

                            }

                            if (bannedAddresses.contains(p.getAddress().getAddress())) {
                                String bannedPlayerName = "";
                                boolean rightType = true;
                                List<UUID> banned;
                                String ipAutoBanID = config.getString("IPautoban.banid");
                                String ipAutoBanReason = config.getString("IDs." + ipAutoBanID + ".reason");
                                int ipAutoBanLvl = 0;

                                try {
                                    ipAutoBanLvl = banManager.getLevel(uuid, ipAutoBanReason) + 1;
                                    banned = banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress());
                                    for (UUID id : banned) {
                                        if (banManager.isBanned(p.getUniqueId(), Type.CHAT))
                                            rightType = false;
                                        if(banManager.getReason(id, Type.valueOf(config.getString("IDs." + ipAutoBanID + ".lvl"))).equals("")) {

                                        }
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
                                        banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType, ipAutoBanReason, con.getAddress().getAddress());
                                    } catch (IOException | SQLException ioException) {
                                        ioException.printStackTrace();
                                    }
                                    ProxyServer.getInstance().getConsole()
                                            .sendMessage(messages.getString("autoban.ip.notify") + bannedPlayerName + " §cwurde automatisch gebannt für §e"
                                                    + config.getString("IDs."
                                                    + config.getInt("IPautoban.banid")
                                                    + ".reason")
                                                    + "§c.");
                                    for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                        if (all.hasPermission("bansys.notify")) {
                                            all.sendMessage(messages.getString("prefix") + "§cDer 2. Account von §e"
                                                    + bannedPlayerName + " §cwurde automatisch gebannt für §e"
                                                    + config.getString("IDs."
                                                    + config.getInt("IPautoban.banid")
                                                    + ".reason")
                                                    + "§c.");
                                        }
                                    }
                                    BaseComponent component = null;
                                    try {
                                        component = new TextComponent(BanSystem.getInstance().getBanScreen()
                                                .replaceAll("%Reason%", banManager.getReason(uuid, Type.NETWORK))
                                                .replaceAll("%ReamingTime%",
                                                        BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                                                banManager.getRemainingTime(uuid, Type.NETWORK))));
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                    e.setCancelReason(component);
                                    e.setCancelled(true);
                                    p.disconnect(component);
                                } else {
                                    ProxyServer.getInstance().getConsole()
                                            .sendMessage(messages.getString("prefix") + "§e" + p.getName()
                                                    + " §cist womöglich ein 2. Account von §e" + bannedPlayerName);
                                    for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                        if (all.hasPermission("bansys.notify")) {
                                            all.sendMessage(messages.getString("prefix") + "§e" + p.getName()
                                                    + " §cist womöglich ein 2. Account von §e" + bannedPlayerName);
                                        }
                                    }
                                }
                            }
                        }
                    }, 1, TimeUnit.SECONDS);
                }
                e.completeIntent(BanSystemBungee.getInstance());
            }).start();
        }
    }
}
