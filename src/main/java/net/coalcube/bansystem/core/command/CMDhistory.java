package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class CMDhistory implements Command {

    private final BanManager banManager;
    private final Config messages;
    private final Config config;
    private final MySQL mysql;

    public CMDhistory(BanManager banmanager, Config messages, Config config, MySQL mysql) {
        this.banManager = banmanager;
        this.messages = messages;
        this.config = config;
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
                    try {
                        if (banManager.hasHistory(uuid)) {

                            user.sendMessage(messages.getString("History.header")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                    .replaceAll("§", "&"));

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                            for(History history : banManager.getHistory(uuid)) {
                                String id = "Not Found";
                                for(String ids : config.getSection("IDs").getKeys()) {
                                    if(config.getString("IDs." + ids + ".reason") == history.getReason()) {
                                        id = config.getString(ids);
                                    }
                                }
                                for(String message : messages.getStringList("History.body")) {
                                    message.replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%reason%", history.getReason())
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%enddate%", simpleDateFormat.format(history.getEndDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%ip%", history.getIp().getHostName())
                                            .replaceAll("%type%", history.getType().toString())
                                            .replaceAll("%id%", id)
                                            .replaceAll("&", "§");
                                }
                            }

                            user.sendMessage(messages.getString("History.footer")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("§", "&"));

                        } else {
                            user.sendMessage(messages.getString("History.historynotfound")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        }
                    } catch (UnknownHostException | SQLException e) {
                        e.printStackTrace();
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
