package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDcheck implements Command {

    private final BanManager bm;
    private final Database sql;
    private final Config messages;

    private UUID uuid;
    private String name;

    public CMDcheck(BanManager banmanager, Database sql, Config messages) {
        this.bm = banmanager;
        this.sql = sql;
        this.messages = messages;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.check")) {
            if (sql.isConnected()) {
                if (args.length == 1) {;

                    // Set name and uuid
                    if(BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                        uuid = BanSystem.getInstance().getUser(args[0]).getUniqueId();
                        name = BanSystem.getInstance().getUser(args[0]).getName();
                    } else {
                        try {
                            uuid = UUID.fromString(args[0]);
                            if(UUIDFetcher.getName(uuid) == null) {
                                if(bm.isSavedBedrockPlayer(uuid)) {
                                    name = bm.getSavedBedrockUsername(uuid);
                                    uuid = bm.getSavedBedrockUUID(name);
                                }
                            } else {
                                name = UUIDFetcher.getName(uuid);
                            }
                        } catch (IllegalArgumentException exception) {
                            if(UUIDFetcher.getUUID(args[0].replaceAll("&", "§")) == null) {
                                try {
                                    if(bm.isSavedBedrockPlayer(args[0].replaceAll("&", "§"))) {
                                        uuid = bm.getSavedBedrockUUID(args[0].replaceAll("&", "§"));
                                        name = bm.getSavedBedrockUsername(uuid);
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
                        user.sendMessage(
                                messages.getString("Playerdoesnotexist")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));
                        return;
                    }

                    try {
                        if (bm.isBanned(uuid, Type.CHAT) && bm.isBanned(uuid, Type.NETWORK)) {

                            String player = name;
                            String bannerchat = bm.getBanner(uuid, Type.CHAT);
                            String bannernetwork = bm.getBanner(uuid, Type.NETWORK);
                            String reasonchat = bm.getReason(uuid, Type.CHAT);
                            String reasonnetwork = bm.getReason(uuid, Type.NETWORK);
                            String reamingtimechat = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String reamingtimenetwork = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            String lvlchat = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                            String lvlnetwork = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(bannerchat)) != null) {
                                    bannerchat = UUIDFetcher.getName(UUID.fromString(bannerchat));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(bannerchat))) {
                                    bannerchat = bm.getSavedBedrockUsername(UUID.fromString(bannerchat));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(bannernetwork)) != null) {
                                    bannernetwork = UUIDFetcher.getName(UUID.fromString(bannernetwork));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(bannernetwork))) {
                                    bannernetwork = bm.getSavedBedrockUsername(UUID.fromString(bannernetwork));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            for (String m : messages.getStringList("Check.networkandchat")) {
                                assert player != null;
                                assert bannerchat != null;
                                assert bannernetwork != null;
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%bannerchat%", bannerchat)
                                        .replaceAll("%reasonchat%", reasonchat)
                                        .replaceAll("%reamingtimechat%", reamingtimechat)
                                        .replaceAll("%levelchat%", lvlchat)
                                        .replaceAll("%bannernetwork%", bannernetwork)
                                        .replaceAll("%reasonnetwork%", reasonnetwork)
                                        .replaceAll("%reamingtimenetwork%", reamingtimenetwork)
                                        .replaceAll("%levelnetwork%", lvlnetwork)
                                        .replaceAll("&", "§"));
                            }

                        } else if (bm.isBanned(uuid, Type.CHAT)) {

                            String player = name;
                            String banner = bm.getBanner(uuid, Type.CHAT);
                            String reason = bm.getReason(uuid, Type.CHAT);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String lvl = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(banner)) != null) {
                                    banner = UUIDFetcher.getName(UUID.fromString(banner));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(banner))) {
                                    banner = bm.getSavedBedrockUsername(UUID.fromString(banner));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            for (String m : messages.getStringList("Check.chat")) {
                                assert player != null;
                                assert banner != null;
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%banner%", banner)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", reamingtime)
                                        .replaceAll("%level%", lvl)
                                        .replaceAll("%type%", Type.CHAT.toString())
                                        .replaceAll("&", "§"));
                            }

                        } else if (bm.isBanned(uuid, Type.NETWORK)) {

                            String player = name;
                            String banner = bm.getBanner(uuid, Type.NETWORK);
                            String reason = bm.getReason(uuid, Type.NETWORK);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            int lvl = bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK));

                            try {
                                banner = UUIDFetcher.getName(UUID.fromString(banner));
                            } catch (IllegalArgumentException ignored) {
                            }


                            for (String m : messages.getStringList("Check.network")) {
                                assert player != null;
                                assert banner != null;
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%banner%", banner)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", reamingtime)
                                        .replaceAll("%level%", String.valueOf(lvl))
                                        .replaceAll("%type%", Type.NETWORK.toString())
                                        .replaceAll("&", "§"));
                            }

                        } else {
                            user.sendMessage(messages.getString("Playernotbanned")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", name)
                                    .replaceAll("&", "§"));
                        }
                    } catch (UnknownHostException | SQLException | ParseException e) {
                        user.sendMessage(messages.getString("Check.faild")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        e.printStackTrace();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("Check.usage")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
            }
        } else {
            user.sendMessage(messages.getString("NoPermission")
                    .replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
        }
    }
}
