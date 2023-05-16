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
import net.md_5.bungee.event.EventPriority;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LoginListener implements Listener {

    private final BanManager banManager;
    private final Config config;
    private final Database sql;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;

    public LoginListener(BanManager banManager, Config config, Database sql, URLUtil urlUtil, ConfigurationUtil configurationUtil) {
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent e) {
        PendingConnection con = e.getConnection();
        UUID uuid = con.getUniqueId();

        if (!(config.getBoolean("mysql.enable") && !sql.isConnected())) {
            e.registerIntent(BanSystemBungee.getInstance());
            new Thread(() -> {
                try {
                    if(UUIDFetcher.getName(uuid) == null && !banManager.isSavedBedrockPlayer(uuid)) {
                        if(org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid) != null) {
                            banManager.saveBedrockUser(uuid, con.getName());
                        }
                    }
                } catch (SQLException | ExecutionException | InterruptedException ex) {
                    ex.printStackTrace();
                }
                try {
                    if (banManager.isBanned(uuid, Type.NETWORK)) {
                        try {
                            if (banManager.getEnd(uuid, Type.NETWORK) > System.currentTimeMillis()
                                    || banManager.getEnd(uuid, Type.NETWORK) == -1) {
                                String banScreen = BanSystem.getInstance().getBanScreen();
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                                String enddate = simpleDateFormat.format(new Date(banManager.getEnd(uuid, Type.NETWORK)));
                                try {
                                    e.setCancelReason(banScreen
                                            .replaceAll("%reason%", banManager.getReason(uuid, Type.NETWORK))
                                            .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                    .getFormattedRemainingTime(banManager.getRemainingTime(uuid, Type.NETWORK)))
                                            .replaceAll("%creator%", banManager.getBanner(uuid, Type.NETWORK))
                                            .replaceAll("%enddate%", enddate)
                                            .replaceAll("&", "§")
                                            .replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid, banManager.getReason(uuid, Type.NETWORK)))));
                                } catch (UnknownHostException unknownHostException) {
                                    unknownHostException.printStackTrace();
                                }
                                e.setCancelled(true);
                                // p.disconnect(component);
                                if (!banManager.isSetIP(e.getConnection().getUniqueId())) {
                                    banManager.setIP(e.getConnection().getUniqueId(), con.getAddress().getAddress());
                                }
                            } else {
                                try {
                                    if(config.getBoolean("needReason.Unmute")) {
                                        banManager.unBan(uuid, ProxyServer.getInstance().getConsole().getName(), "Strafe abgelaufen");
                                    } else {
                                        banManager.unBan(uuid, ProxyServer.getInstance().getConsole().getName());
                                    }
                                    banManager.log("Unbanned Player", ProxyServer.getInstance().getConsole().getName(),
                                            con.getUniqueId().toString(), "Autounban");
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                                ProxyServer.getInstance().getConsole()
                                        .sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                                .replaceAll("%player%", con.getName()));
                                for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                    if (all.hasPermission("bansys.notify")) {
                                        all.sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                                .replaceAll("%player%", con.getName()));
                                    }
                                }
                            }
                        } catch (SQLException | ParseException | InterruptedException | ExecutionException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                } catch (SQLException | InterruptedException | ExecutionException throwables) {
                    throwables.printStackTrace();
                }
                if (!e.isCancelled()) {
                    ProxyServer.getInstance().getScheduler().schedule(BanSystemBungee.getInstance(), () -> {
                        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(con.getUniqueId());
                        if (p != null) {
                            if (p.getUniqueId().equals(UUID.fromString("617f0c2b-6014-47f2-bf89-fade1bc9bb59"))) {
                                for(ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                    if(all.hasPermission("bansys.notify")) {
                                        all.sendMessage(configurationUtil.getMessage("prefix") + "§cDer Entwickler §e"
                                                + p.getDisplayName() + " §cist gerade gejoint.");
                                    }
                                }
                                BanSystem.getInstance().getConsole().sendMessage(configurationUtil.getMessage("prefix")
                                        + "§cDer Entwickler §e" + p.getDisplayName() + " §cist gerade gejoint.");
                                p.sendMessage(configurationUtil.getMessage("prefix") + "§cDieser Server benutzt das Bansystem Version §e"
                                        + BanSystem.getInstance().getVersion() + " §cauf §eBungeecord");
                            }
                            if (config.getBoolean("VPN.enable")) {
                                try {
                                    if (urlUtil.isVPN(p.getAddress().getAddress().getHostAddress())) {
                                        if (config.getBoolean("VPN.autoban.enable")) {
                                            try {
                                                int id = config.getInt("VPN.autoban.ID");
                                                String reason = config.getString("IDs." + id + ".reason");
                                                int lvl;
                                                if(isMaxBanLvl(String.valueOf(id), banManager.getLevel(uuid, reason))) {
                                                    lvl = banManager.getLevel(uuid, reason)+1;
                                                } else
                                                    lvl = getMaxLvl(String.valueOf(id));

                                                long duration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");
                                                Type type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvl + ".type"));

                                                banManager.ban(uuid, duration,
                                                        BanSystem.getInstance().getConsole().getDisplayName(), type, reason);
                                                banManager.log("Banned Player", ProxyServer.getInstance().getConsole().getName(),
                                                        p.getUniqueId().toString(), "VPN Autoban");
                                            } catch (IOException | SQLException | InterruptedException | ExecutionException ioException) {
                                                ioException.printStackTrace();
                                            }
                                        } else {
                                            for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                                if(all != p)
                                                    all.sendMessage(configurationUtil.getMessage("VPN.warning")
                                                            .replaceAll("%player%", p.getDisplayName()));
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

                            if (p.hasPermission("bansys.ban.admin")) {
                                try {
                                    if (new UpdateChecker(65863).checkForUpdates()) {
                                        TextComponent comp = new TextComponent(configurationUtil.getMessage("prefix")
                                                + "§7Lade es dir unter §ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");

                                        p.sendMessage(new TextComponent(
                                                configurationUtil.getMessage("prefix") + "§cEin neues Update ist verfügbar."));

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

                            try {
                                List<UUID> playersWithSameIP = banManager.getBannedPlayersWithSameIP(p.getAddress().getAddress());
                                if (!playersWithSameIP.isEmpty() && !p.hasPermission("bansys.ban")
                                        && !playersWithSameIP.contains(p.getUniqueId())) {
                                    StringBuilder bannedPlayerName = new StringBuilder();
                                    boolean rightType = true;
                                    int ipAutoBanID = config.getInt("IPautoban.banid");
                                    String ipAutoBanReason = config.getString("IDs." + ipAutoBanID + ".reason");
                                    int ipAutoBanLvl = 0;

                                    try {
                                        if (!isMaxBanLvl(String.valueOf(ipAutoBanID), banManager.getLevel(uuid, ipAutoBanReason))) {
                                            ipAutoBanLvl = banManager.getLevel(uuid, ipAutoBanReason) + 1;
                                        } else
                                            ipAutoBanLvl = getMaxLvl(String.valueOf(ipAutoBanID));

                                        for (UUID id : playersWithSameIP) {
                                            if (banManager.isBanned(p.getUniqueId(), Type.CHAT))
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

                                    long ipAutoBanDuration = config.getLong("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".duration");
                                    Type ipAutoBanType = Type.valueOf(config.getString("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".type"));

                                    if (!rightType && config.getBoolean("IPautoban.onlyNetworkBans")) {
                                        return;
                                    }
                                    if (config.getBoolean("IPautoban.enable")) {
                                        try {
                                            banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType, ipAutoBanReason, con.getAddress().getAddress());
                                            banManager.log("Banned Player", ProxyServer.getInstance().getConsole().getName(), uuid.toString(), "Same IP Autoban");
                                        } catch (IOException | SQLException ioException) {
                                            ioException.printStackTrace();
                                        }
                                        BanSystem.getInstance().sendConsoleMessage(configurationUtil.getMessage("ip.autoban")
                                                .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                                .replaceAll("%reason%", ipAutoBanReason));
                                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                            if (all.hasPermission("bansys.notify") && all != p) {
                                                all.sendMessage(configurationUtil.getMessage("ip.autoban")
                                                        .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                                        .replaceAll("%reason%", ipAutoBanReason));
                                            }
                                        }
                                        BaseComponent component = null;
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + ipAutoBanDuration));
                                        try {
                                            component = new TextComponent(BanSystem.getInstance().getBanScreen()
                                                    .replaceAll("%reason%", banManager.getReason(uuid, Type.NETWORK))
                                                    .replaceAll("%reamingtime%",
                                                            BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                                                    banManager.getRemainingTime(uuid, Type.NETWORK)))
                                                    .replaceAll("%creator%", ProxyServer.getInstance().getConsole().getName())
                                                    .replaceAll("%enddate%", enddate)
                                                    .replaceAll("%lvl%", String.valueOf(ipAutoBanLvl))
                                                    .replaceAll("&", "§"));
                                        } catch (SQLException | ParseException throwables) {
                                            throwables.printStackTrace();
                                        }
                                        e.setCancelReason(component);
                                        e.setCancelled(true);
                                        p.disconnect(component);
                                    } else {
                                        BanSystem.getInstance().sendConsoleMessage(
                                                configurationUtil.getMessage("ip.warning")
                                                        .replaceAll("%player%", p.getDisplayName())
                                                        .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                                        for (ProxiedPlayer all : ProxyServer.getInstance().getPlayers()) {
                                            if (all.hasPermission("bansys.notify")) {
                                                all.sendMessage( configurationUtil.getMessage("ip.warning")
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
                    }, 1, TimeUnit.SECONDS);
                }
                e.completeIntent(BanSystemBungee.getInstance());
            }).start();
        }
    }
    private boolean isMaxBanLvl(String id, int lvl) {
        return lvl >= getMaxLvl(id);
    }

    private int getMaxLvl(String id) {
        return config.getSection("IDs." + id + ".lvl").getKeys().size();
    }
}
