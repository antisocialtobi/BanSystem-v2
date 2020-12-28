package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.Database;
import net.coalcube.bansystem.core.util.MySQL;
import net.coalcube.bansystem.core.util.User;

import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CMDbansystem implements Command {

    private final Database sql;
    private final MySQL mySQL;
    private final Config config;
    private final Config messages;

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

                    Map<String, String> helpCommands = new TreeMap<>();

                    helpCommands.put("bansystem help", "Zeigt dir alle Befehle des BanSystems");
                    helpCommands.put("bansystem reload", "Lädt das Plugin neu");
                    helpCommands.put("bansystem version", "Zeigt dir die Version des Plugins");
                    helpCommands.put("bansystem syncids", "Synchronisiere die BanIDs");
                    helpCommands.put("ban §8<§7Spieler§8> §8<§7ID§8>", "Bannt/Muted Spieler");
                    helpCommands.put("kick §8<§7Spieler§8> §8[§7Grund§8]", "Kickt einen Spieler");
                    helpCommands.put("unban §8<§7Spieler§8>", "Entbannt einen Spieler");
                    helpCommands.put("unmute §8<§7Spieler§8>", "Entmuted einen Spieler");
                    helpCommands.put("check §8<§7Spieler§8>", "Prüft ob ein Spieler bestraft ist");
                    helpCommands.put("history §8<§7Spieler§8>", "Zeigt die History von einem Spieler");
                    helpCommands.put("deletehistory §8<§7Spieler§8>", "Löscht die History von einem Spieler");

                    user.sendMessage(messages.getString("bansystem.help.header")
                            .replaceAll("&", "§")
                            .replaceAll("%P%", messages.getString("prefix")));

                    for (int i = 0; i < helpCommands.size(); i++) {

                    }
                    for (Map.Entry<String, String> entry : helpCommands.entrySet()) {
                        String command = entry.getKey();
                        String description = entry.getValue();

                        user.sendMessage(messages.getString("bansystem.help.entry")
                                .replaceAll("&", "§")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("%command%", command)
                                .replaceAll("%description%", description));
                    }

                    user.sendMessage(messages.getString("bansystem.help.footer")
                            .replaceAll("&", "§")
                            .replaceAll("%P%", messages.getString("prefix")));

//                    for (String s : messages.getStringList("bansystem.help")) {
//                        user.sendMessage(s.replaceAll("%P%", messages.getString("prefix"))
//                                        .replaceAll("&", "§"));
//                    }

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
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
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
