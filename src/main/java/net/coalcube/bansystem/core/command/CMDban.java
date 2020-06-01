package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class CMDban implements Command {

    private final BanManager banmanager;
    private final Config config;
    private final Config messages;
    private final MySQL mysql;

    private Type type;
    private String reason;
    private String creator;
    private byte lvl;
    private long duration;
    private UUID uuid;
    private InetAddress address;

    public CMDban(BanManager banmanager, Config config, Config messages, MySQL mysql) {
        this.banmanager = banmanager;
        this.config = config;
        this.messages = messages;
        this.mysql = mysql;
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

        if (config.getBoolean("mysql.enable") && !mysql.isConnected()) {
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

            uuid = UUIDFetcher.getUUID(args[0]);
            if (uuid == null) {
                user.sendMessage(messages.getString("Playerdoesnotexist")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                return;
            }

            if (type != null) {
                if (user.hasPermission("bansys.ban." + args[1]) || user.hasPermission("bansys.ban.all")
                        || user.hasPermission("bansys.ban.admin")) {

                    //Ban
                    if (!(type == Type.CHAT && !banmanager.isBanned(uuid, Type.CHAT)
                            || (type == Type.NETWORK && !banmanager.isBanned(uuid, Type.NETWORK)))) {
                        user.sendMessage(messages.getString("Ban.alreadybanned").replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                        return;
                    }
                    if (BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                        User target = BanSystem.getInstance().getUser(args[0]);
                        address = target.getAddress();
                        if(target == user) {
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
                        if (target.hasPermission("bansys.ban.bypass")){
                            user.sendMessage(messages.getString("Ban.cannotban.bypassedplayers")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                        }

                        /**
                         * TODO: Fix this piece of shit and bring it in a nice and clear shape
                         */

                        // Kick or send mute message
                        if (type == Type.NETWORK) {
                            String banscreen = "";
                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("Datetimepattern"));
                            String enddate = simpleDateFormat.format(new Date(System.currentTimeMillis()+duration));

                            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                                screen.replaceAll("%Reason%", reason)
                                        .replaceAll("%ReamingTime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banmanager.getRemainingTime(uuid, type)))
                                        .replaceAll("%creator", creator)
                                        .replaceAll("%enddate%", enddate)
                                        .replaceAll("%lvl%", String.valueOf(lvl))
                                        .replaceAll("&", "§");

                                banscreen += screen+"\n";
                            }
                            BanSystem.getInstance().disconnect(target, banscreen);
                        } else {
                            for (String message : messages.getStringList("Ban.Chat.Screen")) {
                                target.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%reason%", reason).replaceAll("%Player%", target.getDisplayName())
                                        .replaceAll("%reason%", reason).replaceAll("&", "§"));
                            }
                        }


                    }
                    try {
                        banmanager.ban(uuid, duration, creator, type, reason, address);
                    } catch (IOException e) {
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
                                        .replaceAll("%reamingTime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banmanager.getRemainingTime(uuid, type)))
                                        .replaceAll("%banner%", creator).replaceAll("%type%", type.toString())
                                        .replaceAll("&", "§"));
                    }
                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                        if (all.hasPermission("bansys.notify") && (all != user)) {
                            for (String message : messages.getStringList("Ban.notify")) {
                                all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason)
                                        .replaceAll("%reamingTime%", BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(banmanager.getRemainingTime(uuid, type)))
                                        .replaceAll("%banner%", creator).replaceAll("%type%", type.toString())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                } else
                    user.sendMessage(messages.getString("Ban.ID.NoPermission").replaceAll("%P%",
                            messages.getString("prefix")));

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
        for (String key : config.getSection("IDs").getKeys()) {
            if (args[1].equalsIgnoreCase(key)) {

                // set lvl

                /**
                 * TODO: fix that you have no lvl limit and check that its work fine
                 */
                if (banmanager.hasHistory(uuid, reason)) {
                    if(!isMaxBanLvl(args[1], banmanager.getLevel(uuid, reason))) {
                        lvl = (byte) (banmanager.getLevel(uuid, reason) + 1);
                    } else {
                        lvl = (byte) banmanager.getLevel(uuid, reason);
                    }
                } else {
                    lvl = 1;
                }
                for (String lvlkey : config.getSection("IDs." + key + ".lvl").getKeys()) {
                    if (Byte.parseByte(lvlkey) == lvl) {
                        duration = config.getLong("IDs." + key + ".lvl." + lvlkey + ".duration");
                    }
                }
                if (duration == 0) {
                    duration = -1;
                }

                // check Admin permissions
                if (config.getBoolean("IDs." + key + ".onlyAdmins")) {
                    if (!user.hasPermission("bansys.ban.admin")) {
                        user.sendMessage(messages.getString("Ban.onlyadmins")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }
                }

                //set reason
                reason = config.getString("IDs." + key + ".reason");

                //set Type
                type = Type.valueOf(config.getString("IDs." + key + ".lvl." + lvl + ".type"));

            }
        }
    }

    private boolean isMaxBanLvl(String id, int lvl) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if(Integer.parseInt(key) > maxLvl) maxLvl = Integer.parseInt(key);
        }
        return lvl >= maxLvl;
    }
}
