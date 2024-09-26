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

public class CMDunmute implements Command {

    private final BanManager bm;
    private final YamlDocument config;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;

    public CMDunmute(BanManager banmanager, YamlDocument config, Database sql, ConfigurationUtil configurationUtil) {
        this.bm = banmanager;
        this.config = config;
        this.sql = sql;
        this.configurationUtil = configurationUtil;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unmute")) {
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
                    Ban mute = bm.getBan(uuid, Type.CHAT);
                    if (mute != null) {
                        if (config.getBoolean("needReason.Unmute")) {
                            if (args.length > 1) {

                                StringBuilder reason = new StringBuilder();
                                for (int i = 1; i < args.length; i++) {
                                    reason.append(args[i]).append(" ");
                                }

                                if (user.getUniqueId() != null) {
                                    bm.unBan(mute, user.getUniqueId(), reason.toString());
                                    BanSystem.getInstance().sendConsoleMessage(
                                            configurationUtil.getMessage("Unmute.needreason.notify")
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", user.getName())
                                                    .replaceAll("%reason%", reason.toString())
                                                    .replaceAll("%id%", mute.getId()));
                                } else
                                    bm.unBan(mute, user.getName(), reason.toString());

                                user.sendMessage(configurationUtil.getMessage("Unmute.needreason.success")
                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                        .replaceAll("%reason%", reason.toString())
                                        .replaceAll("%id%", mute.getId()));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all.getUniqueId() != user.getUniqueId()) {
                                        all.sendMessage(configurationUtil.getMessage("Unmute.needreason.notify")
                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                .replaceAll("%sender%", user.getName())
                                                .replaceAll("%reason%", reason.toString())
                                                .replaceAll("%id%", mute.getId()));
                                    }
                                }
                                if (user.getUniqueId() != null) {
                                    BanSystem.getInstance().sendConsoleMessage(
                                            configurationUtil.getMessage("Unmute.needreason.notify")
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", user.getName())
                                                    .replaceAll("%reason%", reason.toString())
                                                    .replaceAll("%id%", mute.getId()));
                                }
                            } else {
                                user.sendMessage(configurationUtil.getMessage("Unmute.needreason.usage"));
                            }
                        } else {
                            if (args.length == 1) {
                                if (user.getUniqueId() != null) {
                                    bm.unBan(mute, user.getUniqueId());
                                    bm.log("Unmuted Player", user.getUniqueId().toString(), uuid.toString(),
                                            "banID: " + mute.getId());
                                } else {
                                    bm.unBan(mute, user.getName());
                                    bm.log("Unmuted Player", user.getName(), uuid.toString(),
                                            "banID" + mute.getId());
                                }

                                user.sendMessage(
                                        configurationUtil.getMessage("Unmute.success")
                                                .replaceAll("%player%", Objects.requireNonNull(name)));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all.getUniqueId() != user.getUniqueId()) {
                                        all.sendMessage(configurationUtil.getMessage("Unmute.notify")
                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                .replaceAll("%sender%", (user.getUniqueId() != null
                                                        ? user.getDisplayName() : user.getName()))
                                                .replaceAll("%id%", mute.getId()));
                                    }
                                }
                                if (user.getUniqueId() != null) {
                                    BanSystem.getInstance().getConsole()
                                            .sendMessage(configurationUtil.getMessage("Unmute.notify")
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", (user.getUniqueId() != null
                                                            ? user.getDisplayName() : user.getName()))
                                                    .replaceAll("%id%", mute.getId()));
                                }
                            } else {
                                user.sendMessage(configurationUtil.getMessage("Unmute.usage"));
                            }
                        }
                    } else {
                        user.sendMessage(
                                configurationUtil.getMessage("Unmute.notmuted")
                                        .replaceAll("%player%", Objects.requireNonNull(name)));
                    }
                } catch (SQLException | InterruptedException | ExecutionException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                if (!config.getBoolean("needReason.Unmute")) {
                    user.sendMessage(configurationUtil.getMessage("Unmute.usage"));
                } else {
                    user.sendMessage(configurationUtil.getMessage("Unmute.needreason.usage"));
                }
            }
        } else {
            user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
        }
    }

    @Override
    public List<String> suggest(User user, String[] args) {
        if (!user.hasPermission("bansys.unmute")) {
            return List.of();
        }
        return BanSystem.getInstance().getCachedMutedPlayerNames();
    }
}
