package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDdeletehistory implements Command {

    private final BanManager banmanager;
    private final Config messages;
    private final Database sql;

    public CMDdeletehistory(BanManager banmanager, Config messages, Database sql) {
        this.banmanager = banmanager;
        this.messages = messages;
        this.sql = sql;
    }


    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.history.delete")) {
            if (sql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid;
                    try{
                        uuid = UUID.fromString(args[0]);
                        if(UUIDFetcher.getName(uuid) == null) {
                            uuid = UUIDFetcher.getUUID(args[0].replaceAll("&", "§"));
                        }
                    } catch (IllegalArgumentException exception){
                        uuid = UUIDFetcher.getUUID(args[0].replaceAll("&", "§"));
                    }
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
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
                            user.sendMessage(messages.getString("Deletehistory.success")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", UUIDFetcher.getName(uuid)).replaceAll("&", "§"));
                            for (User all : BanSystem.getInstance().getAllPlayers()) {
                                if (all.hasPermission("bansys.notify") && all.getRawUser() != user.getRawUser()) {
                                    all.sendMessage(messages.getString("Deletehistory.notify")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                            .replaceAll("%sender%",
                                                    (user.getUniqueId() != null ? user.getDisplayName() : user.getName()))
                                            .replaceAll("&", "§"));
                                }
                            }

                            if (user.getUniqueId() != null)
                                BanSystem.getInstance().getConsole()
                                        .sendMessage(messages.getString("Deletehistory.notify")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                                .replaceAll("%sender%",
                                                        (user.getUniqueId() != null ? user.getDisplayName() : user.getName()))
                                                .replaceAll("&", "§"));

                        } else {
                            user.sendMessage(messages.getString("History.historynotfound")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        }
                    } catch (UnknownHostException | SQLException e) {
                        user.sendMessage(messages.getString("Deletehistroy.faild")
                                .replaceAll("%prefix%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("Deletehistory.usage")
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
