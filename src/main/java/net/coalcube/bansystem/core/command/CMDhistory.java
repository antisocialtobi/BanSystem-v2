package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDhistory implements Command {

    private final BanManager banmanager;
    private final Config messages;
    private final Config config;
    private final Database sql;

    private UUID uuid;
    private String name;

    public CMDhistory(BanManager banmanager, Config messages, Config config, Database sql) {
        this.banmanager = banmanager;
        this.messages = messages;
        this.config = config;
        this.sql = sql;
    }


    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.history.show")) {
            if (sql.isConnected()) {
                if (args.length == 1) {

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
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }
                    try {
                        if (banmanager.hasHistory(uuid)) {

                            user.sendMessage(messages.getString("History.header")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", Objects.requireNonNull(name))
                                    .replaceAll("&", "§"));

                            user.sendMessage(messages.getString("prefix"));

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(messages.getString("DateTimePattern"));
                            for(History history : banmanager.getHistory(uuid)) {
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
            user.sendMessage(messages.getString("NoPermissionMessage")
                    .replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
        }
    }
}
