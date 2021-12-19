package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDunmute implements Command {

    private final BanManager bm;
    private final Config messages;
    private final Config config;
    private final Database sql;

    private UUID uuid;
    private String name;

    public CMDunmute(BanManager banmanager, Config messages, Config config, Database sql) {
        this.bm = banmanager;
        this.messages = messages;
        this.config = config;
        this.sql = sql;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unmute")) {
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
                        user.sendMessage(messages.getString("Playerdoesnotexist").replaceAll("%P%",
                                messages.getString("prefix")));
                        return;
                    }
                    try {
                        if (bm.isBanned(uuid, Type.CHAT)) {
                            if(config.getBoolean("needReason.Unmute")) {
                                if (args.length > 1) {

                                    StringBuilder reason = new StringBuilder();
                                    for (int i = 1; i < args.length; i++) {
                                        reason.append(args[i]).append(" ");
                                    }

                                    try {
                                        if (user.getUniqueId() != null) {
                                            bm.unMute(uuid, user.getUniqueId(), reason.toString());
                                            for(String msg : messages.getStringList("Unmute.needreason.notify")) {
                                                BanSystem.getInstance().getConsole()
                                                        .sendMessage(msg
                                                                .replaceAll("%P%", messages.getString("prefix"))
                                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                                .replaceAll("%sender%", user.getName()).replaceAll("%reason%", reason.toString()));
                                            }
                                        } else
                                            bm.unMute(uuid, user.getName(), reason.toString());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        user.sendMessage(messages.getString("Unmute.faild")
                                                .replaceAll("%P%", messages.getString("prefix")));
                                        return;
                                    }

                                    user.sendMessage(messages.getString("Unmute.needreason.success")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", Objects.requireNonNull(name)).replaceAll("%reason%", reason.toString()));
                                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                                        if (all.hasPermission("bansys.notify") && all.getUniqueId() != user.getUniqueId()) {
                                            for(String msg : messages.getStringList("Unmute.needreason.notify")) {
                                                all.sendMessage(msg
                                                                .replaceAll("%P%", messages.getString("prefix"))
                                                                .replaceAll("%player%", Objects.requireNonNull(name))
                                                                .replaceAll("%sender%", user.getName()).replaceAll("%reason%", reason.toString()));
                                            }
                                        }
                                    }
                                    if(user.getUniqueId() != null) {
                                        for(String msg : messages.getStringList("Unmute.needreason.notify")) {
                                            BanSystem.getInstance().getConsole()
                                                    .sendMessage(msg
                                                            .replaceAll("%P%", messages.getString("prefix"))
                                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                                            .replaceAll("%sender%", user.getName()).replaceAll("%reason%", reason.toString()));
                                        }
                                    }
                                } else {
                                    user.sendMessage(messages.getString("Unmute.needreason.usage")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            } else {
                                if (args.length == 1) {
                                    try {
                                        if (user.getUniqueId() != null) {
                                            bm.unMute(uuid, user.getUniqueId());
                                            bm.log("Unmuted Player", user.getUniqueId().toString(), uuid.toString(), "");
                                        } else {
                                            bm.unMute(uuid, user.getName());
                                            bm.log("Unmuted Player", user.getName(), uuid.toString(), "");
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        user.sendMessage(messages.getString("Unmute.faild")
                                                .replaceAll("%P%", messages.getString("prefix")));
                                        return;
                                    }

                                    user.sendMessage(
                                            messages.getString("Unmute.success").replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%player%", Objects.requireNonNull(name)));
                                    for (User all : BanSystem.getInstance().getAllPlayers()) {
                                        if (all.hasPermission("bansys.notify") && all.getUniqueId() != user.getUniqueId()) {
                                            all.sendMessage(messages.getString("Unmute.notify")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                                    .replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName())).replaceAll("&", "§"));
                                        }
                                    }
                                    if(user.getUniqueId() != null) {
                                        BanSystem.getInstance().getConsole()
                                                .sendMessage(messages.getString("Unmute.notify")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%player%", Objects.requireNonNull(name))
                                                        .replaceAll("%sender%", (user.getUniqueId() != null ? user.getDisplayName() : user.getName())));
                                    }
                                } else {
                                    user.sendMessage(messages.getString("Unmute.usage")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            }
                        } else {
                            user.sendMessage(
                                    messages.getString("Unmute.notmuted")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", Objects.requireNonNull(name))
                                            .replaceAll("&", "§"));
                        }
                    } catch (SQLException | InterruptedException | ExecutionException throwables) {
                        throwables.printStackTrace();
                    }
                } else {
                    if(!config.getBoolean("needReason.Unmute")) {
                        user.sendMessage(messages.getString("Unmute.usage")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    } else {
                        user.sendMessage(messages.getString("Unmute.needreason.usage")
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
