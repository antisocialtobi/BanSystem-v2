package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDcheck implements Command {

    private final BanManager bm;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    private final SimpleDateFormat simpleDateFormat;

    private UUID uuid;
    private String name;

    public CMDcheck(BanManager banmanager, Database sql, ConfigurationUtil configurationUtil) {
        this.bm = banmanager;
        this.sql = sql;
        this.configurationUtil = configurationUtil;

        simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
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
                        user.sendMessage(configurationUtil.getMessage("Playerdoesnotexist"));
                        return;
                    }

                    try {
                        if (bm.isBanned(uuid, Type.CHAT) && bm.isBanned(uuid, Type.NETWORK)) {

                            String player = name;
                            String bannerChat = bm.getBanner(uuid, Type.CHAT);
                            String bannerNetwork = bm.getBanner(uuid, Type.NETWORK);
                            String reasonChat = bm.getReason(uuid, Type.CHAT);
                            String reasonNetwork = bm.getReason(uuid, Type.NETWORK);
                            String reamingTimeChat = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String reamingTimeNetwork = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            String lvlChat = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                            String lvlNetwork = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));
                            String endDateNetwork = simpleDateFormat.format(bm.getEnd(uuid, Type.NETWORK));
                            String endDateChat = simpleDateFormat.format(bm.getEnd(uuid, Type.CHAT));

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(bannerChat)) != null) {
                                    bannerChat = UUIDFetcher.getName(UUID.fromString(bannerChat));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(bannerChat))) {
                                    bannerChat = bm.getSavedBedrockUsername(UUID.fromString(bannerChat));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(bannerNetwork)) != null) {
                                    bannerNetwork = UUIDFetcher.getName(UUID.fromString(bannerNetwork));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(bannerNetwork))) {
                                    bannerNetwork = bm.getSavedBedrockUsername(UUID.fromString(bannerNetwork));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            assert bannerChat != null;
                            assert bannerNetwork != null;

                            String networkAndChat = configurationUtil.getMessage("Check.networkandchat")
                                    .replaceAll("%player%", player)
                                    .replaceAll("%bannerchat%", bannerChat)
                                    .replaceAll("%reasonchat%", reasonChat)
                                    .replaceAll("%reamingtimechat%", reamingTimeChat)
                                    .replaceAll("%levelchat%", lvlChat)
                                    .replaceAll("%bannernetwork%", bannerNetwork)
                                    .replaceAll("%reasonnetwork%", reasonNetwork)
                                    .replaceAll("%reamingtimenetwork%", reamingTimeNetwork)
                                    .replaceAll("%levelnetwork%", lvlNetwork)
                                    .replaceAll("%enddatenetwork%", endDateNetwork)
                                    .replaceAll("%enddatechat%", endDateChat);
                            if(user.getUniqueId() != null)
                                user.sendMessage(networkAndChat);
                            else
                                BanSystem.getInstance().sendConsoleMessage(networkAndChat);

                        } else if (bm.isBanned(uuid, Type.CHAT)) {

                            String player = name;
                            String banner = bm.getBanner(uuid, Type.CHAT);
                            String reason = bm.getReason(uuid, Type.CHAT);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String lvl = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                            String enddate = simpleDateFormat.format(bm.getEnd(uuid, Type.CHAT));

                            try {
                                if(UUIDFetcher.getName(UUID.fromString(banner)) != null) {
                                    banner = UUIDFetcher.getName(UUID.fromString(banner));
                                } else if(bm.isSavedBedrockPlayer(UUID.fromString(banner))) {
                                    banner = bm.getSavedBedrockUsername(UUID.fromString(banner));
                                }
                            } catch (IllegalArgumentException ignored) {
                            }

                            assert player != null;
                            assert banner != null;

                            String chat = configurationUtil.getMessage("Check.chat")
                                    .replaceAll("%player%", player)
                                    .replaceAll("%banner%", banner)
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", reamingtime)
                                    .replaceAll("%level%", lvl)
                                    .replaceAll("%type%", Type.CHAT.toString())
                                    .replaceAll("%enddate%", enddate);

                            if(user.getUniqueId() != null)
                                user.sendMessage(chat);
                            else
                                BanSystem.getInstance().sendConsoleMessage(chat);

                        } else if (bm.isBanned(uuid, Type.NETWORK)) {

                            String player = name;
                            String banner = bm.getBanner(uuid, Type.NETWORK);
                            String reason = bm.getReason(uuid, Type.NETWORK);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            String enddate = simpleDateFormat.format(bm.getEnd(uuid, Type.NETWORK));
                            int lvl = bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK));


                            try {
                                banner = UUIDFetcher.getName(UUID.fromString(banner));
                            } catch (IllegalArgumentException ignored) {
                            }

                            assert player != null;
                            assert banner != null;

                            String network = configurationUtil.getMessage("Check.network")
                                    .replaceAll("%player%", player)
                                    .replaceAll("%banner%", banner)
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%reamingtime%", reamingtime)
                                    .replaceAll("%level%", String.valueOf(lvl))
                                    .replaceAll("%type%", Type.NETWORK.toString())
                                    .replaceAll("%enddate%", enddate);;
                            if(user.getUniqueId() != null)
                                user.sendMessage(network);
                            else
                                BanSystem.getInstance().sendConsoleMessage(network);

                        } else {
                            user.sendMessage(configurationUtil.getMessage("Playernotbanned")
                                    .replaceAll("%player%", name));
                        }
                    } catch (UnknownHostException | SQLException | ParseException e) {
                        user.sendMessage(configurationUtil.getMessage("Check.faild"));
                        e.printStackTrace();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(configurationUtil.getMessage("Check.usage"));
                }
            } else {
                user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
            }
        } else {
            user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }
}
