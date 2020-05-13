package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.util.UUID;

public class CMDunmute implements Command {

    private BanManager bm;
    private Config messages;
    private Config config;
    private MySQL mysql;

    public CMDunmute(BanManager banmanager, Config messages, Config config, MySQL mysql) {
        this.bm = banmanager;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.unmute")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist").replaceAll("%P%",
                                messages.getString("prefix")));
                        return;
                    }
                    if (bm.isBanned(uuid, Type.CHAT)) {
                        if (args.length > 1 && config.getBoolean("needReason.Unmute")) {

                            String reason = "";
                            for (int i = 1; i < args.length; i++) {
                                reason = reason + args[i] + " ";
                            }

                            try {
                                if (user.getUniqueId() != null) {
                                    bm.unmute(uuid, user.getUniqueId(), reason);
                                } else
                                    bm.unmute(uuid, user.getName(), reason);
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
                                    bm.unmute(uuid, user.getUniqueId());
                                } else
                                    bm.unmute(uuid, user.getName());
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
