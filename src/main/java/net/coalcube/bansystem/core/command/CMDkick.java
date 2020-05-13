package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.Config;
import net.coalcube.bansystem.core.util.MySQL;
import net.coalcube.bansystem.core.util.User;

public class CMDkick implements Command {

    private Config messages;
    private MySQL mysql;

    public CMDkick(Config messages, MySQL mysql) {
        this.messages = messages;
        this.mysql = mysql;
    }

    public void noReasonKick(User p, User target) {
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.noreason.screen").replaceAll("&", "§"));
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
                .replaceAll("%player%", target.getName()).replaceAll("&", "§"));

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

    public void reasonKick(User p, User target, String[] args) {
        p.sendMessage(messages.getString("Kick.success").replaceAll("%P%", messages.getString("prefix"))
                .replaceAll("%player%", target.getName()).replaceAll("&", "§"));
        String msg = "";
        for (int i = 1; i < args.length; i++) {
            msg = msg + args[i] + " ";
        }
        BanSystem.getInstance().disconnect(target, messages.getString("Kick.reason.screen")
                .replaceAll("%P%", messages.getString("prefix")).replaceAll("%reason%", msg));
        for (User all : BanSystem.getInstance().getAllPlayers()) {
            if (all.hasPermission("bansys.notify") && all != p) {
                for (String message : messages.getStringList("Kick.reason.notify")) {
                    all.sendMessage(message.replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("%player%", target.getName()).replaceAll("%sender%", p.getName())
                            .replaceAll("%reason%", msg).replaceAll("&", "§"));
                }
            }
        }
    }

    @Override
    public void execute(User p, String[] args) {
        if (p.hasPermission("bansys.kick")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    User target = BanSystem.getInstance().getUser(args[0]);
                    if (target != null) {
                        if (target != p) {
                            if (!target.hasPermission("bansys.kick")) {
                                noReasonKick(p, target);

                            } else {
                                if (p.hasPermission("bansys.kick.admin")) {

                                    noReasonKick(p, target);

                                } else {
                                    p.sendMessage(messages.getString("Kick.cannotkickteammembers")
                                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
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
                } else if (args.length > 1) {
                    User target = BanSystem.getInstance().getUser(args[0]);
                    if (target != null) {
                        if (target != p) {
                            if (!target.hasPermission("bansys.kick")) {
                                reasonKick(p, target, args);
                            } else {
                                if (p.hasPermission("bansys.kick.admin")) {

                                    reasonKick(p, target, args);

                                } else {
                                    p.sendMessage(messages.getString("Kick.cannotkickteammembers")
                                            .replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
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
                p.sendMessage(messages.getString("NoDBConnection"));
            }
        } else {
            p.sendMessage(messages.getString("NoPermission"));
        }
    }
}
