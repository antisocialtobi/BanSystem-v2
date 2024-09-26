package net.coalcube.bansystem.core.command;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CMDunban implements Command {

    private final BanManager banmanager;
    private final Database sql;
    private final YamlDocument config;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;

    public CMDunban(BanManager banmanager, Database sql, YamlDocument config, ConfigurationUtil configurationUtil) {
        this.banmanager = banmanager;
        this.sql = sql;
        this.config = config;
        this.configurationUtil = configurationUtil;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unban")) {
            if (!sql.isConnected()) {
                try {
                    sql.connect();
                } catch (SQLException ex) {
                    user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
                    return;
                }
            }
            if (args.length >= 1) {

                // Set name and uuid
                if (BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                    uuid = BanSystem.getInstance().getUser(args[0]).getUniqueId();
                    name = BanSystem.getInstance().getUser(args[0]).getName();
                } else {
                    try {
                        uuid = UUID.fromString(args[0]);
                        if (UUIDFetcher.getName(uuid) == null) {
                            if (banmanager.isSavedBedrockPlayer(uuid)) {
                                name = banmanager.getSavedBedrockUsername(uuid);
                                uuid = banmanager.getSavedBedrockUUID(name);
                            }
                        } else {
                            name = UUIDFetcher.getName(uuid);
                        }
                    } catch (IllegalArgumentException exception) {
                        if (UUIDFetcher.getUUID(args[0].replaceAll("&", "§")) == null) {
                            try {
                                if (banmanager.isSavedBedrockPlayer(args[0].replaceAll("&", "§"))) {
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
                    Ban ban = banmanager.getBan(uuid, Type.NETWORK);
                    if (ban != null) {
                        if (config.getBoolean("needReason.Unban")) {
                            if (args.length > 1) {

                                StringBuilder reason = new StringBuilder();
                                for (int i = 1; i < args.length; i++) {
                                    reason.append(args[i]).append(" ");
                                }

                                try {
                                    if (user.getUniqueId() != null) {
                                        banmanager.unBan(ban, user.getUniqueId(), reason.toString());
                                        BanSystem.getInstance().sendConsoleMessage(
                                                configurationUtil.getMessage("Unban.needreason.notify")
                                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                                        .replaceAll("%sender%", user.getName())
                                                        .replaceAll("%reason%", reason.toString())
                                                        .replaceAll("%id%", ban.getId()));
                                    } else {
                                        banmanager.unBan(ban, user.getName(), reason.toString());
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                    user.sendMessage(configurationUtil.getMessage("Unban.failed"));
                                    return;
                                }

                                user.sendMessage(configurationUtil.getMessage("Unban.needreason.success")
                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                        .replaceAll("%reason%", reason.toString())
                                        .replaceAll("%id%", ban.getId()));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all.getRawUser() != user) {
                                        all.sendMessage(configurationUtil.getMessage("Unban.needreason.notify")
                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                .replaceAll("%sender%", (user.getUniqueId() != null
                                                        ? user.getDisplayName() : user.getName()))
                                                .replaceAll("%reason%", reason.toString())
                                                .replaceAll("%id%", ban.getId()));
                                    }
                                }
                                if (user.getUniqueId() != null) {
                                    BanSystem.getInstance().sendConsoleMessage(
                                            configurationUtil.getMessage("Unban.needreason.notify")
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", (user.getUniqueId() != null
                                                            ? user.getDisplayName() : user.getName()))
                                                    .replaceAll("%reason%", reason.toString())
                                                    .replaceAll("%id%", ban.getId()));
                                }
                            } else {
                                user.sendMessage(configurationUtil.getMessage("Unban.needreason.usage"));
                            }
                        } else {
                            if (args.length == 1) {
                                if (user.getUniqueId() != null) {
                                    banmanager.unBan(ban, user.getUniqueId());
                                    banmanager.log("Unbanned Player", user.getUniqueId().toString(), uuid.toString(),
                                            "banID: " + ban.getId());
                                } else {
                                    banmanager.unBan(ban, user.getName());
                                    banmanager.log("Unbanned Player", user.getName(), uuid.toString(),
                                            "banID: " + ban.getId());
                                }
                                user.sendMessage(
                                        configurationUtil.getMessage("Unban.success")
                                                .replaceAll("%player%", Objects.requireNonNull(name)));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all.getRawUser() != user.getRawUser()) {
                                        all.sendMessage(configurationUtil.getMessage("Unban.notify")
                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                .replaceAll("%sender%", (user.getUniqueId() != null
                                                        ? user.getDisplayName() : user.getName()))
                                                .replaceAll("%id%", ban.getId()));
                                    }
                                }
                                if (user.getUniqueId() != null) {
                                    BanSystem.getInstance().getConsole()
                                            .sendMessage(configurationUtil.getMessage("Unban.notify")
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", (user.getUniqueId() != null
                                                            ? user.getDisplayName() : user.getName()))
                                                    .replaceAll("%id%", ban.getId()));
                                }

                            } else {
                                user.sendMessage(configurationUtil.getMessage("Unban.usage"));
                            }
                        }
                    } else {
                        user.sendMessage(configurationUtil.getMessage("Unban.notbanned")
                                .replaceAll("%player%", Objects.requireNonNull(name)));
                    }
                } catch (SQLException | InterruptedException | ExecutionException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                if (!config.getBoolean("needReason.Unban")) {
                    user.sendMessage(configurationUtil.getMessage("Unban.usage"));
                } else {
                    user.sendMessage(configurationUtil.getMessage("Unban.needreason.usage"));
                }
            }
        } else {
                user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }

    @Override
    public List<String> suggest(User user, String[] args) {
        if (!user.hasPermission("bansys.unban")) {
            return List.of();
        }
        return BanSystem.getInstance().getCachedBannedPlayerNames();
    }
}
