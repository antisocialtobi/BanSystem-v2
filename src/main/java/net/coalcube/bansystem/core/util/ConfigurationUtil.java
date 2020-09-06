package net.coalcube.bansystem.core.util;

import java.util.Arrays;

public class ConfigurationUtil {

    public static void initConfig(Config config) {
        config.set("mysql.enable", true);
        config.set("mysql.user", "root");
        config.set("mysql.password", "sicherespasswort");
        config.set("mysql.database", "Bansystem");
        config.set("mysql.host", "localhost");
        config.set("mysql.port", 3306);

        config.set("IPautoban.enable", false);
        config.set("IPautoban.banid", 9);
        config.set("IPautoban.onlyNetworkBans", false);

        config.set("VPN.enable", true);
        config.set("VPN.autoban.enable", true);
        config.set("VPN.autoban.ID", 11);
        config.set("VPN.serverIP", "00.00.00.00");

        config.set("mute.blockedCommands", Arrays.asList("/msg", "/tell", "/me", "/whisper", "/say"));

        config.set("needReason.Unban", false);
        config.set("needReason.Unmute", false);

        config.set("blacklist.words.enable", true);
        config.set("blacklist.words.autoban.enable", true);
        config.set("blacklist.words.autoban.id", 6);

        config.set("blacklist.ads.enable", true);
        config.set("blacklist.ads.autoban.enable", true);
        config.set("blacklist.ads.autoban.id", 7);

        config.set("IDs.1.reason", "Unerlaubte Clientmodification/Hackclient");
        config.set("IDs.1.onlyAdmins", false);
        config.set("IDs.1.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.1.lvl.1.duration", 86400 * 30);
        config.set("IDs.1.lvl.2.type", Type.NETWORK.toString());
        config.set("IDs.1.lvl.2.duration", -1);

        config.set("IDs.2.reason", "Reportmissbrauch");
        config.set("IDs.2.onlyAdmins", false);
        config.set("IDs.2.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.2.lvl.1.duration", 86400 * 10);
        config.set("IDs.2.lvl.2.type", Type.NETWORK.toString());
        config.set("IDs.2.lvl.2.duration", -1);

        config.set("IDs.3.reason", "Unangebrachter Skin/Name");
        config.set("IDs.3.onlyAdmins", false);
        config.set("IDs.3.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.3.lvl.1.duration", 86400 * 30);
        config.set("IDs.3.lvl.2.type", Type.NETWORK.toString());
        config.set("IDs.3.lvl.2.duration", -1);

        config.set("IDs.4.reason", "Crossteaming");
        config.set("IDs.4.onlyAdmins", false);
        config.set("IDs.4.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.4.lvl.1.duration", 86400 * 10);
        config.set("IDs.4.lvl.2.type", Type.NETWORK.toString());
        config.set("IDs.4.lvl.2.duration", -1);

        config.set("IDs.5.reason", "Bugusing");
        config.set("IDs.5.onlyAdmins", false);
        config.set("IDs.5.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.5.lvl.1.duration", 86400 * 10);
        config.set("IDs.5.lvl.2.type", Type.NETWORK.toString());
        config.set("IDs.5.lvl.2.duration", -1);

        config.set("IDs.6.reason", "Chat Verhalten");
        config.set("IDs.6.onlyAdmins", false);
        config.set("IDs.6.lvl.1.type", Type.CHAT.toString());
        config.set("IDs.6.lvl.1.duration", 86400);
        config.set("IDs.6.lvl.2.type", Type.CHAT.toString());
        config.set("IDs.6.lvl.2.duration", 86400 * 7);
        config.set("IDs.6.lvl.3.type", Type.CHAT.toString());
        config.set("IDs.6.lvl.3.duration", 86400 * 14);
        config.set("IDs.6.lvl.4.type", Type.CHAT.toString());
        config.set("IDs.6.lvl.4.duration", 86400 * 30);
        config.set("IDs.6.lvl.5.type", Type.CHAT.toString());
        config.set("IDs.6.lvl.5.duration", -1);

        config.set("IDs.7.reason", "Werbung");
        config.set("IDs.7.onlyAdmins", false);
        config.set("IDs.7.lvl.1.type", Type.CHAT.toString());
        config.set("IDs.7.lvl.1.duration", 86400 * 7);
        config.set("IDs.7.lvl.2.type", Type.CHAT.toString());
        config.set("IDs.7.lvl.2.duration", 86400 * 14);
        config.set("IDs.7.lvl.3.type", Type.CHAT.toString());
        config.set("IDs.7.lvl.3.duration", 86400 * 30);
        config.set("IDs.7.lvl.4.type", Type.CHAT.toString());
        config.set("IDs.7.lvl.4.duration", -1);

        config.set("IDs.8.reason", "Alt-Account");
        config.set("IDs.8.onlyAdmins", false);
        config.set("IDs.8.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.8.lvl.1.duration", -1);

        config.set("IDs.9.reason", "Bannumgehung");
        config.set("IDs.9.onlyAdmins", false);
        config.set("IDs.9.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.9.lvl.1.duration", -1);

        config.set("IDs.10.reason", "Sicherheitsbann");
        config.set("IDs.10.onlyAdmins", false);
        config.set("IDs.10.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.10.lvl.1.duration", -1);

        config.set("IDs.11.reason", "VPN");
        config.set("IDs.11.onlyAdmins", false);
        config.set("IDs.11.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.11.lvl.1.duration", -1);

        config.set("IDs.99.reason", "§4EXTREM");
        config.set("IDs.99.onlyAdmins", true);
        config.set("IDs.99.lvl.1.type", Type.NETWORK.toString());
        config.set("IDs.99.lvl.1.duration", -1);
    }

