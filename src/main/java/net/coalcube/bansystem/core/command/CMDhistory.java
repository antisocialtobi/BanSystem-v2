package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class CMDhistory implements Command {

    private final BanManager banmanager;
    private final Config config;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;

    public CMDhistory(BanManager banmanager, Config config, Database sql, ConfigurationUtil configurationUtil) {
        this.banmanager = banmanager;
        this.config = config;
        this.sql = sql;
        this.configurationUtil = configurationUtil;
    }


    @Override
    public void execute(User user, String[] args) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(configurationUtil.getMessage("DateTimePattern"));
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
                        user.sendMessage(configurationUtil.getMessage("Playerdoesnotexist"));
                        return;
                    }
                    try {
                        if (banmanager.hasHistory(uuid)) {

                            user.sendMessage(configurationUtil.getMessage("History.header")
                                    .replaceAll("%player%", Objects.requireNonNull(name)));

                            ArrayList<Date> sortDateList = new ArrayList<>();
                            ArrayList<History> sortedHistory = new ArrayList<>();

                            for(History history : banmanager.getHistory(uuid)) {
                                sortDateList.add(history.getCreateDate());
                            }
                            Collections.sort(sortDateList, new Comparator<Date>() {
                                @Override
                                public int compare(Date o1, Date o2) {
                                    return (o1.getTime() > o2.getTime() ? 1 : -1);
                                }
                            });

                            for(Date date : sortDateList) {
                                for(History history : banmanager.getHistory(uuid)) {
                                    if(history.getCreateDate().equals(date))
                                        sortedHistory.add(history);
                                }
                            }

                            for(History history : sortedHistory) {
                                String row = "";

                                if(history.getHistoryType().equals(HistoryType.BAN)) {
                                    String id = "Not Found";
                                    for(String ids : config.getSection("IDs").getKeys()) {
                                        if(config.getString("IDs." + ids + ".reason").equals(history.getReason()))
                                            id = ids;
                                    }
                                    row = configurationUtil.getMessage("History.ban")
                                            .replaceAll("%reason%", history.getReason())
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%enddate%", simpleDateFormat.format(history.getEndDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%ip%", (history.getIp() == null ? "§cNicht vorhanden" : history.getIp().getHostName()))
                                            .replaceAll("%type%", history.getType().toString())
                                            .replaceAll("%ID%", id);
                                } else if(history.getHistoryType().equals(HistoryType.CLEAR)) {
                                    row = configurationUtil.getMessage("History.clearedHistory")
                                            .replaceAll("%reason%", history.getReason())
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator());
                                } else if(history.getHistoryType().equals(HistoryType.KICK)) {
                                    row = configurationUtil.getMessage("History.kick")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator());
                                } else if(history.getHistoryType().equals(HistoryType.KICKWITHREASON)) {
                                    row = configurationUtil.getMessage("History.kickWithReason")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%reason%", history.getReason());
                                } else if(history.getHistoryType().equals(HistoryType.UNMUTE)) {
                                    row = configurationUtil.getMessage("History.unmute")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator());
                                } else if(history.getHistoryType().equals(HistoryType.UNMUTEWITHREASON)) {
                                    row = configurationUtil.getMessage("History.unmuteWithReason")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%reason%", history.getReason());
                                } else if(history.getHistoryType().equals(HistoryType.UNBAN)) {
                                    row = configurationUtil.getMessage("History.unban")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator());
                                } else if(history.getHistoryType().equals(HistoryType.UNBANWITHREASON)) {
                                    row = configurationUtil.getMessage("History.unbanWithReason")
                                            .replaceAll("%creationdate%", simpleDateFormat.format(history.getCreateDate()))
                                            .replaceAll("%creator%", history.getCreator())
                                            .replaceAll("%reason%", history.getReason());
                                }

                                if(user.getUniqueId() != null)
                                    user.sendMessage(row);
                                else
                                    BanSystem.getInstance().sendConsoleMessage(row);
                            }
                            user.sendMessage(configurationUtil.getMessage("History.footer"));

                        } else {
                            user.sendMessage(configurationUtil.getMessage("History.historynotfound"));
                        }
                    } catch (UnknownHostException | SQLException | ParseException | InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(configurationUtil.getMessage("History.usage"));
                }
            } else {
                user.sendMessage(configurationUtil.getMessage("NoDBConnection"));
            }
        } else {
            user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }
}
