package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.User;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDcheck implements Command {

    private final BanManager bm;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;

    public CMDcheck(BanManager banmanager, Database sql, ConfigurationUtil configurationUtil) {
        this.bm = banmanager;
        this.sql = sql;
        this.configurationUtil = configurationUtil;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.check")) {
            if (!sql.isConnected()) {
                try {
                    sql.connect();
                } catch (SQLException ex) {
                    user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
                    return;
                }
            }
            if (args.length == 1) {

                // Set name and uuid
                if (BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                    uuid = BanSystem.getInstance().getUser(args[0]).getUniqueId();
                    name = BanSystem.getInstance().getUser(args[0]).getName();
                } else {
                    try {
                        uuid = UUID.fromString(args[0]);
                        if (UUIDFetcher.getName(uuid) == null) {
                            if (bm.isSavedBedrockPlayer(uuid)) {
                                name = bm.getSavedBedrockUsername(uuid);
                                uuid = bm.getSavedBedrockUUID(name);
                            }
                        } else {
                            name = UUIDFetcher.getName(uuid);
                        }
                    } catch (IllegalArgumentException exception) {
                        if (UUIDFetcher.getUUID(args[0].replaceAll("&", "§")) == null) {
                            try {
                                if (bm.isSavedBedrockPlayer(args[0].replaceAll("&", "§"))) {
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
                    Ban ban = bm.getBan(uuid, Type.NETWORK);
                    Ban mute = bm.getBan(uuid, Type.CHAT);
                    if (mute != null && ban != null) {

                        String player = name;
                        String bannerchat = mute.getCreator();
                        String bannernetwork = ban.getCreator();
                        String reasonchat = mute.getReason();
                        String reasonnetwork = ban.getReason();
                        String reamingtimechat = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(mute.getRemainingTime());
                        String reamingtimenetwork = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(ban.getRemainingTime());
                        String lvlchat = String.valueOf(bm.getLevel(uuid, mute.getReason()));
                        String lvlnetwork = String.valueOf(bm.getLevel(uuid, ban.getReason()));
                        String idchat = mute.getId();
                        String idnetwork = ban.getId();

                        try {
                            if (UUIDFetcher.getName(UUID.fromString(bannerchat)) != null) {
                                bannerchat = UUIDFetcher.getName(UUID.fromString(bannerchat));
                            } else if (bm.isSavedBedrockPlayer(UUID.fromString(bannerchat))) {
                                bannerchat = bm.getSavedBedrockUsername(UUID.fromString(bannerchat));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }

                        try {
                            if (UUIDFetcher.getName(UUID.fromString(bannernetwork)) != null) {
                                bannernetwork = UUIDFetcher.getName(UUID.fromString(bannernetwork));
                            } else if (bm.isSavedBedrockPlayer(UUID.fromString(bannernetwork))) {
                                bannernetwork = bm.getSavedBedrockUsername(UUID.fromString(bannernetwork));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }

                        assert bannerchat != null;
                        assert bannernetwork != null;

                        String networkAndChat = configurationUtil.getMessage("Check.networkandchat")
                                .replaceAll("%player%", player)
                                .replaceAll("%bannerchat%", bannerchat)
                                .replaceAll("%reasonchat%", reasonchat)
                                .replaceAll("%reamingtimechat%", reamingtimechat)
                                .replaceAll("%levelchat%", lvlchat)
                                .replaceAll("%bannernetwork%", bannernetwork)
                                .replaceAll("%reasonnetwork%", reasonnetwork)
                                .replaceAll("%reamingtimenetwork%", reamingtimenetwork)
                                .replaceAll("%levelnetwork%", lvlnetwork)
                                .replaceAll("%idchat%", idchat)
                                .replaceAll("%idnetwork%", idnetwork);
                        if (user.getUniqueId() != null)
                            user.sendMessage(networkAndChat);
                        else
                            BanSystem.getInstance().sendConsoleMessage(networkAndChat);

                    } else if (mute != null) {

                        String player = name;
                        String banner = mute.getCreator();
                        String reason = mute.getReason();
                        String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(mute.getRemainingTime());
                        String lvl = String.valueOf(bm.getLevel(uuid, mute.getReason()));

                        try {
                            if (UUIDFetcher.getName(UUID.fromString(banner)) != null) {
                                banner = UUIDFetcher.getName(UUID.fromString(banner));
                            } else if (bm.isSavedBedrockPlayer(UUID.fromString(banner))) {
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
                                .replaceAll("%id%", mute.getId());

                        if (user.getUniqueId() != null)
                            user.sendMessage(chat);
                        else
                            BanSystem.getInstance().sendConsoleMessage(chat);

                    } else if (ban != null) {

                        String player = name;
                        String banner = ban.getCreator();
                        String reason = ban.getReason();
                        String reamingtime = BanSystem.getInstance().getTimeFormatUtil()
                                .getFormattedRemainingTime(ban.getRemainingTime());
                        int lvl = bm.getLevel(uuid, ban.getReason());

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
                                .replaceAll("%id%", ban.getId());
                        if (user.getUniqueId() != null)
                            user.sendMessage(network);
                        else
                            BanSystem.getInstance().sendConsoleMessage(network);

                    } else {
                        user.sendMessage(configurationUtil.getMessage("Playernotbanned")
                                .replaceAll("%player%", name));
                    }
                } catch (UnknownHostException | SQLException e) {
                    user.sendMessage(configurationUtil.getMessage("Check.failed"));
                    e.printStackTrace();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                user.sendMessage(configurationUtil.getMessage("Check.usage"));
            }
        } else {
            user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }

    /*
    /command   arg0+arg1  | permission
    /check     <player>   | bansys.check
    */


    @Override
    public List<String> suggest(User user, String[] args) {
        if (!user.hasPermission("bansys.check")) {
            return List.of();
        }
        List<String> suggests = new ArrayList<>();
        List<User> players = BanSystem.getInstance().getAllPlayers();

        if (args.length == 0 || args.length == 1) {
            for (User player : players) {
                suggests.add(player.getName());
            }
        }
        return suggests;
    }
}
