package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class CMDunban implements Command {

    private final BanManager banManager;
    private final Database sql;
    private final Config messages;
    private final Config config;

    public CMDunban(BanManager banmanager, Database sql, Config messages, Config config) {
        this.banManager = banmanager;
        this.sql = sql;
        this.messages = messages;
        this.config = config;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unban")) {
            if (sql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }
                    try {
                        if (banManager.isBanned(uuid, Type.NETWORK)) {
                            if (args.length > 1 && config.getBoolean("needReason.Unban")) {

                                String reason = "";
                                for (int i = 1; i < args.length; i++) {
                                    reason += args[i] + " ";
                                }

                                try {
                                    if (user.getUniqueId() != null) {
                                        banManager.unBan(uuid, user.getUniqueId(), reason);
                                        BanSystem.getInstance().getConsole()
                                                .sendMessage(messages.getString("Unban.needreason.notify")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                        .replaceAll("%sender%", user.getName())
                                                        .replaceAll("%reason%", reason));
                                    } else {
                                        banManager.unBan(uuid, user.getName(), reason);
                                    }
                                } catch (IOException | SQLException e) {
                                    e.printStackTrace();
                                    user.sendMessage(messages.getString("Unban.faild")
                                            .replaceAll("%P%", messages.getString("prefix")));
                                    return;
                                }

                                user.sendMessage(messages.getString("Unban.needreason.success")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all != user) {
                                        all.sendMessage(messages.getString("Unban.needreason.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName())
                                                .replaceAll("%reason%", reason));
                                    }
                                }

                            } else {
                                if(config.getBoolean("needReason.Unban")) {
                                    user.sendMessage(messages.getString("Unban.needreason.usage")
                                            .replaceAll("%prefix%", messages.getString("prefix")));
                                }
                                try {
                                    if (user.getUniqueId() != null) {
                                        banManager.unBan(uuid, user.getUniqueId());
                                        BanSystem.getInstance().getConsole()
                                                .sendMessage(messages.getString("Unban.notify")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                        .replaceAll("%sender%", user.getName()));
                                    } else {
                                        banManager.unBan(uuid, user.getName());
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    user.sendMessage(messages.getString("Unban.faild")
                                            .replaceAll("%P%", messages.getString("prefix")));
                                    return;
                                }
                                user.sendMessage(
                                        messages.getString("Unban.success").replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid)));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all != user) {
                                        all.sendMessage(messages.getString("Unban.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName()).replaceAll("&", "§"));
                                    }
                                }


                            }
                        } else {
                            user.sendMessage(
                                    messages.getString("Unban.notbanned").replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("Unban.usage").replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection")
                        .replaceAll("&", "§")
                        .replaceAll("%P%", messages.getString("prefix")));
            }
        } else {
            user.sendMessage(messages.getString("NoPermissionMessage").replaceAll("%P%", messages.getString("prefix")));
        }
    }
}
