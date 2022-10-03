package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDdeletehistory implements Command {

    private final BanManager banmanager;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;

    public CMDdeletehistory(BanManager banmanager, Database sql, ConfigurationUtil configurationUtil) {
        this.banmanager = banmanager;
        this.sql = sql;
        this.configurationUtil = configurationUtil;
    }


    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.history.delete")) {
            if (sql.isConnected()) {
                if (args.length == 1) {

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
                        user.sendMessage(configurationUtil.getMessage("Playerdoesnotexist"));
                        return;
                    }
                    try {
                        if (banmanager.hasHistory(uuid)) {
                            banmanager.deleteHistory(uuid);
                            if(user.getUniqueId() != null) {
                                banmanager.log("Deleted History", user.getUniqueId().toString(), uuid.toString(), "");
                            } else {
                                banmanager.log("Deleted History", user.getName(), uuid.toString(), "");

                            }
                            user.sendMessage(configurationUtil.getMessage("Deletehistory.success")
                                    .replaceAll("%player%", Objects.requireNonNull(name)).replaceAll("&", "§"));
                            for (User all : BanSystem.getInstance().getAllPlayers()) {
                                if (all.hasPermission("bansys.notify") && all.getRawUser() != user.getRawUser()) {
                                    all.sendMessage(configurationUtil.getMessage("Deletehistory.notify")
                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                            .replaceAll("%sender%",
                                                    (user.getUniqueId() != null ? user.getDisplayName() : user.getName())));
                                }
                            }

                            if (user.getUniqueId() != null)
                                BanSystem.getInstance().getConsole()
                                        .sendMessage(configurationUtil.getMessage("Deletehistory.notify")
                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                .replaceAll("%sender%",
                                                        (user.getUniqueId() != null ? user.getDisplayName() : user.getName())));

                        } else {
                            user.sendMessage(configurationUtil.getMessage("History.historynotfound"));
                        }
                    } catch (UnknownHostException | SQLException e) {
                        user.sendMessage(configurationUtil.getMessage("Deletehistroy.faild"));
                        e.printStackTrace();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(configurationUtil.getMessage("Deletehistory.usage"));
                }
            } else {
                user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
            }
        } else {
            user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }
}
