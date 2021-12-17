package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDhistory implements Command {

    private final BanManager banManager;
    private final Config messages;
    private final Config config;
    private final Database sql;

    public CMDhistory(BanManager banmanager, Config messages, Config config, Database sql) {
        this.banManager = banmanager;
        this.messages = messages;
        this.config = config;
        this.sql = sql;
    }


    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.history.show")) {
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
                        if (banManager.hasHistory(uuid)) {

                            user.sendMessage(messages.getString("History.header")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", Objects.requireNonNull(UUIDFetcher.getName(uuid)))
                                    .replaceAll("&", "§"));

                            user.sendMessage(messages.getString("prefix"));

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                            for(History history : banManager.getHistory(uuid)) {
                                String id = "Not Found";
                                for(String ids : config.getSection("IDs").getKeys()) {
                                    if(config.getString("IDs." + ids + ".reason").equals(history.getReason()))
                                        id = ids;
                                }
                                for(String message : messages.getStringList("History.body")) {
                                    user.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("%reason%", history.getReason())
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%enddate%", simpleDateFormat.format(history.getEndDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%ip%", (history.getIp() == null ? "§cNicht vorhanden" : history.getIp().getHostName()))
                                            .replaceAll("%type%", history.getType().toString())
                                            .replaceAll("%ID%", id)
                                            .replaceAll("&", "§"));
                                }
                            }
                            user.sendMessage(messages.getString("History.footer")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));

                        } else {
                            user.sendMessage(messages.getString("History.historynotfound")
                                    .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        }
                    } catch (UnknownHostException | SQLException | ParseException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("History.usage")
                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
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
