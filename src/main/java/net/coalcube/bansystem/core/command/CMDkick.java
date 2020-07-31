package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.sql.SQLException;

public class CMDkick implements Command {

    private final Config messages;
    private final Database sql;
    private final BanManager banManager;

    public CMDkick(Config messages, Database sql, BanManager banManager) {
        this.messages = messages;
        this.sql = sql;
        this.banManager = banManager;
    }

    public void noReasonKick(User p, User target) throws SQLException {
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.noreason.screen").replaceAll("&", "§"));
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
                .replaceAll("%player%", target.getName()).replaceAll("&", "§"));
        if (sql.isConnected()) {
            if (p.getUniqueId() == null)
                banManager.kick(target.getUniqueId(), p.getName());
            else
                banManager.kick(target.getUniqueId(), p.getUniqueId());
        } else {
            p.sendMessage(messages.getString("NoDBConnection").replaceAll("%P%", messages.getString("prefix")));
        }
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all != p) {
                for (String message : messages.getStringList("Kick.noreason.notify")) {
                    all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%player%", target.getName()).replaceAll("%sender%", p.getName())
                            .replaceAll("&", "§"));
                }
            }
        }
    }

    public void reasonKick(User p, User target, String[] args) throws SQLException {
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
                .replaceAll("%player%", target.getName()).replaceAll("&", "§"));
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            StringBuilder append = msg.append(args[i]).append(" ");
        }
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.reason.screen")
                .replaceAll("%P%", messages.getString("prefix")).replaceAll("%reason%", msg.toString()));

        if (p.getUniqueId() == null)
            banManager.kick(target.getUniqueId(), p.getName(), msg.toString());
        else
            banManager.kick(target.getUniqueId(), p.getUniqueId(), msg.toString());
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all != p) {
                for (String message : messages.getStringList("Kick.reason.notify")) {
                    all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%player%", target.getName()).replaceAll("%sender%", p.getName())
                            .replaceAll("%reason%", msg.toString()).replaceAll("&", "§"));
                }
            }
        }
    }

    @Override
    public void execute(User p, String[] args) {
        /**
         * TODO: Check if the user is online
         */
        if (p.hasPermission("bansys.kick")) {
            if (args.length >= 1) {
                if(BanSystem.getInstance().getUser(args[0]).getUniqueId() != null) {
                    User target = BanSystem.getInstance().getUser(args[0]);
                    if (target != p) {
                        if (args.length == 1) {
                            if (!target.hasPermission("bansys.kick")) {
                                try {
                                    noReasonKick(p, target);
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }

                            } else {
                                if (p.hasPermission("bansys.kick.admin")) {
                                    try {
                                        noReasonKick(p, target);
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                } else {
                                    p.sendMessage(messages.getString("Kick.cannotkickteammembers")
                                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                                }
                            }
                        } else {
                            if (!target.hasPermission("bansys.kick")) {
                                try {
                                    reasonKick(p, target, args);
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                            } else {
                                if (p.hasPermission("bansys.kick.admin")) {
                                    try {
                                        reasonKick(p, target, args);
                                    } catch (SQLException throwables) {
                                        throwables.printStackTrace();
                                    }
                                } else {
                                    p.sendMessage(messages.getString("Kick.cannotkickteammembers")
                                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                                }
                            }
                        }
                    } else {
                        p.sendMessage(messages.getString("Kick.cannotkickyouselfe")
                                .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                    }
                } else {
                    p.sendMessage(messages.getString("PlayerNotFound")
                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                }
            } else {
                p.sendMessage(messages.getString("Kick.usage").replaceAll("%P%", messages.getString("prefix"))
                        .replaceAll("&", "§"));
            }
        } else {
            p.sendMessage(messages.getString("NoPermissionMessage").replaceAll("%P%", messages.getString("prefix")));
        }
    }
}
