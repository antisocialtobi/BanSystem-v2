package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.ChatColor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CMDban implements Command {

    private final BanManager banmanager;
    private final Config config;
    private final Config messages;
    private final Database sql;

    private SimpleDateFormat simpleDateFormat;
    private Type type;
    private String reason;
    private String creator;
    private String creatorName;
    private Date endDate;
    private int lvl;
    private long duration;
    private UUID uuid;
    private String name;
    private InetAddress address;
    private ArrayList<Integer> ids;

    public CMDban(BanManager banmanager, Config config, Config messages, Database sql) {
        this.banmanager = banmanager;
        this.config = config;
        this.messages = messages;
        this.sql = sql;
    }

    @Override
    public void execute(User user, String[] args) {
        simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
        ids = new ArrayList<>();

        if (!user.hasPermission("bansys.ban") &&
                !user.hasPermission("bansys.ban.all") &&
                !user.hasPermission("bansys.ban.admin") &&
                !hasPermissionForAnyID(user)) {

            user.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    messages.getString("NoPermissionMessage").replaceAll("%P%", messages.getString("prefix"))));

            return;
        }

        for (String key : config.getSection("IDs").getKeys()) {
            ids.add(Integer.valueOf(key));
        }

        Collections.sort(ids);

        if (config.getBoolean("mysql.enable") && !sql.isConnected()) {
            user.sendMessage(messages.getString("NoDBConnection")
                    .replaceAll("&", "§")
                    .replaceAll("%P%", messages.getString("prefix")));
            return;
        }

        if (args.length <= 1) {
            user.sendMessage(messages.getString("Ban.ID.Listlayout.heading")
                    .replaceAll("%P%", messages.getString("prefix")));
            for (Integer key : ids) {
                if (config.getBoolean("IDs." + key + ".onlyAdmins")) {
                    user.sendMessage(
                            messages.getString("Ban.ID.Listlayout.IDs.onlyadmins")
                                    .replaceAll("%ID%", key.toString())
                                    .replaceAll("%reason%", config.getString("IDs." + key + ".reason"))
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                } else
                    user.sendMessage(
                            messages.getString("Ban.ID.Listlayout.IDs.general").replaceAll("%ID%", key.toString())
                                    .replaceAll("%reason%", config.getString("IDs." + key + ".reason"))
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
            }
            user.sendMessage(messages.getString("Ban.usage").replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
            return;
        }

        if (args.length == 2) {


            // Set name and uuid
            if(BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                uuid = BanSystem.getInstance().getUser(args[0]).getUniqueId();
                name = BanSystem.getInstance().getUser(args[0]).getName();
            } else {
                try {
                    uuid = UUID.fromString(args[0]);
                    if(UUIDFetcher.getName(uuid) == null) {
                        if(banmanager.isSavedBedrockPlayer(uuid)) {
                            name = banmanager.getSavedBedrockUsername(uuid);
                            uuid = banmanager.getSavedBedrockUUID(name);
                        }
                    } else {
                        name = UUIDFetcher.getName(uuid);
                    }
                } catch (IllegalArgumentException exception) {
                    if(UUIDFetcher.getUUID(args[0].replaceAll("&", "§")) == null) {
                        try {
                            if(banmanager.isSavedBedrockPlayer(args[0].replaceAll("&", "§"))) {
                                uuid = banmanager.getSavedBedrockUUID(args[0].replaceAll("&", "§"));
                                name = banmanager.getSavedBedrockUsername(uuid);
                            } else
                                uuid = null;
                        } catch (SQLException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        uuid = UUIDFetcher.getUUID(args[0].replaceAll("&", "§"));
                        name = UUIDFetcher.getName(uuid);
                    }
                } catch (SQLException | ExecutionException | InterruptedException throwables) {
                    throwables.printStackTrace();
                }
            }

            if (uuid == null) {
                user.sendMessage(messages.getString("Playerdoesnotexist")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                return;
            }

            // Unknown ID
            if (!ids.contains(Integer.valueOf(args[1]))) {
                user.sendMessage(messages.getString("Ban.invalidinput")
                        .replaceAll("%P%", messages.getString("prefix")).replaceAll("%ID%", args[1])
                        .replaceAll("&", "§"));
                return;
            }

            // cannot ban yourself
            if(user.getUniqueId() != null && user.getUniqueId().equals(uuid)) {
                user.sendMessage(messages.getString("Ban.cannotban.yourself")
                        .replaceAll("%P%", messages.getString("prefix"))
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
                return;
            }

            if (type != null) {
                if (user.hasPermission("bansys.ban." + args[1]) || user.hasPermission("bansys.ban.all")
                        || user.hasPermission("bansys.ban.admin")) {
                    String formattedEndDate;
                    if(endDate  != null) {
                        formattedEndDate = simpleDateFormat.format(endDate);
                    } else
                        formattedEndDate = "§4PERMANENT";


                    try {
                        if (!(type == Type.CHAT && !banmanager.isBanned(uuid, Type.CHAT)
                                || (type == Type.NETWORK && !banmanager.isBanned(uuid, Type.NETWORK)))) {
                            user.sendMessage(messages.getString("Ban.alreadybanned")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                    .replaceAll("&", "§"));
                            return;
                        }
                    } catch (SQLException throwables) {
                        user.sendMessage(messages.getString("Ban.faild")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        throwables.printStackTrace();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    // if target is online
                    if (BanSystem.getInstance().getUser(name).getUniqueId() != null) {
                        User target = BanSystem.getInstance().getUser(name.replaceAll("&", "§"));
                        address = target.getAddress();
                        if (target == user) {
                            user.sendMessage(messages.getString("Ban.cannotban.yourself")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                            return;
                        }

                        if((target.hasPermission("bansys.ban") || target.hasPermission("bansys.ban.all") || hasPermissionForAnyID(target))
                                && !user.hasPermission("bansys.ban.admin")) {
                            user.sendMessage(messages.getString("Ban.cannotban.teammembers")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                            return;
                        }

                        if(target.hasPermission("bansys.ban.admin") && user.getUniqueId() != null) {
                            user.sendMessage(messages.getString("Ban.cannotban.teammembers")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                            return;
                        }

                        if (target.hasPermission("bansys.ban.bypass") && !user.hasPermission("bansys.ban.admin")) {
                            user.sendMessage(messages.getString("Ban.cannotban.bypassedplayers")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                            return;
                        }
                        // Kick or send mute message
                        if (type == Type.NETWORK) {
                            String banScreen = BanSystem.getInstance().getBanScreen();

                            BanSystem.getInstance().disconnect(target, banScreen
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                            .getFormattedRemainingTime(duration))
                                    .replaceAll("%creator%", creatorName)
                                    .replaceAll("%enddate%", formattedEndDate)
                                    .replaceAll("%lvl%", String.valueOf(lvl))
                                    .replaceAll("&", "§"));
                        } else {
                            for (String message : messages.getStringList("Ban.Chat.Screen")) {
                                target.sendMessage(message
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%player%", target.getDisplayName())
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%creator%", creatorName)
                                        .replaceAll("%enddate%", formattedEndDate)
                                        .replaceAll("%lvl%", String.valueOf(lvl))
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                    // Ban Player
                    try {
                        if (address != null)
                            banmanager.ban(uuid, duration, creator, type, reason, address);
                        else
                            banmanager.ban(uuid, duration, creator, type, reason);

                        banmanager.log("Banned Player", creator, uuid.toString(), "reason: "+reason+", lvl: "+lvl);
                    } catch (IOException | SQLException e) {
                        user.sendMessage(messages.getString("Ban.faild")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        e.printStackTrace();
                    }

                    user.sendMessage(messages.getString("Ban.success")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%Player%", Objects.requireNonNull(name))
                            .replaceAll("%reason%", reason)
                            .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                    .getFormattedRemainingTime(duration))
                            .replaceAll("%banner%", creatorName)
                            .replaceAll("%type%", type.toString())
                            .replaceAll("%enddate%", formattedEndDate)
                            .replaceAll("&", "§"));
                    if(user.getUniqueId() != null) {
                        for (String message : messages.getStringList("Ban.notify")) {
                            BanSystem.getInstance().getConsole()
                                    .sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                            .replaceAll("%reason%", reason)
                                            .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                    .getFormattedRemainingTime(duration))
                                            .replaceAll("%banner%", creatorName)
                                            .replaceAll("%type%", type.toString())
                                            .replaceAll("&", "§"));
                        }
                    }
                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                        if (all.hasPermission("bansys.notify") && (all.getUniqueId() != user.getUniqueId())) {
                            for (String message : messages.getStringList("Ban.notify")) {
                                all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", BanSystem.getInstance().getTimeFormatUtil()
                                                .getFormattedRemainingTime(duration))
                                        .replaceAll("%banner%", creatorName)
                                        .replaceAll("%type%", type.toString())
                                        .replaceAll("&", "§"));
                            }
                        }
                    }
                } else
                    user.sendMessage(messages.getString("Ban.ID.NoPermission")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));

            } else {
                System.out.println("Type is null");
            }
            return;
        }

        if (args.length >= 3) {
            user.sendMessage(messages.getString("Ban.usage")
                    .replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
        }
    }

    private boolean hasPermissionForAnyID(User user) {
        for (Integer key : ids) {
            if (user.hasPermission("bansys.ban." + key))
                return true;
        }
        return false;
    }

    private void setParameters(User user, String[] args) throws UnknownHostException {

        // set creator
        if (user.getUniqueId() != null) {
            creator = user.getUniqueId().toString();
            creatorName = user.getDisplayName();
        } else {
            creatorName = user.getName();
            creator = user.getName();
        }

        // set ID
        if (config.getSection("IDs").getKeys().contains(args[1])) {
            String id = args[1];
            //set reason
            reason = config.getString("IDs." + id + ".reason");

            // set lvl
            try {
                if (!isMaxBanLvl(args[1], banmanager.getLevel(uuid, reason))) {
                    lvl = banmanager.getLevel(uuid, reason)+1;
                } else {
                    lvl = getMaxLvl(args[1]);
                }
            } catch (SQLException | ExecutionException | InterruptedException throwables) {
                user.sendMessage(messages.getString("Ban.faild")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                throwables.printStackTrace();
            }


            //set duration
            for (String lvlkey : config.getSection("IDs." + id + ".lvl").getKeys()) {
                if (Integer.parseInt(lvlkey) == lvl) {
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

        if(duration != -1)
            endDate = new Date(System.currentTimeMillis() + duration);

    }

    private boolean isMaxBanLvl(String id, int lvl) {
        int maxLvl = 0;

        for (String key : config.getSection("IDs." + id + ".lvl").getKeys()) {
            if (Integer.parseInt(key) > maxLvl) maxLvl = Integer.parseInt(key);
        }
        return lvl >= maxLvl;
    }

    private int getMaxLvl(String id) {
        return config.getSection("IDs." + id + ".lvl").getKeys().size();
    }
}
