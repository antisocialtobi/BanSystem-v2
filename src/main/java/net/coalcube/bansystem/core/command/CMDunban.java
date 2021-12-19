package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDunban implements Command {

    private final BanManager banmanager;
    private final Database sql;
    private final Config messages;
    private final Config config;

    private UUID uuid;
    private String name;

    public CMDunban(BanManager banmanager, Database sql, Config messages, Config config) {
        this.banmanager = banmanager;
        this.sql = sql;
        this.messages = messages;
        this.config = config;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unban")) {
            if (sql.isConnected()) {
                if (args.length >= 1) {

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
                    try {
                        if (banmanager.isBanned(uuid, Type.NETWORK)) {
                            if (config.getBoolean("needReason.Unban")) {
                                if (args.length > 1) {

                                    StringBuilder reason = new StringBuilder();
                                    for (int i = 1; i < args.length; i++) {
                                        reason.append(args[i]).append(" ");
                                    }

                                    try {
                                        if (user.getUniqueId() != null) {
                                            banmanager.unBan(uuid, user.getUniqueId(), reason.toString());
                                            for(String msg : messages.getStringList("Unban.needreason.notify")) {
                                                msg = msg.replaceAll("%P%", messages.getString("prefix"));
                                                msg = msg.replaceAll("%player%", Objects.requireNonNull(name));
                                                msg = msg.replaceAll("%sender%", user.getName());
                                                msg = msg.replaceAll("%reason%", reason.toString());
                                                msg = msg.replaceAll("&", "§");

                                                BanSystem.getInstance().getConsole().sendMessage(msg);
                                            }
                                        } else {
                                            banmanager.unBan(uuid, user.getName(), reason.toString());
                                        }
                                    } catch (IOException | SQLException e) {
                                        e.printStackTrace();
                                        user.sendMessage(messages.getString("Unban.faild")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                        return;
                                    }

                                    user.sendMessage(messages.getString("Unban.needreason.success")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                            .replaceAll("%reason%", reason.toString())
                                            .replaceAll("&", "§"));
                                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                                        if (all.hasPermission("bansys.notify") && all.getRawUser() != user) {
                                            for(String msg : messages.getStringList("Unban.needreason.notify")) {
                                                msg = msg.replaceAll("%P%", messages.getString("prefix"));
                                                msg = msg.replaceAll("%player%", Objects.requireNonNull(name));
                                                msg = msg.replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName()));
                                                msg = msg.replaceAll("%reason%", reason.toString());
                                                msg = msg.replaceAll("&", "§");

                                                all.sendMessage(msg);
                                            }
                                        }
                                    }
                                    if(user.getUniqueId() != null) {
                                        for(String msg : messages.getStringList("Unban.needreason.notify")) {
                                            msg = msg.replaceAll("%P%", messages.getString("prefix"));
                                            msg = msg.replaceAll("%player%", Objects.requireNonNull(name));
                                            msg = msg.replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName()));
                                            msg = msg.replaceAll("%reason%", reason.toString());
                                            msg = msg.replaceAll("&", "§");

                                            BanSystem.getInstance().getConsole().sendMessage(msg);
                                        }
                                    }
                                } else {
                                    user.sendMessage(messages.getString("Unban.needreason.usage")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            } else {
                                if(args.length == 1) {
                                    try {
                                        if (user.getUniqueId() != null) {
                                            banmanager.unBan(uuid, user.getUniqueId());
                                            banmanager.log("Unbanned Player", user.getUniqueId().toString(), uuid.toString(), "");
                                        } else {
                                            banmanager.unBan(uuid, user.getName());
                                            banmanager.log("Unbanned Player", user.getName(), uuid.toString(), "");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        user.sendMessage(messages.getString("Unban.faild")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                        return;
                                    }
                                    user.sendMessage(
                                            messages.getString("Unban.success")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("&", "§"));
                                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                                        if (all.hasPermission("bansys.notify") && all.getRawUser() != user.getRawUser()) {
                                            all.sendMessage(messages.getString("Unban.notify")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName()))
                                                    .replaceAll("&", "§"));
                                        }
                                    }
                                    if(user.getUniqueId() != null) {
                                        BanSystem.getInstance().getConsole()
                                                .sendMessage(messages.getString("Unban.notify")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                                        .replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName()))
                                                        .replaceAll("&", "§"));
                                    }

                                } else {
                                    user.sendMessage(messages.getString("Unban.usage")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            }
                        } else {
                            user.sendMessage(
                                    messages.getString("Unban.notbanned")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                            .replaceAll("&", "§"));
                        }
                    } catch (SQLException | InterruptedException | ExecutionException throwables) {
                        throwables.printStackTrace();
                    }
                } else {
                    if(!config.getBoolean("needReason.Unban")) {
                        user.sendMessage(messages.getString("Unban.usage")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    } else {
                        user.sendMessage(messages.getString("Unban.needreason.usage")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    }
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
