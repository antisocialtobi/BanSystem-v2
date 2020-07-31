package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CMDban implements Command {

    private final BanManager banmanager;
    private final Config config;
    private final Config messages;
    private final Database sql;

    private Type type;
    private String reason;
    private String creator;
    private int lvl;
    private long duration;
    private UUID uuid;
    private InetAddress address;

    public CMDban(BanManager banmanager, Config config, Config messages, Database sql) {
        this.banmanager = banmanager;
        this.config = config;
        this.messages = messages;
        this.sql = sql;
    }

    @Override
    public void execute(User user, String[] args) {
        if (!(user.hasPermission("bansys.ban") ||
                user.hasPermission("bansys.ban.all") ||
                user.hasPermission("bansys.all.admin") ||
                hasPermissionForAnyID(user))) {

            user.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    messages.getString("NoPermissionMessage").replaceAll("%P%", messages.getString("prefix"))));

            return;
        }

        if (config.getBoolean("mysql.enable") && !sql.isConnected()) {
            user.sendMessage(messages.getString("NoDBConnection")
                    .replaceAll("&", "§")
                    .replaceAll("%P%", messages.getString("prefix")));
            return;
        }

        if (args.length <= 1) {
            user.sendMessage(messages.getString("Ban.ID.Listlayout.heading").replaceAll("%P%",
                    messages.getString("prefix")));
            for (String key : config.getSection("IDs").getKeys()) {
                if (config.getBoolean("IDs." + key + ".onlyAdmins")) {
                    user.sendMessage(
                            messages.getString("Ban.ID.Listlayout.IDs.onlyadmins").replaceAll("%ID%", key)
                                    .replaceAll("%reason%", config.getString("IDs." + key + ".reason"))
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                } else
                    user.sendMessage(
                            messages.getString("Ban.ID.Listlayout.IDs.general").replaceAll("%ID%", key)
                                    .replaceAll("%reason%", config.getString("IDs." + key + ".reason"))
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
            }
            user.sendMessage(messages.getString("Ban.usage").replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
            return;
        }

        if (args.length == 2) {
            uuid = UUIDFetcher.getUUID(args[0]);
            if (uuid == null) {
                user.sendMessage(messages.getString("Playerdoesnotexist")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                return;
            }

            // Unknown ID
            if (!config.getSection("IDs").getKeys().contains(args[1])) {
                user.sendMessage(messages.getString("Ban.invalidinput")
                        .replaceAll("%P%", messages.getString("prefix")).replaceAll("%ID%", args[1])
                        .replaceAll("&", "§"));
                return;
            }

            // Set Parameters
            try {
                setParameters(user, args);
            } catch (UnknownHostException e) {
                user.sendMessage(messages.getString("Ban.faild")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                e.printStackTrace();
            }

            if (type != null) {
                if (user.hasPermission("bansys.ban." + args[1]) || user.hasPermission("bansys.ban.all")
                        || user.hasPermission("bansys.ban.admin")) {

                    //Ban
                    try {
                        if (!(type == Type.CHAT && !banmanager.isBanned(uuid, Type.CHAT)
                                || (type == Type.NETWORK && !banmanager.isBanned(uuid, Type.NETWORK)))) {
                            user.sendMessage(messages.getString("Ban.alreadybanned").replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                            return;
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }

                    // if target is online
                    if (BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                        User target = BanSystem.getInstance().getUser(args[0]);
                        address = target.getAddress();
                        if (target == user) {
                            user.sendMessage(messages.getString("Ban.cannotban.yourself")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                            return;
                        }
                        if (target.hasPermission("bansys.ban") && !user.hasPermission("bansys.ban.admin")) {
                            user.sendMessage(messages.getString("Ban.cannotban.teammembers")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                            return;
                        }
                        if (target.hasPermission("bansys.ban.bypass")) {
                            user.sendMessage(messages.getString("Ban.cannotban.bypassedplayers")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                        }
                        // Kick or send mute message
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                        String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis() + duration));
                        if (type == Type.NETWORK) {
                            String banScreen = BanSystem.getInstance().getBanScreen();

                            BanSystem.getInstance().disconnect(target, banScreen
                                    .replaceAll("%Reason%", reason)
                                    .replaceAll("%ReamingTime%", BanSystem.getInstance().getTimeFormatUtil()
                                            .getFormattedRemainingTime(duration))
                                    .replaceAll("%creator", creator)
                                    .replaceAll("%enddate%", enddate)
                                    .replaceAll("%lvl%", String.valueOf(lvl)));
                        } else {
                            for (String message : messages.getStringList("Ban.Chat.Screen")) {
                                target.sendMessage(message
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%Player%", target.getDisplayName())
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%creator", creator)
                                        .replaceAll("%enddate%", enddate)
                                        .replaceAll("%lvl%", String.valueOf(lvl))
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                    try {
                        if (address != null)
                            banmanager.ban(uuid, duration, creator, type, reason, address);
                        else
                            banmanager.ban(uuid, duration, creator, type, reason);
                    } catch (IOException | SQLException e) {
                        e.printStackTrace();
                    }
                    user.sendMessage(messages.getString("Ban.success")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%Player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason)
                            .replaceAll("&", "§"));
                    for (String message : messages.getStringList("Ban.notify")) {
                        BanSystem.getInstance().getConsole()
                                .sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason)
                                        .replaceAll("%reamingTime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%banner%", creator).replaceAll("%type%", type.toString())
                                        .replaceAll("&", "§"));
                    }
                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                        if (all.hasPermission("bansys.notify") && (all != user)) {
                            for (String message : messages.getStringList("Ban.notify")) {
                                all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason)
                                        .replaceAll("%reamingTime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%banner%", creator).replaceAll("%type%", type.toString())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                } else
                    user.sendMessage(messages.getString("Ban.ID.NoPermission").replaceAll("%P%",
                            messages.getString("prefix")));

            } else {
                System.out.println("Type is null");
            }
            return;
        }

        if (args.length >= 3) {
            user.sendMessage(messages.getString("Ban.usage")
                    .replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
            return;
        }
    }

    private boolean hasPermissionForAnyID(User user) {
        for (String key : config.getSection("IDs").getKeys()) {
            if (user.hasPermission("bansys.ban." + key))
                return true;
        }
        return false;
    }

    private void setParameters(User user, String[] args) throws UnknownHostException {

        // set creator
        if (user.getUniqueId() != null) {
            creator = user.getUniqueId().toString();
        } else {
            creator = user.getName();
        }

        // set ID
        if (config.getSection("IDs").getKeys().contains(args[1])) {
            String id = args[1];
            //set reason
            reason = config.getString("IDs." + id + ".reason");

            // set lvl

            /**
             * TODO: fix that you have no lvl limit and check that its work fine
             */
            try {
                if (banmanager.hasHistory(uuid, reason)) {
                    if (!isMaxBanLvl(args[1], banmanager.getLevel(uuid, reason))) {
                        System.out.println("lvl is not maxlvl");
                        lvl = (banmanager.getLevel(uuid, reason) + 1);
                    } else {
                        System.out.println("lvl is maxlvl");
                        lvl = getMaxLvl(args[1]);
                    }

                } else {
                    lvl = 1;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }


            System.out.println(lvl);

            //set duration
            for (String lvlkey : config.getSection("IDs." + id + ".lvl").getKeys()) {
                if (Integer.valueOf(lvlkey) == lvl) {
                    duration = config.getLong("IDs." + id + ".lvl." + lvlkey + ".duration");
                    duration = (duration == -1) ? duration : duration * 1000;
                    type = Type.valueOf(config.getString("IDs." + id + ".lvl." + lvlkey + ".type"));
                }
            }
            if (duration == 0) {
                duration = -1;
            }

            // check Admin permissions
            if (config.getBoolean("IDs." + id + ".onlyAdmins")) {
                if (!user.hasPermission("bansys.ban.admin")) {
                    user.sendMessage(messages.getString("Ban.onlyadmins")
                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                    return;
                }
            }
        }
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
