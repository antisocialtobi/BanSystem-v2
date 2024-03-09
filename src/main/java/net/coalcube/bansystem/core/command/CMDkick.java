package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.sql.SQLException;
import java.util.UUID;

public class CMDkick implements Command {

    private final Database sql;
    private final BanManager banManager;
    private final ConfigurationUtil configurationUtil;

    private UUID uuid;
    private String name;
    private User target;

    public CMDkick(Database sql, BanManager banManager, ConfigurationUtil configurationUtil) {
        this.sql = sql;
        this.banManager = banManager;
        this.configurationUtil = configurationUtil;
    }

    public void kick(User p, User target) throws SQLException {
        BanSystem.getInstance().disconnect(target, configurationUtil.getMessage("Kick.noreason.screen"));
        p.sendMessage(configurationUtil.getMessage("Kick.success")
                .replaceAll("%player%", target.getDisplayName()).replaceAll("&", "§"));
        if (sql.isConnected()) {
            if (p.getUniqueId() == null) {
                banManager.kick(target.getUniqueId(), p.getName());
                banManager.log("Kicked Player", p.getName(), target.getUniqueId().toString(), "");
            } else {
                banManager.kick(target.getUniqueId(), p.getUniqueId());
                banManager.log("Kicked Player", p.getUniqueId().toString(), target.getUniqueId().toString(), "");
            }
        } else {
            p.sendMessage(configurationUtil.getMessage("NoDBConnection"));
        }
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all.getUniqueId() != p.getUniqueId()) {
                all.sendMessage(configurationUtil.getMessage("Kick.noreason.notify")
                        .replaceAll("%player%", target.getDisplayName())
                        .replaceAll("%sender%", (p.getUniqueId() != null ? p.getDisplayName() : p.getName())));
            }
        }
    }

    public void kick(User p, User target, String[] args) throws SQLException {
        p.sendMessage(configurationUtil.getMessage("Kick.success")
                .replaceAll("%player%", target.getDisplayName()).replaceAll("&", "§"));
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msg.append(args[i].replaceAll("&", "§")).append(" ");
        }
        BanSystem.getInstance().disconnect(target, configurationUtil.getMessage("Kick.reason.screen")
                .replaceAll("%reason%", msg.toString()));

        if (p.getUniqueId() == null) {
            banManager.kick(target.getUniqueId(), p.getName(), msg.toString());
            banManager.log("Kicked Player", p.getName(), target.getUniqueId().toString(), "reason: "+ msg);
        } else {
            banManager.kick(target.getUniqueId(), p.getUniqueId(), msg.toString());
            banManager.log("Kicked Player", p.getUniqueId().toString(), target.getUniqueId().toString(), "reason: "+ msg);
        }
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all.getUniqueId() != p.getUniqueId()) {
                all.sendMessage(configurationUtil.getMessage("Kick.reason.notify")
                        .replaceAll("%player%", target.getDisplayName())
                        .replaceAll("%sender%", (p.getUniqueId() != null ? p.getDisplayName() : p.getName()))
                        .replaceAll("%reason%", msg.toString()));
            }
        }
    }

    @Override
    public void execute(User p, String[] args) {
        if (p.hasPermission("bansys.kick") || p.hasPermission("bansys.kick.admin")) {
            if (args.length >= 1) {
                if(BanSystem.getInstance().getUser(args[0].replaceAll("&", "§")).getUniqueId() != null) {
                    target = BanSystem.getInstance().getUser(args[0].replaceAll("&", "§"));
                } else {
                    try {
                        if(BanSystem.getInstance().getUser(UUID.fromString(args[0])) != null) {
                            target = BanSystem.getInstance().getUser(UUID.fromString(args[0]));
                        } else {
                            p.sendMessage(configurationUtil.getMessage("PlayerNotFound"));
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        p.sendMessage(configurationUtil.getMessage("PlayerNotFound"));
                        return;
                    }
                }

                uuid = target.getUniqueId();
                name = target.getDisplayName();
                if (!target.getUniqueId().equals(p.getUniqueId())) {

                    if(((target.hasPermission("bansys.kick") && !p.hasPermission("bansys.kick.admin"))
                            || target.hasPermission("bansys.kick.admin")) && p.getUniqueId() != null) {
                        p.sendMessage(configurationUtil.getMessage("Kick.cannotkick.teammembers"));
                        return;
                    }
                    if (target.hasPermission("bansys.kick.bypass") && !p.hasPermission("bansys.kick.admin")) {
                        p.sendMessage(configurationUtil.getMessage("Kick.cannotkick.bypass"));
                        return;
                    }
                    if (args.length == 1) {
                        try {
                            kick(p, target);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    } else {
                        try {
                            kick(p, target, args);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                } else {
                    p.sendMessage(configurationUtil.getMessage("Kick.cannotkick.youselfe"));
                }
            } else {
                p.sendMessage(configurationUtil.getMessage("Kick.usage"));
            }
        } else {
            p.sendMessage(configurationUtil.getMessage("NoPermissionMessage"));
        }
    }
}
