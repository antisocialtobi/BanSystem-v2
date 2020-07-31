package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.Database;
import net.coalcube.bansystem.core.util.MySQL;
import net.coalcube.bansystem.core.util.User;

import java.sql.SQLException;
import java.util.UUID;

public class CMDbansystem implements Command {

    private Database sql;
    private MySQL mySQL;
    private Config config;
    private Config messages;

    public CMDbansystem(Config messages, Config config, Database sql, MySQL mysql) {
        this.config = config;
        this.messages = messages;
        this.sql = sql;
        this.mySQL = mysql;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.bansys")) {
            if (args.length == 0) {
                user.sendMessage(messages.getString("bansystem.usage")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("help")) {
                    for (String s : messages.getStringList("bansystem.help")) {
                        user.sendMessage(s.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));
                    }

                } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
                    user.sendMessage(messages.getString("bansystem.reload.process")
                                    .replaceAll("%P%", messages.getString("prefix")
                                    .replaceAll("&", "§")));

                    BanSystem.getInstance().onDisable();
                    BanSystem.getInstance().onEnable();

                    user.sendMessage(messages.getString("bansystem.reload.finished")
                            .replaceAll("%P%", messages.getString("prefix")
                                    .replaceAll("&", "§")));

                } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                    user.sendMessage(messages.getString("bansystem.version")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%ver%", BanSystem.getInstance().getVersion())
                            .replaceAll("&", "§"));
                } else if(args[0].equalsIgnoreCase("syncids")) {
                    if(config.getBoolean("mysql.enable")) {
                        if(sql.isConnected()) {
                            try {
                                BanSystem.getInstance().loadConfig();
                                mySQL.syncIDs(config);
                                user.sendMessage(messages.getString("bansystem.ids.sync.success")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));

                            } catch (SQLException throwables) {
                                user.sendMessage(messages.getString("bansystem.ids.sync.faild")
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));
                            }
                        } else {
                            user.sendMessage(messages.getString("NoMySQLconnection")
                                    .replaceAll("%P%", messages.getString("prefix")));
                        }
                    } else {
                        user.sendMessage(messages.getString("bansystem.ids.sync.MySQLdisabled")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    }
                } else {
                    user.sendMessage(messages.getString("bansystem.usage")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("bansystem.usage")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
            }
        } else {
            user.sendMessage(messages.getString("prefix") + "§7BanSystem by §eTobi§7.");
            if(user.getUniqueId().equals(UUID.fromString("617f0c2b-6014-47f2-bf89-fade1bc9bb59"))) {
                user.sendMessage("HU du bist ja Tobi :)");
            }

        }
    }
}
