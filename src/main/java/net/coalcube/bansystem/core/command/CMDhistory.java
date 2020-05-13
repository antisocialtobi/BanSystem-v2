package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.util.*;

import java.util.UUID;

public class CMDhistory implements Command {

    private BanManager banManager;
    private Config messages;
    private MySQL mysql;

    public CMDhistory(BanManager banmanager, Config messages, MySQL mysql) {
        this.banManager = banmanager;
        this.messages = messages;
        this.mysql = mysql;
    }


    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.history.show")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(messages.getString("Playerdoesnotexist")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }
                    if (banManager.hashistory(uuid)) {
                        // send history


                    } else {
                        user.sendMessage(messages.getString("History.historynotfound")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                    }
                } else {
                    user.sendMessage(messages.getString("History.usage")
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
