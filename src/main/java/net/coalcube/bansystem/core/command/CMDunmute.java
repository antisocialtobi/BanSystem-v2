package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class CMDunmute implements Command {

    private final BanManager bm;
    private final Config messages;
    private final Config config;
    private final Database sql;

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
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist").replaceAll("%P%",
                                messages.getString("prefix")));
                        return;
                    }
                    try {
                        if (bm.isBanned(uuid, Type.CHAT)) {
                            if (args.length > 1 && config.getBoolean("needReason.Unmute")) {

                                String reason = "";
                                for (int i = 1; i < args.length; i++) {
                                    reason = reason + args[i] + " ";
                                }

                                try {
                                    if (user.getUniqueId() != null) {
                                        bm.unMute(uuid, user.getUniqueId(), reason);
                                        bm.log("Unmuted Player", user.getUniqueId().toString(), uuid.toString(), "reason: " + reason);
                                    } else
                                        bm.unMute(uuid, user.getName(), reason);
                                    bm.log("Unmuted Player", user.getName(), uuid.toString(), "reason: " + reason);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    user.sendMessage(messages.getString("Unmute.faild")
                                            .replaceAll("%P%", messages.getString("prefix")));
                                    return;
                                }

                                user.sendMessage(messages.getString("Unmute.needreason.success")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("%reason%", reason));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all != user) {
                                        all.sendMessage(messages.getString("Unmute.needreason.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName()).replaceAll("%reason%", reason));
                                    }
                                }
                                BanSystem.getInstance().getConsole()
                                        .sendMessage(messages.getString("Unmute.needreason.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName()).replaceAll("%reason%", reason));
                            } else {
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
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid)));
                                for (User all : BanSystem.getInstance().getAllPlayers()) {
                                    if (all.hasPermission("bansys.notify") && all != user) {
                                        all.sendMessage(messages.getString("Unmute.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName()).replaceAll("&", "§"));
                                    }
                                }
                                BanSystem.getInstance().getConsole()
                                        .sendMessage(messages.getString("Unmute.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%", user.getName()));
                            }
                        } else {
                            user.sendMessage(
                                    messages.getString("Unmute.notmuted").replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("Unmute.usage")
                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection"));
            }
        } else {
            user.sendMessage(messages.getString("NoPermission"));
        }
    }
}