    public static void initMessages(Config messages) {

        messages.set("prefix", "§8§l┃ §cBanSystem §8» §7");
        messages.set("NoPermissionMessage", "%P%§cDafür hast du keine Rechte!");
        messages.set("NoPlayerMessage", "%P%§cDu bist kein Spieler!");
        messages.set("Playerdoesnotexist", "%P%§cDieser Spieler existiert nicht!");
        messages.set("PlayerNotFound", "%P%§cDer Spieler wurde nicht gefunden.");
        messages.set("NoDBConnection",
                "%P%§cDie Datenbankverbindung besteht nicht. Wende dich bitte an einen Administrator.");
        messages.set("Playernotbanned", "%P%§cDieser Spieler ist nicht gebannt/gemuted!");
        messages.set("DateTimePattern", "dd.MM.yyyy HH:mm:ss");

        messages.set("Ban.Network.Screen",
                Arrays.asList(
                        "§8§m----------------------§r",
                        " ",
                        "§4Du wurdest §lGebannt!",
                        "§7Grund §8» §c%Reason%",
                        "§7Verbleibende Zeit §8» §3%reamingtime%",
                        " ",
                        "§8§m----------------------"));
        messages.set("Ban.Network.autounban", "%P%§e%player% §7wurde §eautomatisch §7entbannt.");
        messages.set("Ban.Chat.Screen",
                Arrays.asList("§8§m----------------------------",
                        "§4Du bist aus dem chat gebannt!",
                        "§7Grund §8» §c%reason%",
                        "§7Verbleibende Zeit §8» §c%reamingtime%",
                        "§8§m----------------------------"));
        messages.set("Ban.Chat.autounmute.success", "%P%§e%player% §7wurde §eautomatisch §7entmuted.");
        messages.set("Ban.Chat.autounmute.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("Ban.success", "%P%§7Du hast §e%Player% §7erfolgreich §cgebannt/gemuted.");
        messages.set("Ban.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("Ban.bypass", "%P%§cDu kannst Spieler, die eine Bypass Permission haben, nicht bannen.");
        messages.set("Ban.notify",
                Arrays.asList("%P%§8§m-------------------",
                        "%P%§e%player% §7wurde Gebannt.",
                        "%P%Grund §8» §c%reason%",
                        "%P%Verbleibende Zeit §8» §c%reamingTime%",
                        "%P%Von §8» §c%banner%",
                        "%P%Type §8» §c%type%",
                        "%P%§8§m-------------------"));
        messages.set("Ban.cannotban.teammembers", "%P%§cDu kannst keine Teammitglieder bannen.");
        messages.set("Ban.cannotban.bypassedplayers", "%P%§cDu kannst keine Spieler Bannen die eine Bypass Permission haben.");
        messages.set("Ban.cannotban.yourself", "%P%§cDu kannst dich nicht selbst bestrafen.");
        messages.set("Ban.onlyadmins", "%P%§cDas dürfen nur Admins und Owner!");
        messages.set("Ban.invalidinput", "%P%§cUngültige Eingabe!");
        messages.set("Ban.alreadybanned", "%P%§cDieser Spieler ist berreits gebannt/gemuted.");
        messages.set("Ban.usage", "%P%§cBenutze §8» §e/ban §8<§7Spieler§8> §8<§7ID§8>");
        messages.set("Ban.ID.Listlayout.heading", "§8§m------------§8» §4Bann IDs §8«§m------------");
        messages.set("Ban.ID.Listlayout.IDs.general", " §e%ID% §8» §c%reason%");
        messages.set("Ban.ID.Listlayout.IDs.onlyadmins",
                " §e%ID% §8» §c%reason% §8» §4nur für Admins und Owner");
        messages.set("Ban.ID.NoPermission", "%P%§cFür diese ID hast du keine Berechtigung!");

        messages.set("Check.usage", "%P%§cBenutze §8» §e/check §8<§7Spieler§8>");
        messages.set("Check.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("Check.chat", Arrays.asList("%P%§8§m------§8» §e%player% §8«§m------",
                "%P%§7Von §8» §c%banner%",
                "%P%§7Grund §8» §c%reason%",
                "%P%§7Verbleibende Zeit §8» §c%reamingtime%",
                "%P%§7Type §8» §cChat",
                "%P%§7Level §8» §c%level%",
                "%P%§8§m-----------------"));
        messages.set("Check.network", Arrays.asList("%P%§8§m------§8» §e%player% §8«§m------",
                "%P%§7Von §8» §c%banner%",
                "%P%§7Grund §8» §c%reason%",
                "%P%§7Verbleibende Zeit §8» §c%reamingtime%",
                "%P%§7Type §8» §cNetzwerk",
                "%P%§7Level §8» §c%level%",
                "%P%§8§m-----------------"));
        messages.set("Check.networkandchat", Arrays.asList("%P%§8§m------§8» §e%player% §8«§m------",
                "%P%§7Von §8» §c%bannernetwork%",
                "%P%§7Grund §8» §c%reasonnetwork%",
                "%P%§7Verbleibende Zeit §8» §c%reamingtimenetwork%",
                "%P%§7Type §8» §cNetzwerk",
                "%P%§7Level §8» §c%levelnetwork%",
                "%P%",
                "%P%§7Von §8» §c%bannerchat%",
                "%P%§7Grund §8» §c%reasonchat%",
                "%P%§7Verbleibende Zeit §8» §c%reamingtimechat%",
                "%P%§7Type §8» §cChat",
                "%P%§7Level §8» §c%levelchat%",
                "%P%§8§m-----------------"));

        messages.set("Deletehistory.notify",
                "%P%Die History von §e%player% §7wurde von §e%sender% §7gelöscht.");
        messages.set("Deletehistory.success", "%P%Die History von §e%player% §7wurde gelöscht.");
        messages.set("Deletehistory.usage", "%P%§cBenutze §8» §e/deletehistory §8<§7Spieler§8>");
        messages.set("Deletehistroy.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");

        messages.set("History.historynotfound", "%P%§cDieser Spieler hat keine History");
        messages.set("History.usage", "%P%§cBenutze §8» §e/history §8<§7Spieler§8>");
        messages.set("History.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("History.header", "%P%§8§m-------§8» §e%player% §8«§m-------");
        messages.set("History.body", Arrays.asList(
                "%P%§7Grund §8» §c%reason%",
                "%P%§7Erstelldatum §8» §c%creationdate%",
                "%P%§7Enddatum §8» §c%enddate%",
                "%P%§7Ersteller §8» §c%creator%",
                "%P%§7IP §8» §c%ip%",
                "%P%§7Type §8» §c%type%",
                "%P%§7ID §8» §c%id%",
                "%P%"));
        messages.set("History.footer", "%P%§8§m------------------------");

        messages.set("Kick.usage", "%P%§cBenutze §8» §e/kick §8<§7Spieler§8> §8[§7Grund§8]");
        messages.set("Kick.cannotkickyouselfe", "%P%§cDu kannst dich nicht selbst Kicken!");
        messages.set("Kick.cannotkickteammembers", "%P%§cDu kannst keine Teammitglieder Kicken.");
        messages.set("Kick.bypass", "%P%§cDu kannst Spieler, die eine Bypass Permission haben, nicht kicken.");
        messages.set("Kick.noreason.screen", "\n §cDu wurdest vom Netzwerk §4§lgekickt§c! \n \n");
        messages.set("Kick.noreason.notify",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§7Der Spieler §e%player%", "%P%§7wurde von §e%sender% §cgekickt.",
                        "%P%§8§m------------------------------"));
        messages.set("Kick.reason.screen",
                "\n §cDu wurdest vom Netzwerk §4§lgekickt§c!\n \n§7Grund §8» §c%reason%\n\n");
        messages.set("Kick.reason.notify",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§7Der Spieler §e%player%", "%P%§7wurde von §e%sender% §cgekickt.",
                        "%P%§7Grund §8» §e%reason%", "%P%§8§m------------------------------"));
        messages.set("Kick.success", "%P%§7Der Spieler §e%player% §7wurde gekickt.");

        messages.set("Unban.success", "%P%§e%player% §7wurde §2erfolgeich §7entbannt.");
        messages.set("Unban.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("Unban.notify", "%P%§e%player% §7wurde von §e%sender% §7entbannt.");
        messages.set("Unban.notbanned", "%P%§e%player% §cist nicht gebannt.");
        messages.set("Unban.usage", "%P%§cBenutze §8» §e/unban §8<§7Spieler§8>");

        messages.set("Unban.needreason.usage", "%P%§cBenutze §8» §e/unban §8<§7Spieler§8> §8<§7Grund§8>");
        messages.set("Unban.needreason.success", "%P%§e%player% §7wurde §2erfolgeich §7entbannt.");
        messages.set("Unban.needreason.notify",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e%player% §7wurde von §e%sender% §7entbannt.", "%P%§7Grund §8» §e%reason%",
                        "%P%§8§m------------------------------"));

        messages.set("Unmute.usage", "%P%§cBenutze §8» §e/unmute §8<§7Spieler§8>");
        messages.set("Unmute.success", "%P%§7Die Schweigepflicht von §e%player% §7wurde §2aufgehoben!");
        messages.set("Unmute.faild", "%P%§cEs ist ein Fehler aufgetreten. In der Konsole findest du mehr Informationen.");
        messages.set("Unmute.notmuted", "%P%§e%player% §cist nicht gemuted.");
        messages.set("Unmute.notify", "%P%§e%player% §7wurde von §e%sender% §7entmuted.");

        messages.set("Unmute.needreason.usage", "%P%§cBenutze §8» §e/unmute §8<§7Spieler§8> §8<§7Grund§8>");
        messages.set("Unmute.needreason.success",
                "%P%§7Die Schweigepflicht von §e%player% §7wurde §2aufgehoben!");
        messages.set("unmute.needreason.notify",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e%player% §7wurde von §e%sender% §7entmuted.", "%P%§7Grund §8» §e%reason%",
                        "%P%§8§m------------------------------"));

