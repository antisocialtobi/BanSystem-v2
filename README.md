## Beschreibung
Umfangreiches Bestrafungssystem für Minecraft Server. Es umfasst viele Funktionen und ist einfach zu bedienen.

## Anforderungen
- mind. Java 9
- Bungeecord/Spigot/Velocity Server
- MySQL Datenbank (Nicht notwendig)

## Funktionen
- Ban+Mute+Kick System
- Chatfilter
- Chatdelay
- Fast Alles einstellbar
- Fast Alle Nachrichten anpassbar
- IP Autoban
- ID System
- History System
- MySQL/SQLite
- VPN-Check
- Geyser Support

## Installation
1. Lade dir das Plugin runter
2. Ziehe das Plugin in den Plugins Ordner (Bei Verwendung eines Proxys nur in den jeweiligen Plugins Ordner ziehen **!nicht in die Unterserver!**)
3. Server starten

### !Ab der 1.19.1!
Wegen der Chat Signatur ab der Version müssen zusätzliche Schritte gemacht werden.

**Bungeecord**
1. die Option `signdChatBypass` in der config.yml auf true `stellen`
2. die beigelegte Datei "`BanSystem-SpigotChatAdapter-X.X-SNAPSHOT.jar`" auf den Unterservern installieren

**Velocity**
1. SigndVelocity für Velocity und den entsprechenden Backend Server Herunterladen (https://modrinth.com/plugin/signedvelocity)
2. SigndVelocity installieren

## Support
Discord Server: https://discord.gg/PfQTqhfjgA

# IP Handling
Wenn ein Spieler gebannt wird, dann wird die IP in der Datenbank gespeichert und falls ein Spieler mit derselben IP den Server betritt werden alle Teammitglieder, 
die die Benötigten rechte dafür haben, benachrichtigt dass ein Spieler die selbe IP hat wie ein gebannter Spieler. 
Ob der Spieler gebannt werden soll oder nicht kann man dann selbst entscheiden. Man kann allerdings auch einstellen, dass die diejenigen automatisch gebannt werden.​

## ID System
Das ID System ist komplett variabel, D.h. man kann IDs abändern, entfernen und hinzufügen. Es lassen sich alle IDs mithilfle von /ban auflisten. Die Zeit ist in Sekunden gerechnet, z.B. ein Tag sind 86400 Sekunden oder für ein Jahr 31536000 Sekunden. 
Für jede ID gibt es auch Level, D.h. wenn ein Spieler zwei mal für den gleichen Grund gebannt wird, wird dieser länger gebannt insofern ein 2. Level bei der ID angegeben wurde. Falls ein Spieler schon mal für alle Level gebannt wurde, wird dieser für das letzte Level nochmal gebannt.

## VPN-Check Apikey
Bei dem Apikey musst du nicht zwingend etwas eintragen, jedoch kannst du dir bei https://vpnapi.io/ einen Account erstellen und erhältst somit einen Key. Diesen benötigst du, wenn deinem Server mehr als 100 Leute am Tag beitreten.

## Befehle und Rechte

- /bansystem - `bansys.bansys`
- /bansystem reload - `bansys.reload`
- /bansystem ids create <ID> <Type> <OnlyAdmins> <duration> <reason> - `bansys.ids.create`
- /bansystem ids delete <ID> - `bansys.ids.delete`
- /bansystem ids edit <ID> add lvl <Duration> <Type> - `bansys.ids.addlvl`
- /bansystem ids edit <ID> remove lvl <lvl> - bansys.ids.removelvl
- /bansystem ids edit <ID> set lvlduration <lvl> <Duration> - `bansys.ids.setduration`
- /bansystem ids edit <ID> set lvltype <lvl> <Type> - `bansys.ids.settype`
- /bansystem ids edit <ID> set onlyadmins <True/False> - `bansys.ids.setonlyadmins`
- /bansystem ids edit <ID> set reason <reason> - `bansys.ids.setreason`
- /bansystem ids show <ID> - `bansys.ids.show`
- /bansys logs show [site] - `bansys.logs.show`
- /bansys logs clear - `bansys.logs.clear`
- /ban <Spieler> <ID> - `bansys.ban(.<ID>/.all/.admin)`
- /unban <Spieler> - `bansys.unban`
- /unmute <Spieler> - `bansys.unmute`
- /check <Spieler> - `bansys.check`
- /deletehistory <Spieler> - `bansys.history.delete`
- /history <Spieler> - `bansys.history.show`
- /kick <Spieler> [Grund] - `bansys.kick(.admin)`
- Team Benachrichtigungen - `bansys.notify`
- Kick bypass - `bansys.kick.bypass`
- Ban bypass - `bansys.ban.bypass`
- Chatfilter bypass - `bansys.bypasschatfilter`
- Chatdelay bypass - `bansys.bypasschatdelay`
