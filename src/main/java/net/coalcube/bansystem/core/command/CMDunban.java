package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.util.UUID;

public class CMDunban implements Command {

    private BanManager bm;
    private MySQL mysql;
    private Config messages;
    private Config config;

    public CMDunban(BanManager banmanager, MySQL mysql, Config messages, Config config) {
        this.bm = banmanager;
        this.mysql = mysql;
        this.messages = messages;
        this.config = config;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unban")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }
                    if (bm.isBanned(uuid, Type.NETWORK)) {
                        if (args.length > 1 && config.getBoolean("needReason.Unban")) {

                            String reason = "";
                            for (int i = 1; i < args.length; i++) {
                                reason = reason + args[i] + " ";
                            }

                            try {
                                if (user.getUniqueId() != null) {
                                    bm.unban(uuid, user.getUniqueId(), reason);
                                } else {
                                    bm.unban(uuid, user.getName(), reason);
                                }
                            } catch (IOException e) {
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
                            BanSystem.getInstance().getConsole()
                                    .sendMessage(messages.getString("Unban.needreason.notify")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                            .replaceAll("%sender%", user.getName())
                                            .replaceAll("%reason%", reason));
                        } else {
                            try {
                                if (user.getUniqueId() != null) {
                                    bm.unban(uuid, user.getUniqueId());
                                } else {
                                    bm.unban(uuid, user.getName());
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
                            BanSystem.getInstance().getConsole()
                                    .sendMessage(messages.getString("Unban.notify")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                            .replaceAll("%sender%", user.getName()));

                        }
                    } else {
                        user.sendMessage(
                                messages.getString("Unban.notbanned").replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                    }
                } else {
                    user.sendMessage(messages.getString("Unban.usage").replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection"));
            }
        } else {
            user.sendMessage(messages.getString("NoPermission"));
        }
    }
}