        messages.set("VPN.warning", "%P%§e%player% §chat sich mit einer VPN verbunden!");
        messages.set("bansystem.usage", "%P%§7Benutze §e/bansystem help");
        messages.set("bansystem.help",
                Arrays.asList("§8§m--------§8[ §cBanSystem §8]§m--------",
                        "§e/bansystem help §8» §7Zeigt dir alle Befehle des BanSystems",
                        "§e/bansystem reload §8» §7Lädt das Plugin neu",
                        "§e/bansystem version §8» §7Zeigt dir die Version des Plugins",
                        "§e/bansystem syncids §8» §7Synchronisiere die BanIDs",
                        "§e/ban §8<§7Spieler§8> §8<§7ID§8> §8» §7Bannt/Muted Spieler",
                        "§e/kick §8<§7Spieler§8> §8[§7Grund§8] §8» §7Kickt einen Spieler",
                        "§e/unban §8<§7Spieler§8> §8» §7Entbannt einen Spieler",
                        "§e/unmute §8<§7Spieler§8> §8» §7Entmuted einen Spieler",
                        "§e/check §8<§7Spieler§8> §8» §7Prüft ob ein Spieler bestraft ist",
                        "§e/history §8<§7Spieler§8> §8» §7Zeigt die History von einem Spieler",
                        "§e/deletehistory §8<§7Spieler§8> §8» §7Löscht die History von einem Spieler",
                        "§8§m-----------------------------"));
        messages.set("bansystem.reload.process", "%P%§7Plugin wird §eneu geladen§7.");
        messages.set("bansystem.reload.finished", "%P%§7Plugin §eneu geladen§7.");
        messages.set("bansystem.version", "%P%§7Version §8» §e%ver%");
        messages.set("bansystem.ids.sync.MySQLdisabled", "%P%§cDu kannst diese Funktion nicht verwenden wenn du keine MySQL Datenbank verwendest.");
        messages.set("bansystem.ids.sync.faild", "%P%§cDie synchronisation ist fehlgeschlagen. Um mehr Informationen zu bekommen schau in die Konsole.");
        messages.set("bansystem.ids.sync.success", "%P%§7Die BanIDs wurden §2synchronisiert§7.");

