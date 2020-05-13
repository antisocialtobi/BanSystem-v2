package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.util.UUID;

public class CMDcheck implements Command {

    private BanManager bm;
    private MySQL mysql;
    private Config messages;

    public CMDcheck(BanManager banmanager, MySQL mysql, Config messages) {
        this.bm = banmanager;
        this.mysql = mysql;
        this.messages = messages;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.check")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(
                                messages.getString("Playerdoesnotexist").replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }

                    if (bm.isBanned(uuid, Type.CHAT) && bm.isBanned(uuid, Type.NETWORK)) {

                        user.sendMessage(messages.getString("prefix") + "§8§m------§8» §e" + UUIDFetcher.getName(uuid) + " §8«§m------");
                        user.sendMessage(messages.getString("prefix") + "§7Von §8» §c" + bm.getBanner(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Grund §8» §c" + bm.getReason(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Verbleibende Zeit §8» §c" + bm.getRemainingTime(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Type §8» §c" + Type.CHAT);
                        user.sendMessage(messages.getString("prefix") + "§7Level §8» §c" + bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                        user.sendMessage(messages.getString("prefix"));
                        user.sendMessage(messages.getString("prefix") + "§7Von §8» §c" + bm.getBanner(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Grund §8» §c" + bm.getReason(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Verbleibende Zeit §8» §c" + bm.getRemainingTime(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Type §8» §c" + Type.NETWORK);
                        user.sendMessage(messages.getString("prefix") + "§7Level §8» §c" + bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));
                        user.sendMessage(messages.getString("prefix") + "§8§m-----------------");

                    } else if (bm.isBanned(uuid, Type.CHAT)) {

                        user.sendMessage(messages.getString("prefix") + "§8§m------§8» §e" + UUIDFetcher.getName(uuid) + " §8«§m------");
                        user.sendMessage(messages.getString("prefix") + "§7Von §8» §c" + bm.getBanner(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Grund §8» §c" + bm.getReason(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Verbleibende Zeit §8» §c" + bm.getRemainingTime(uuid, Type.CHAT));
                        user.sendMessage(messages.getString("prefix") + "§7Type §8» §c" + Type.CHAT);
                        user.sendMessage(messages.getString("prefix") + "§7Level §8» §c" + bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                        user.sendMessage(messages.getString("prefix") + "§8§m-----------------");

                    } else if (bm.isBanned(uuid, Type.NETWORK)) {
                        user.sendMessage(messages.getString("prefix") + "§8§m------§8» §e" + UUIDFetcher.getName(uuid) + " §8«§m------");
                        user.sendMessage(messages.getString("prefix") + "§7Von §8» §c" + bm.getBanner(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Grund §8» §c" + bm.getReason(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Verbleibende Zeit §8» §c" + bm.getRemainingTime(uuid, Type.NETWORK));
                        user.sendMessage(messages.getString("prefix") + "§7Type §8» §c" + Type.NETWORK);
                        user.sendMessage(messages.getString("prefix") + "§7Level §8» §c" + bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));
                        user.sendMessage(messages.getString("prefix") + "§8§m-----------------");
                    } else {
                        user.sendMessage(messages.getString("Playernotbanned")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                .replaceAll("&", "§"));
                    }
                } else {
                    user.sendMessage(messages.getString("Check.usage")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection"));
            }
        } else {
            user.sendMessage(messages.getString("NoPermission"));
        }
    }
}
