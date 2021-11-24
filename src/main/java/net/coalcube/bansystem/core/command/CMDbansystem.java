package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
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
    private final IDManager idManager;
    private final TimeFormatUtil timeFormatUtil;
    private final BanManager banManager;

    public CMDbansystem(Config messages, Config config, Database sql, MySQL mysql, IDManager idManager, TimeFormatUtil timeFormatUtil, BanManager banManager) {
        this.config = config;
        this.messages = messages;
        this.sql = sql;
        this.mySQL = mysql;
        this.idManager = idManager;
        this.timeFormatUtil = timeFormatUtil;
        this.banManager = banManager;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.bansys")) {
            if (args.length == 0) {
                user.sendMessage(messages.getString("bansystem.usage.help")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
                return;
            }
            if (args[0].equalsIgnoreCase("help")) {
                if (args.length == 1)
                    sendHelp(user);
                else {
                    user.sendMessage(messages.getString("bansystem.usage.help")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
                if (args.length == 1) {
                    user.sendMessage(messages.getString("bansystem.reload.process")
                            .replaceAll("%P%", messages.getString("prefix")
                                    .replaceAll("&", "§")));

                    BanSystem.getInstance().onDisable();
                    BanSystem.getInstance().onEnable();

                    user.sendMessage(messages.getString("bansystem.reload.finished")
                            .replaceAll("%P%", messages.getString("prefix")
                                    .replaceAll("&", "§")));
                } else
                    user.sendMessage(messages.getString("bansystem.usage.reload")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
            } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                if (args.length == 1) {
                    user.sendMessage(messages.getString("bansystem.version")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%ver%", BanSystem.getInstance().getVersion())
                            .replaceAll("&", "§"));
                } else
                    user.sendMessage(messages.getString("bansystem.usage.version")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
            } else if (args[0].equalsIgnoreCase("syncids")) {
                if (args.length == 1) {
                    if (config.getBoolean("mysql.enable")) {
                        if (sql.isConnected()) {
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
                            } catch (InterruptedException | ExecutionException e) {
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
                } else
                    user.sendMessage(messages.getString("bansystem.usage.syncids")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
            } else if (args[0].equalsIgnoreCase("ids")) {
                if (args[1].equalsIgnoreCase("create")) {
                    if (args.length == 7) {

                        String id = args[2];
                        String reason = args[3].replaceAll("&", "§");
                        boolean onlyAdmins = Boolean.parseBoolean(args[4]);
                        long duration = Long.parseLong(args[5]);
                        Type type = Type.valueOf(args[6]);
                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                        if (!idManager.existsID(id)) {
                            try {
                                idManager.createID(id, reason, onlyAdmins, duration, type, creator);
                                banManager.log("created BanID", creator, "",
                                        "id: "+ id
                                        + ", reason: " + reason
                                        + ", duration: " + duration
                                        + ", type: " + type
                                        + ", onlyAdmins: " + onlyAdmins);
                                user.sendMessage(messages.getString("bansystem.ids.create.success")
                                        .replaceAll("%ID%", id)
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));
                            } catch (SQLException | IOException e) {
                                user.sendMessage(messages.getString("bansystem.ids.create.failure")
                                        .replaceAll("%ID%", id)
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§"));
                                e.printStackTrace();
                            }
                        } else {
                            user.sendMessage(messages.getString("bansystem.ids.alreadyexists")
                                    .replaceAll("%ID%", id)
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("&", "§"));
                        }
                    } else {
                        user.sendMessage(messages.getString("bansystem.usage.addid")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    }
                } else if (args[1].equalsIgnoreCase("delete")) {
                    if (args.length == 3) {
                        if (config.getSection("IDs").getKeys().contains(args[2])) {
                            String id = args[2];
                            String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();
                            if (idManager.existsID(id)) {
                                try {
                                    idManager.deleteID(id);
                                    banManager.log("deleted BanID", creator, "",
                                            "id: "+ id);
                                    user.sendMessage(messages.getString("bansystem.ids.delete.success")
                                            .replaceAll("%ID%", id)
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                } catch (SQLException | IOException e) {
                                    user.sendMessage(messages.getString("bansystem.ids.delete.failure")
                                            .replaceAll("%ID%", id)
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                    e.printStackTrace();
                                }
                            } else {
                                user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                        .replaceAll("%ID%", id)
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%ID%", id)
                                        .replaceAll("&", "§"));
                            }

                        }
                    } else {
                        user.sendMessage(messages.getString("bansystem.usage.removeid")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    }
                } else if (args[1].equalsIgnoreCase("edit")) {
                    if (args.length >= 3) {
                        if (args[3].equalsIgnoreCase("add")) {
                            if (args[4].equalsIgnoreCase("lvl")) {
                                if (args.length == 7) {

                                    String id = args[2];
                                    long duration = Long.parseLong(args[5]);
                                    Type type = Type.valueOf(args[6]);
                                    String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                    if (idManager.existsID(id)) {
                                        try {
                                            idManager.addLvl(id, duration, type, user.getUniqueId().toString());
                                            banManager.log("added BanID-Lvl", creator, "",
                                                    "id: "+ id
                                                            + ", duration: " + duration
                                                            + ", type: " + type);
                                            user.sendMessage(messages.getString("bansystem.ids.edit.addlvl.success")
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("&", "§"));
                                        } catch (SQLException | IOException e) {
                                            user.sendMessage(messages.getString("bansystem.ids.edit.addlvl.failure")
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("&", "§"));
                                            e.printStackTrace();
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                .replaceAll("%ID%", id)
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%ID%", id)
                                                .replaceAll("&", "§"));
                                    }
                                } else {
                                    user.sendMessage(messages.getString("bansystem.usage.addlvl")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            } else {
                                sendHelp(user);
                            }
                        } else if (args[3].equalsIgnoreCase("remove")) {
                            if (args[4].equalsIgnoreCase("lvl")) {
                                if (args.length == 6) {

                                    String id = args[2];
                                    String lvl = args[5];
                                    String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                    if (idManager.existsID(id)) {
                                        if (idManager.existsLvl(id, lvl)) {
                                            try {
                                                idManager.removeLvl(id, lvl);
                                                banManager.log("removed BanID-Lvl", creator, "",
                                                        "id: "+ id
                                                                + ", lvl: " + lvl);
                                                user.sendMessage(messages.getString("bansystem.ids.edit.removelvl.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("&", "§"));
                                            } catch (SQLException | IOException e) {
                                                user.sendMessage(messages.getString("bansystem.ids.edit.removelvl.failure")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("&", "§"));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(messages.getString("bansystem.ids.lvldoesnotexists")
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("&", "§"));
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                .replaceAll("%ID%", id)
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("%ID%", id)
                                                .replaceAll("&", "§"));
                                    }
                                } else {
                                    user.sendMessage(messages.getString("bansystem.usage.removelvl")
                                            .replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§"));
                                }
                            } else {
                                sendHelp(user);
                            }
                        } else if (args[3].equalsIgnoreCase("set")) {
                            if (args.length >= 5) {
                                if (args[4].equalsIgnoreCase("lvlduration")) {
                                    if (args.length == 7) {

                                        String id = args[2];
                                        String lvl = args[5];
                                        long duration = Long.parseLong(args[6]);
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                        if (idManager.existsID(id)) {
                                            if (idManager.existsLvl(id, lvl)) {
                                                try {
                                                    idManager.setLvlDuration(id, lvl, duration);
                                                    banManager.log("set banduration", creator, "",
                                                            "id: "+ id
                                                                    + ", duration: " + duration
                                                                    + ", lvl: " + lvl);
                                                    user.sendMessage(messages.getString("bansystem.ids.edit.setlvlduration.success")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%P%", messages.getString("prefix"))
                                                            .replaceAll("&", "§"));
                                                } catch (SQLException | IOException e) {
                                                    user.sendMessage(messages.getString("bansystem.ids.edit.setlvlduration.failure")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%P%", messages.getString("prefix"))
                                                            .replaceAll("&", "§"));
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                user.sendMessage(messages.getString("bansystem.ids.lvldoesnotexists")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("&", "§"));
                                            }
                                        } else {
                                            user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("&", "§"));
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.usage.setlvlduration")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                    }
                                } else if (args[4].equalsIgnoreCase("lvltype")) {
                                    if (args.length == 7) {

                                        String id = args[2];
                                        String lvl = args[5];
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();
                                        Type type = Type.valueOf(args[6]);

                                        if (idManager.existsID(id)) {
                                            if (idManager.existsLvl(id, lvl)) {
                                                try {
                                                    idManager.setLvlType(id, lvl, type);
                                                    banManager.log("set bantype", creator, "",
                                                            "id: "+ id
                                                                    + ", type: " + type
                                                                    + ", lvl: " + lvl);
                                                    user.sendMessage(messages.getString("bansystem.ids.edit.setlvltype.success")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%P%", messages.getString("prefix"))
                                                            .replaceAll("&", "§"));
                                                } catch (SQLException | IOException e) {
                                                    user.sendMessage(messages.getString("bansystem.ids.edit.setlvltype.failure")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%P%", messages.getString("prefix"))
                                                            .replaceAll("&", "§"));
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                user.sendMessage(messages.getString("bansystem.ids.lvldoesnotexists")
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("&", "§"));
                                            }
                                        } else {
                                            user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("&", "§"));
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.usage.setlvltype")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                    }
                                } else if (args[4].equalsIgnoreCase("onlyadmins")) {
                                    if (args.length == 6) {

                                        String id = args[2];
                                        boolean onlyadmins = Boolean.parseBoolean(args[5]);
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                        if (idManager.existsID(id)) {
                                            try {
                                                idManager.setOnlyAdmins(id, onlyadmins);
                                                banManager.log("set banonlyadmins", creator, "",
                                                        "id: " + id
                                                                + ", onlyAdmins: " + onlyadmins);
                                                user.sendMessage(messages.getString("bansystem.ids.edit.setonlyadmins.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("&", "§"));
                                            } catch (SQLException | IOException e) {
                                                user.sendMessage(messages.getString("bansystem.ids.edit.setonlyadmins.failure")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("&", "§"));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("&", "§"));
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.usage.setonlyadmins")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                    }
                                } else if(args[4].equalsIgnoreCase("reason")) {
                                    if (args.length == 6) {

                                        String id = args[2];
                                        String reason = args[5].replaceAll("&", "§");
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                        if (idManager.existsID(id)) {
                                            try {
                                                idManager.setReason(id, reason);
                                                banManager.log("set banreason", creator, "",
                                                        "id: " + id
                                                                + ", reason: " + reason);
                                                user.sendMessage(messages.getString("bansystem.ids.edit.setreason.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("%reason%", reason)
                                                        .replaceAll("&", "§"));
                                            } catch (SQLException | IOException e) {
                                                user.sendMessage(messages.getString("bansystem.ids.edit.setreason.failure")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%P%", messages.getString("prefix"))
                                                        .replaceAll("&", "§"));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                                    .replaceAll("%P%", messages.getString("prefix"))
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("&", "§"));
                                        }
                                    } else {
                                        user.sendMessage(messages.getString("bansystem.usage.setreason")
                                                .replaceAll("%P%", messages.getString("prefix"))
                                                .replaceAll("&", "§"));
                                    }
                                } else {
                                    sendHelp(user);
                                }
                            } else {
                                sendHelp(user);
                            }
                        } else {
                            sendHelp(user);
                        }
                    } else {
                        sendHelp(user);
                    }
                } else if (args[1].equalsIgnoreCase("show")) {
                    if (args.length == 3) {

                        String id = args[2];
                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                        if (idManager.existsID(id)) {

                            String reason = config.getString("IDs." + id + ".reason");
                            String onlyAdmins;
                            if(config.getBoolean("IDs." + id + ".onlyAdmins")) {
                                onlyAdmins = messages.getString("true")
                                        .replaceAll("&", "§");
                            } else
                                onlyAdmins = messages.getString("false")
                                        .replaceAll("&", "§");

                            for(String line : messages.getStringList("bansystem.ids.edit.show.header")) {
                                user.sendMessage(line.replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("&", "§")
                                        .replaceAll("%ID%", id)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%onlyAdmins%", onlyAdmins)
                                        .replaceAll("%creator%", creator));
                            }

                            for(String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
                                long rawduration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");

                                if(rawduration != -1)
                                    rawduration = rawduration * 1000;

                                String duration = timeFormatUtil.getFormattedRemainingTime(rawduration);

                                for(String line : messages.getStringList("bansystem.ids.edit.show.lvls")) {
                                    user.sendMessage(line.replaceAll("%P%", messages.getString("prefix"))
                                            .replaceAll("&", "§")
                                            .replaceAll("%lvl%", lvl)
                                            .replaceAll("%duration%", duration)
                                            .replaceAll("%type%",
                                                    config.getString("IDs." + id + ".lvl." + lvl + ".type"))
                                            .replaceAll("%creator%", creator));
                                }
                            }

                        } else {
                            user.sendMessage(messages.getString("bansystem.ids.doesnotexists")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%ID%", id)
                                    .replaceAll("&", "§"));
                        }
                    } else {
                        user.sendMessage(messages.getString("bansystem.usage.showid")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                    }
                } else {
                    sendHelp(user);
                }
            } else
                sendHelp(user);
        } else {
            user.sendMessage(messages.getString("prefix") + "§7BanSystem by §eTobi§7.");
            user.sendMessage(messages.getString("prefix") + "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids-spigot-bungeecord.65863/");
            if (user.getUniqueId().equals(UUID.fromString("617f0c2b-6014-47f2-bf89-fade1bc9bb59"))) {
                user.sendMessage("HU du bist ja Tobi :)");
            }

        }
    }

    private void sendHelp(User user) {
        Map<String, String> helpCommands = new TreeMap<>();

        helpCommands.put("bansystem help", "Zeigt dir alle Befehle des BanSystems");
        helpCommands.put("bansystem reload", "Lädt das Plugin neu");
        helpCommands.put("bansystem version", "Zeigt dir die Version des Plugins");
        helpCommands.put("bansystem syncids", "Synchronisiere die BanIDs");
        helpCommands.put("bansystem ids add §8<§7ID§8> §8<§7Grund§8> §8<§7onlyAdmins§8> §8<§7Dauer§8> §8<§7Type§8>", "Erstellt eine neue Ban-ID");
        helpCommands.put("bansystem ids delete §8<§7ID§8>", "Löscht eine vorhandene Ban-ID");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §ecreate lvl §8<§7duration§8> §8<§7type§8>", "Füge ein Ban-lvl einer Ban-ID hinzu");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eremove lvl §8<§7lvl§8>", "Lösche eine vorhandene Ban-lvl");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eset lvlduration §8<§7lvl§8> §8<§7duration§8>", "Ändere die Dauer von einem Ban-lvl");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eset lvltype  §8<§7lvl§8> §8<§7type§8>", "Ändere die art von einem Ban-lvl");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eset reason §8<§7reason§8>", "Ändere den Grund von einer Ban-ID");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eset onlyadmins §8<§7True§8/§7False§8>", "");
        helpCommands.put("bansystem ids show §8<§7ID§8>", "Zeige alle informationen über eine Ban-ID");
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
    }
}
