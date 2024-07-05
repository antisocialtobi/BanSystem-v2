package net.coalcube.bansystem.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import net.coalcube.bansystem.velocity.BanSystemVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class LoginEvent {

    private final BanSystemVelocity banSystemVelocity;
    private final BanManager banManager;
    private final YamlDocument config;
    private final Database sql;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;

    public LoginEvent(BanSystemVelocity banSystemVelocity, BanManager banManager, YamlDocument config, Database sql, URLUtil urlUtil, ConfigurationUtil configurationUtil) {
        this.banSystemVelocity = banSystemVelocity;
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerChat(com.velocitypowered.api.event.connection.LoginEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if(!sql.isConnected()) {
            try {
                sql.connect();
            } catch (SQLException ex) {
                return;
            }
        }

        if (!(config.getBoolean("mysql.enable") && !sql.isConnected())) {
            //new Thread(() -> {
                try {
                    if(UUIDFetcher.getName(uuid) == null && !banManager.isSavedBedrockPlayer(uuid)) {
                        if(org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid) != null) {
                            banManager.saveBedrockUser(uuid, player.getUsername());
                        }
                    }
                } catch (SQLException | ExecutionException | InterruptedException ex) {
                    ex.printStackTrace();
                }
                try {
                    Ban ban = banManager.getBan(uuid, Type.NETWORK);
                    if (ban != null) {
                        try {
                            if (ban.getEnd() > System.currentTimeMillis()
                                    || ban.getEnd() == -1) {
                                String banScreen = BanSystem.getInstance().getBanScreen();
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                                String enddate = simpleDateFormat.format(new Date(ban.getEnd()));
                                try {
                                    e.setResult(ResultedEvent.ComponentResult.denied(Component.text(banScreen
                                            .replaceAll("%reason%", ban.getReason())
                                            .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                    .getFormattedRemainingTime(ban.getRemainingTime()))
                                            .replaceAll("%creator%", ban.getCreator())
                                            .replaceAll("%enddate%", enddate)
                                            .replaceAll("&", "§")
                                            .replaceAll("%lvl%", String.valueOf(banManager.getLevel(uuid, ban.getReason()))))));
                                } catch (UnknownHostException unknownHostException) {
                                    unknownHostException.printStackTrace();
                                }
                                // p.disconnect(component);
                                if (!banManager.isSetIP(player.getUniqueId())) {
                                    banManager.setIP(player.getUniqueId(), player.getRemoteAddress().getAddress());
                                }
                            } else {
                                try {
                                    if(config.getBoolean("needReason.Unmute")) {
                                        banManager.unBan(uuid, banSystemVelocity.getConsole().getName(), Type.NETWORK, "Strafe abgelaufen");
                                    } else {
                                        banManager.unBan(uuid, banSystemVelocity.getConsole().getName(), Type.NETWORK);
                                    }
                                    banManager.log("Unbanned Player", banSystemVelocity.getConsole().getName(),
                                            player.getUniqueId().toString(), "Autounban");
                                } catch (IOException ioException) {
                                    ioException.printStackTrace();
                                }
                                banSystemVelocity.sendConsoleMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                                .replaceAll("%player%", player.getUsername()));
                                for (User all : banSystemVelocity.getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify")) {
                                        all.sendMessage(configurationUtil.getMessage("Ban.Network.autounban")
                                                .replaceAll("%player%", player.getUsername()));
                                    }
                                }
                            }
                        } catch (SQLException | InterruptedException | ExecutionException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                } catch (SQLException | InterruptedException | ExecutionException throwables) {
                    throwables.printStackTrace();
                }
                if (e.getResult().isAllowed()) {
                    if (player.getUniqueId().equals(UUID.fromString("617f0c2b-6014-47f2-bf89-fade1bc9bb59"))) {
                        for (Player all : banSystemVelocity.getServer().getAllPlayers()) {
                            if (all.hasPermission("bansys.notify")) {
                                all.sendMessage(Component.text(configurationUtil.getMessage("prefix") + "§cDer Entwickler §e"
                                        + player.getUsername() + " §cist gerade gejoint."));
                            }
                        }
                        banSystemVelocity.sendConsoleMessage(configurationUtil.getMessage("prefix")
                                + "§cDer Entwickler §e" + player.getUsername() + " §cist gerade gejoint.");
                        banSystemVelocity.getServer().getScheduler()
                                .buildTask(banSystemVelocity, () -> player.sendMessage(Component.text(configurationUtil.getMessage("prefix") + "§cDieser Server benutzt das Bansystem Version §e"
                                        + BanSystem.getInstance().getVersion() + " §cauf §eVelocity")))
                                .delay(1L, TimeUnit.SECONDS)
                                .schedule();
                    }
                    if (config.getBoolean("VPN.enable")) {
                        try {
                            if (urlUtil.isVPN(player.getRemoteAddress().getAddress().getHostAddress())) {
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
                                        banManager.log("Banned Player",banSystemVelocity.getConsole().getName(),
                                                player.getUniqueId().toString(), "VPN Autoban");
                                    } catch (IOException | SQLException | InterruptedException | ExecutionException ioException) {
                                        ioException.printStackTrace();
                                    }
                                } else {
                                    for (Player all : banSystemVelocity.getServer().getAllPlayers()) {
                                        if(all != player)
                                            all.sendMessage(Component.text(configurationUtil.getMessage("VPN.warning")
                                                    .replaceAll("%player%", player.getUsername())));
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            banSystemVelocity.sendConsoleMessage(
                                    configurationUtil.getMessage("prefix") + "§cBei der VPN Abfrage ist ein Fehler aufgetreten: " + ex.getMessage());
                            banSystemVelocity.sendConsoleMessage(configurationUtil.getMessage("prefix")
                                    + "§cVersuche, falls noch nicht vorhanden, einen API Code für die VPN Api einzutragen " +
                                    "indem du auf der seite §ehttps://vpnapi.io/ §cdir einen Acoount erstellst. Falls dies " +
                                    "nicht funktioniert, wende dich bitte an den Support unter §ehttps://discord.gg/PfQTqhfjgA§c.");
                        }
                    }

                    if (player.hasPermission("bansys.ban.admin")) {
                        try {
                            if (new UpdateChecker(65863).checkForUpdates()) {
                                banSystemVelocity.getServer().getScheduler()
                                        .buildTask(banSystemVelocity, () -> {
                                            TextComponent comp = Component.text(configurationUtil.getMessage("prefix")
                                                    + "§7Lade es dir unter §ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");

                                            player.sendMessage(Component.text(
                                                    configurationUtil.getMessage("prefix") + "§cEin neues Update ist verfügbar."));

                                            comp = comp.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL,
                                                    "https://www.spigotmc.org/resources/bansystem-mit-ids.65863/"));
                                            comp = comp.hoverEvent(HoverEvent.showText(Component.text("Klicke um zur Webseite zu gelangen")));

                                            player.sendMessage(comp);
                                        })
                                        .delay(1L, TimeUnit.SECONDS)
                                        .schedule();
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }

                    try {
                        List<UUID> playersWithSameIP = banManager.getBannedPlayersWithSameIP(player.getRemoteAddress().getAddress());
                        if (!playersWithSameIP.isEmpty() && !player.hasPermission("bansys.ban")
                                && !playersWithSameIP.contains(player.getUniqueId())) {
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

                                Ban mute = banManager.getBan(player.getUniqueId(), Type.CHAT);
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

                            long ipAutoBanDuration = config.getLong("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".duration");
                            Type ipAutoBanType = Type.valueOf(config.getString("IDs."+ ipAutoBanID + ".lvl." + ipAutoBanLvl + ".type"));

                            if (!rightType && config.getBoolean("IPautoban.onlyNetworkBans")) {
                                return;
                            }
                            if (config.getBoolean("IPautoban.enable")) {
                                Ban ban = null;
                                try {
                                    ban = banManager.ban(uuid, ipAutoBanDuration, BanSystem.getInstance().getConsole().getName(), ipAutoBanType, ipAutoBanReason, player.getRemoteAddress().getAddress());
                                    banManager.log("Banned Player", banSystemVelocity.getConsole().getName(), uuid.toString(), "Same IP Autoban");
                                } catch (IOException | SQLException ioException) {
                                    ioException.printStackTrace();
                                }
                                BanSystem.getInstance().sendConsoleMessage(configurationUtil.getMessage("ip.autoban")
                                        .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                        .replaceAll("%reason%", ipAutoBanReason));
                                for (Player all : banSystemVelocity.getServer().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all != player) {
                                        all.sendMessage(Component.text(configurationUtil.getMessage("ip.autoban")
                                                .replaceAll("%bannedaccount%", bannedPlayerName.toString())
                                                .replaceAll("%reason%", ipAutoBanReason)));
                                    }
                                }
                                Component component = null;
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
                                String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + ipAutoBanDuration));
                                component = Component.text(BanSystem.getInstance().getBanScreen()
                                        .replaceAll("%reason%", ban.getReason())
                                        .replaceAll("%reamingtime%",
                                                BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(
                                                        ban.getRemainingTime()))
                                        .replaceAll("%creator%", banSystemVelocity.getConsole().getName())
                                        .replaceAll("%enddate%", enddate)
                                        .replaceAll("%lvl%", String.valueOf(ipAutoBanLvl))
                                        .replaceAll("&", "§"));
                                e.setResult(ResultedEvent.ComponentResult.denied(component));
                            } else {
                                BanSystem.getInstance().sendConsoleMessage(
                                        configurationUtil.getMessage("ip.warning")
                                                .replaceAll("%player%", player.getUsername())
                                                .replaceAll("%bannedaccount%", bannedPlayerName.toString()));
                                for (Player all : banSystemVelocity.getServer().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify")) {
                                        all.sendMessage(Component.text(configurationUtil.getMessage("ip.warning")
                                                .replaceAll("%player%", player.getUsername())
                                                .replaceAll("%bannedaccount%", bannedPlayerName.toString())));
                                    }
                                }
                            }
                        }
                    } catch (SQLException | ExecutionException | InterruptedException throwables) {
                        throwables.printStackTrace();
                    }
                }
            //}).start();
        }
    }
    private boolean isMaxBanLvl(String id, int lvl) {
        return lvl >= getMaxLvl(id);
    }

    private int getMaxLvl(String id) {
        return config.getSection("IDs." + id + ".lvl").getKeys().size();
    }
}