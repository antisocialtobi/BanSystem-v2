package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.sql.SQLException;
import java.util.UUID;

public class CMDkick implements Command {

    private final Config messages;
    private final Database sql;
    private final BanManager banManager;

    private UUID uuid;
    private String name;
    private User target;

    public CMDkick(Config messages, Database sql, BanManager banManager) {
        this.messages = messages;
        this.sql = sql;
        this.banManager = banManager;
    }

    public void noReasonKick(User p, User target) throws SQLException {
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.noreason.screen").replaceAll("&", "§"));
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
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
            p.sendMessage(messages.getString("NoDBConnection")
                    .replaceAll("%P%", messages.getString("prefix")));
        }
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all.getUniqueId() != p.getUniqueId()) {
                for (String message : messages.getStringList("Kick.noreason.notify")) {
                    all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%player%", target.getDisplayName())
                            .replaceAll("%sender%", (p.getUniqueId() != null ? p.getDisplayName() : p.getName()))
                            .replaceAll("&", "§"));
                }
            }
        }
    }

    public void reasonKick(User p, User target, String[] args) throws SQLException {
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
                .replaceAll("%player%", target.getDisplayName()).replaceAll("&", "§"));
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            msg.append(args[i]).append(" ");
        }
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.reason.screen")
                .replaceAll("%P%", messages.getString("prefix"))
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
                for (String message : messages.getStringList("Kick.reason.notify")) {
                    all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%player%", target.getDisplayName())
                            .replaceAll("%sender%", (p.getUniqueId() != null ? p.getDisplayName() : p.getName()))
                            .replaceAll("%reason%", msg.toString())
                            .replaceAll("&", "§"));
                }
            }
        }
    }

    @Override
    public void execute(User p, String[] args) {
        if (p.hasPermission("bansys.kick")) {
            if (args.length >= 1) {
                if(BanSystem.getInstance().getUser(args[0].replaceAll("&", "§")).getUniqueId() != null) {
                    target = BanSystem.getInstance().getUser(args[0].replaceAll("&", "§"));
                } else if(BanSystem.getInstance().getUser(UUID.fromString(args[0])) != null) {
                    target = BanSystem.getInstance().getUser(UUID.fromString(args[0]));
                } else {
                    p.sendMessage(messages.getString("PlayerNotFound")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                    return;
                }

                uuid = target.getUniqueId();
                name = target.getDisplayName();
                if (!target.getUniqueId().equals(p.getUniqueId())) {
                    if ((target.hasPermission("bansys.kick") && !p.hasPermission("bansys.kick.admin"))
                            || (target.hasPermission("bansys.kick.admin") && p.hasPermission("bansys.kick.admin"))) {
                        p.sendMessage(messages.getString("Kick.cannotkick.teammembers")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        return;
                    }
                    if (target.hasPermission("bansys.kick.bypass") && !p.hasPermission("bansys.kick.admin")) {
                        p.sendMessage(messages.getString("Kick.cannotkick.bypass")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        return;
                    }
                    if (args.length == 1) {
                        try {
                            noReasonKick(p, target);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    } else {
                        try {
                            reasonKick(p, target, args);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    }
                } else {
                    p.sendMessage(messages.getString("Kick.cannotkickyouselfe")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                p.sendMessage(messages.getString("Kick.usage")
                        .replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
            }
        } else {
            p.sendMessage(messages.getString("NoPermissionMessage")
                    .replaceAll("%P%", messages.getString("prefix"))
                    .replaceAll("&", "§"));
        }
    }
}