        messages.set("blacklist.notify.words.autoban",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e%player% wurde §7für sein Chatverhalten",
                        "%P%§7automatisch §egebannt/gemuted§7.",
                        "%P%§7Grund §8» §e%reason%", "%P%§7Nachricht §8» §e%message%",
                        "%P%§7Verbleibende Zeit §8» §e%reamingTime%",
                        "%P%§8§m------------------------------"));
        messages.set("blacklist.notify.words.warning",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e§l§nWARNUNG",
                        "%P%§e%player% hat eine eventuell unangemessene Nachricht in den Chat geschickt.",
                        "%P%§7Nachricht §8» §e%message%",
                        "%P%§8§m------------------------------"));
        messages.set("blacklist.notify.ads.autoban",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e%player% wurde §7für sein Chatverhalten",
                        "%P%§7automatisch §egebannt/gemuted§7.",
                        "%P%§7Grund §8» §e%reason%",
                        "%P%§7Nachricht §8» §e%message%",
                        "%P%§8§m------------------------------"));
        messages.set("blacklist.notify.ads.warning",
                Arrays.asList("%P%§8§m------------------------------",
                        "%P%§e§l§nWARNUNG",
                        "%P%§e%player% hat eine eventuell im Chat Werbung gemacht.",
                        "%P%§7Nachricht §8» §e%message%",
                        "%P%§8§m------------------------------"));
        messages.set("ip.autoban", "%P%§cDer 2. Account von §e%bannedaccount% §cwurde automatisch gebannt für §e%reason%§c.");
        messages.set("ip.warning", Arrays.asList("%P%§8§m------------------------------",
                "%P%§e§l§nWARNUNG",
                "%P%§e%player% §7ist womöglich ein 2. Account von §e%bannedaccount%.",
                "%P%§8§m------------------------------"));

    }

    public static void initBlacklist(Config blacklist) {
        /**
         * TODO add more blacklisted words
         */

        blacklist.set("Words", Arrays.asList("Arsch", "Nutte", "Hure", "Tits", "Pussy", "ez", "Fick",
                "Basdard", "Muschi", "beschissen"));

        /**
         * TODO add more blacklisted ads
         */

        blacklist.set("Ads",
                Arrays.asList(".de", ". de", ".  de", "PUNKT de", ".net", ". net", ".  net", "PUNKT net", ".at", ".com",
                        ".be", ".eu", ".shop", ".it", "www.", "de.", "shop.", ".dev", ".xyz", ".wtf"));
    }
}
