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
    private final IDManager idManager;
    private final TimeFormatUtil timeFormatUtil;
    private final BanManager banManager;
    private final ConfigurationUtil configurationUtil;

    public CMDbansystem(Config config, Database sql, MySQL mysql, IDManager idManager, TimeFormatUtil timeFormatUtil, BanManager banManager, ConfigurationUtil configurationUtil) {
        this.config = config;
        this.sql = sql;
        this.mySQL = mysql;
        this.idManager = idManager;
        this.timeFormatUtil = timeFormatUtil;
        this.banManager = banManager;
        this.configurationUtil = configurationUtil;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.bansys")) {
            if (args.length == 0) {
                user.sendMessage(configurationUtil.getMessage("bansystem.usage.help"));
                return;
            }
            if (args[0].equalsIgnoreCase("help")) {
                if (args.length == 1)
                    sendHelp(user);
                else {
                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.help"));
                }
            } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")) {
                if (!user.hasPermission("bansys.reload")) {
                    user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                    return;
                }
                if (args.length == 1) {
                    user.sendMessage(configurationUtil.getMessage("bansystem.reload.process"));

                    BanSystem.getInstance().onDisable();
                    BanSystem.getInstance().onEnable();

                    user.sendMessage(configurationUtil.getMessage("bansystem.reload.finished"));
                } else
                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.reload"));
            } else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("ver")) {
                if (args.length == 1) {
                    user.sendMessage(configurationUtil.getMessage("bansystem.version")
                            .replaceAll("%ver%", BanSystem.getInstance().getVersion()));
                } else
                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.version"));
            } else if (args[0].equalsIgnoreCase("syncids")) {
                if (!user.hasPermission("bansys.syncids")) {
                    user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                    return;
                }
                if (args.length == 1) {
                    if (config.getBoolean("mysql.enable")) {
                        if (sql.isConnected()) {
                            try {
                                BanSystem.getInstance().loadConfig();
                                mySQL.syncIDs(config);
                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.sync.success"));

                            } catch (SQLException throwables) {
                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.sync.faild"));
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        } else {
                            user.sendMessage(configurationUtil.getMessage("NoMySQLconnection"));
                        }
                    } else {
                        user.sendMessage(configurationUtil.getMessage("bansystem.ids.sync.MySQLdisabled"));
                    }
                } else
                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.syncids"));
            } else if (args[0].equalsIgnoreCase("ids")) {
                if(args.length < 2) {
                    sendHelp(user);
                    return;
                }
                if (args[1].equalsIgnoreCase("create")) {
                    if (!user.hasPermission("bansys.ids.create")
                            && !user.hasPermission("bansys.ids.*")) {
                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                        return;
                    }
                    if (args.length == 7) {

                        String id = args[2];
                        String reason = args[3].replaceAll("&", "§");
                        boolean onlyAdmins = Boolean.parseBoolean(args[4]);
                        long duration;
                        Type type;

                        try {
                            duration = Long.parseLong(args[5]);
                        } catch (NumberFormatException exception) {
                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.create.invalidDuration")
                                    .replaceAll("%ID%", id));
                            return;
                        }

                        try {
                            type = Type.valueOf(args[6]);
                        } catch (IllegalArgumentException exception) {
                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.create.invalidType")
                                    .replaceAll("%ID%", id));
                            return;
                        }

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

                                String formattedDuration;
                                if(duration == -1)
                                    formattedDuration = timeFormatUtil.getFormattedRemainingTime(duration);
                                else
                                    formattedDuration = timeFormatUtil.getFormattedRemainingTime(duration * 1000);

                                String createSuccess = configurationUtil.getMessage("bansystem.ids.create.success")
                                        .replaceAll("%ID%", id)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%onlyadmins%", onlyAdmins ? configurationUtil.getMessage("true")
                                                : configurationUtil.getMessage("false"))
                                        .replaceAll("%duration%", formattedDuration)
                                        .replaceAll("%type%", type.toString());

                                if(user.getUniqueId() != null)
                                    user.sendMessage(createSuccess);
                                else
                                    BanSystem.getInstance().sendConsoleMessage(createSuccess);

                            } catch (SQLException | IOException e) {
                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.create.failure")
                                        .replaceAll("%ID%", id));
                                e.printStackTrace();
                            }
                        } else {
                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.alreadyexists")
                                    .replaceAll("%ID%", id));
                        }
                    } else {
                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.createid"));
                    }
                } else if (args[1].equalsIgnoreCase("delete")) {
                    if (!user.hasPermission("bansys.ids.deleteid")
                            && !user.hasPermission("bansys.ids.*")) {
                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                        return;
                    }
                    if (args.length == 3) {
                        if (config.getSection("IDs").getKeys().contains(args[2])) {
                            String id = args[2];
                            String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();
                            if (idManager.existsID(id)) {
                                try {
                                    idManager.deleteID(id);
                                    banManager.log("deleted BanID", creator, "",
                                            "id: "+ id);
                                    user.sendMessage(configurationUtil.getMessage("bansystem.ids.delete.success")
                                            .replaceAll("%ID%", id));
                                } catch (SQLException | IOException e) {
                                    user.sendMessage(configurationUtil.getMessage("bansystem.ids.delete.failure")
                                            .replaceAll("%ID%", id));
                                    e.printStackTrace();
                                }
                            } else {
                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                        .replaceAll("%ID%", id));
                            }

                        } else {
                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                    .replaceAll("%ID%", args[2]));
                        }
                    } else {
                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.removeid"));
                    }
                } else if (args[1].equalsIgnoreCase("edit")) {
                    if (args.length >= 3) {
                        if (args.length >= 4 && args[3].equalsIgnoreCase("add")) {
                            if (args.length >= 5 && args[4].equalsIgnoreCase("lvl")) {
                                if (!user.hasPermission("bansys.ids.addlvl")
                                        && !user.hasPermission("bansys.ids.*")) {
                                    user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                    return;
                                }
                                if (args.length == 7) {

                                    String id = args[2];
                                    long duration;
                                    Type type;
                                    String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                    try {
                                        duration = Long.parseLong(args[5]);
                                    } catch (NumberFormatException exception) {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.addlvl.invalidDuration")
                                                .replaceAll("%ID%", id));
                                        return;
                                    }

                                    try {
                                        type = Type.valueOf(args[6]);
                                    } catch (IllegalArgumentException exception) {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.addlvl.invalidType")
                                                .replaceAll("%ID%", id));
                                        return;
                                    }

                                    if (idManager.existsID(id)) {
                                        try {
                                            String formattedDuration;
                                            if(duration != -1)
                                                formattedDuration = timeFormatUtil.getFormattedRemainingTime(duration * 1000);
                                            else
                                                formattedDuration = timeFormatUtil.getFormattedRemainingTime(duration);

                                            idManager.addLvl(id, duration, type, creator);
                                            banManager.log("added BanID-Lvl", creator, "",
                                                    "id: "+ id
                                                            + ", duration: " + duration
                                                            + ", type: " + type);

                                            String addlvlSuccess = configurationUtil.getMessage("bansystem.ids.edit.addlvl.success")
                                                    .replaceAll("%lvl%", String.valueOf(idManager.getLastLvl(id)))
                                                    .replaceAll("%duration%", formattedDuration)
                                                    .replaceAll("%type%", type.toString())
                                                    .replaceAll("%ID%", id);

                                            if(user.getUniqueId() != null)
                                                user.sendMessage(addlvlSuccess);
                                            else
                                                BanSystem.getInstance().sendConsoleMessage(addlvlSuccess);
                                        } catch (SQLException | IOException e) {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.addlvl.failure")
                                                    .replaceAll("%ID%", id));
                                            e.printStackTrace();
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                .replaceAll("%ID%", id));
                                    }
                                } else {
                                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.addlvl"));
                                }
                            } else {
                                sendHelp(user);
                            }
                        } else if (args.length >= 4 && args[3].equalsIgnoreCase("remove")) {
                            if (args.length >= 5 && args[4].equalsIgnoreCase("lvl")) {
                                if (!user.hasPermission("bansys.ids.removelvl")
                                        && !user.hasPermission("bansys.ids.*")) {
                                    user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                    return;
                                }
                                if (args.length == 6) {

                                    String id = args[2];
                                    String lvl = args[5];
                                    String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                    if (idManager.existsID(id)) {
                                        if (idManager.existsLvl(id, lvl)) {
                                            if(lvl.equalsIgnoreCase("1")) {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.removelvl.cannotremovelastlvl")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%lvl%", lvl));
                                                return;
                                            }
                                            try {
                                                idManager.removeLvl(id, lvl);
                                                banManager.log("removed BanID-Lvl", creator, "",
                                                        "id: "+ id
                                                                + ", lvl: " + lvl);
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.removelvl.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%lvl%", lvl));
                                            } catch (SQLException | IOException | ExecutionException | InterruptedException e) {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.removelvl.failure")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%lvl%", lvl));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.lvldoesnotexists")
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("%lvl%", lvl));
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                .replaceAll("%ID%", id)
                                                .replaceAll("%lvl%", lvl));
                                    }
                                } else {
                                    user.sendMessage(configurationUtil.getMessage("bansystem.usage.removelvl"));
                                }
                            } else {
                                sendHelp(user);
                            }
                        } else if (args.length >= 4 && args[3].equalsIgnoreCase("set")) {
                            if (args.length >= 5) {
                                if (args[4].equalsIgnoreCase("lvlduration")) {
                                    if (!user.hasPermission("bansys.ids.setduration")
                                            && !user.hasPermission("bansys.ids.*")) {
                                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                        return;
                                    }
                                    if (args.length == 7) {

                                        String id = args[2];
                                        String lvl = args[5];
                                        long duration;
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();

                                        try {
                                            duration = Long.parseLong(args[6]);
                                        } catch (NumberFormatException exception) {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setlvlduration.invalidDuration")
                                                    .replaceAll("%ID%", id));
                                            return;
                                        }

                                        if (idManager.existsID(id)) {
                                            if (idManager.existsLvl(id, lvl)) {
                                                try {
                                                    idManager.setLvlDuration(id, lvl, duration);
                                                    banManager.log("set banduration", creator, "",
                                                            "id: "+ id
                                                                    + ", duration: " + duration
                                                                    + ", lvl: " + lvl);

                                                    if(duration != -1)
                                                        duration = duration * 1000;

                                                    String formattedDuration = timeFormatUtil.getFormattedRemainingTime(duration);

                                                    String setLvlDurationSuccess = configurationUtil.getMessage("bansystem.ids.edit.setlvlduration.success")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%duration%", formattedDuration)
                                                            .replaceAll("%lvl%", lvl);

                                                    if(user.getUniqueId() != null)
                                                        user.sendMessage(setLvlDurationSuccess);
                                                    else
                                                        BanSystem.getInstance().sendConsoleMessage(setLvlDurationSuccess);

                                                } catch (SQLException | IOException e) {
                                                    user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setlvlduration.failure")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%lvl%", lvl));
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.lvldoesnotexists")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%lvl%", lvl));
                                            }
                                        } else {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                    .replaceAll("%ID%", id)
                                                    .replaceAll("%lvl%", lvl));
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.setlvlduration"));
                                    }
                                } else if (args[4].equalsIgnoreCase("lvltype")) {
                                    if (!user.hasPermission("bansys.ids.settype")
                                            && !user.hasPermission("bansys.ids.*")) {
                                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                        return;
                                    }
                                    if (args.length == 7) {

                                        String id = args[2];
                                        String lvl = args[5];
                                        String creator = user.getUniqueId() != null ? user.getUniqueId().toString() : user.getName();
                                        Type type;

                                        try {
                                            type = Type.valueOf(args[6]);
                                        } catch (IllegalArgumentException exception) {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setlvltype.invalidType")
                                                    .replaceAll("%ID%", id));
                                            return;
                                        }

                                        if (idManager.existsID(id)) {
                                            if (idManager.existsLvl(id, lvl)) {
                                                try {
                                                    idManager.setLvlType(id, lvl, type);
                                                    banManager.log("set bantype", creator, "",
                                                            "id: "+ id
                                                                    + ", type: " + type
                                                                    + ", lvl: " + lvl);

                                                    String setLvlTypeSuccess = configurationUtil.getMessage("bansystem.ids.edit.setlvltype.success")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%lvl%", lvl)
                                                            .replaceAll("%type%", type.toString());

                                                    if(user.getUniqueId() != null)
                                                        user.sendMessage(setLvlTypeSuccess);
                                                    else
                                                        BanSystem.getInstance().sendConsoleMessage(setLvlTypeSuccess);

                                                } catch (SQLException | IOException e) {
                                                    user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setlvltype.failure")
                                                            .replaceAll("%ID%", id)
                                                            .replaceAll("%lvl%", lvl));
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.lvldoesnotexists")
                                                        .replaceAll("%lvl%", lvl)
                                                        .replaceAll("%ID%", id));
                                            }
                                        } else {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                    .replaceAll("%lvl%", lvl)
                                                    .replaceAll("%ID%", id));
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.setlvltype"));
                                    }
                                } else if (args[4].equalsIgnoreCase("onlyadmins")) {
                                    if (!user.hasPermission("bansys.ids.setonlyadmins")
                                            && !user.hasPermission("bansys.ids.*")) {
                                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                        return;
                                    }
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

                                                String setOnlyAdminsSuccess = configurationUtil.getMessage("bansystem.ids.edit.setonlyadmins.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%onlyadmins%", onlyadmins ? configurationUtil.getMessage("true")
                                                                : configurationUtil.getMessage("false"));

                                                if(user.getUniqueId() != null)
                                                    user.sendMessage(setOnlyAdminsSuccess);
                                                else
                                                    BanSystem.getInstance().sendConsoleMessage(setOnlyAdminsSuccess);

                                            } catch (SQLException | IOException e) {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setonlyadmins.failure")
                                                        .replaceAll("%ID%", id));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                    .replaceAll("%ID%", id));
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.setonlyadmins"));
                                    }
                                } else if(args[4].equalsIgnoreCase("reason")) {
                                    if (!user.hasPermission("bansys.ids.setreason")
                                            && !user.hasPermission("bansys.ids.*")
                                            && !user.hasPermission("bansys.*")) {
                                        user.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
                                        return;
                                    }
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

                                                String setReasonSuccess = configurationUtil.getMessage("bansystem.ids.edit.setreason.success")
                                                        .replaceAll("%ID%", id)
                                                        .replaceAll("%reason%", reason);

                                                if(user.getUniqueId() != null)
                                                    user.sendMessage(setReasonSuccess);
                                                else
                                                    BanSystem.getInstance().sendConsoleMessage(setReasonSuccess);
                                            } catch (SQLException | IOException e) {
                                                user.sendMessage(configurationUtil.getMessage("bansystem.ids.edit.setreason.failure")
                                                        .replaceAll("%ID%", id));
                                                e.printStackTrace();
                                            }
                                        } else {
                                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                                    .replaceAll("%ID%", id));
                                        }
                                    } else {
                                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.setreason"));
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
                                onlyAdmins = configurationUtil.getMessage("true");
                            } else
                                onlyAdmins = configurationUtil.getMessage("false");

                            String header = configurationUtil.getMessage("bansystem.ids.edit.show.header")
                                    .replaceAll("%ID%", id)
                                    .replaceAll("%reason%", reason)
                                    .replaceAll("%onlyAdmins%", onlyAdmins)
                                    .replaceAll("%creator%", creator);

                            if(user.getUniqueId() != null)
                                 user.sendMessage(header);
                            else
                                BanSystem.getInstance().sendConsoleMessage(header);

                            for(String lvl : config.getSection("IDs." + id + ".lvl").getKeys()) {
                                long rawduration = config.getLong("IDs." + id + ".lvl." + lvl + ".duration");

                                if(rawduration != -1)
                                    rawduration = rawduration * 1000;

                                String duration = timeFormatUtil.getFormattedRemainingTime(rawduration);
                                String lvls = configurationUtil.getMessage("bansystem.ids.edit.show.lvls")
                                        .replaceAll("%lvl%", lvl)
                                        .replaceAll("%duration%", duration)
                                        .replaceAll("%type%",
                                                config.getString("IDs." + id + ".lvl." + lvl + ".type"))
                                        .replaceAll("%creator%", creator);

                                if(user.getUniqueId() != null)
                                    user.sendMessage(lvls);
                                else
                                    BanSystem.getInstance().sendConsoleMessage(lvls);
                            }

                        } else {
                            user.sendMessage(configurationUtil.getMessage("bansystem.ids.doesnotexists")
                                    .replaceAll("%ID%", id));
                        }
                    } else {
                        user.sendMessage(configurationUtil.getMessage("bansystem.usage.showid"));
                    }
                } else {
                    sendHelp(user);
                }
            } else
                sendHelp(user);
        } else {
            user.sendMessage(configurationUtil.getMessage("prefix") + "§7BanSystem by §eTobi§7.");
            user.sendMessage(configurationUtil.getMessage("prefix") + "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids-spigot-bungeecord.65863/");
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
        helpCommands.put("bansystem ids create §8<§7ID§8> §8<§7Grund§8> §8<§7onlyAdmins§8> §8<§7Dauer§8> §8<§7Type§8>", "Erstellt eine neue Ban-ID");
        helpCommands.put("bansystem ids delete §8<§7ID§8>", "Löscht eine vorhandene Ban-ID");
        helpCommands.put("bansystem ids edit §8<§7ID§8> §eadd lvl §8<§7duration§8> §8<§7type§8>", "Füge ein Ban-lvl einer Ban-ID hinzu");
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

        user.sendMessage(configurationUtil.getMessage("bansystem.help.header"));

        for (Map.Entry<String, String> entry : helpCommands.entrySet()) {
            String command = entry.getKey();
            String description = entry.getValue();

            user.sendMessage(configurationUtil.getMessage("bansystem.help.entry")
                    .replaceAll("%command%", command)
                    .replaceAll("%description%", description));
        }

        user.sendMessage(configurationUtil.getMessage("bansystem.help.footer"));
    }
}
