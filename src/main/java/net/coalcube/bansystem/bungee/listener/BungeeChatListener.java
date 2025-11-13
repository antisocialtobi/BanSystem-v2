package net.coalcube.bansystem.bungee.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.listener.ChatListener;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.BlacklistUtil;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.IDManager;
import net.coalcube.bansystem.core.util.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class BungeeChatListener implements Listener {

    private final BanSystem banSystem;
    private final ChatListener chatListener;
    private final boolean signedChatBypass;

    public BungeeChatListener(BanSystem banSystem, BanManager banManager, YamlDocument config, Database sql, BlacklistUtil blacklistUtil, ConfigurationUtil configurationUtil, IDManager idManager) {
        this.banSystem = banSystem;
        this.chatListener = new ChatListener(banSystem, banManager, configurationUtil, sql, blacklistUtil, idManager);

        signedChatBypass = config.getBoolean("signdChatBypass.enable");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent event) throws SQLException, IOException, ExecutionException, InterruptedException {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        User user = banSystem.getUser(player.getUniqueId());
        String message = event.getMessage();

        Event chatEvent = chatListener.onChat(user, message);

        if (chatEvent.isCancelled())
            chatEvent.setCancelled(true);

        // send message to Spigot server to bypass the problem with chat signing.

        if (!signedChatBypass) return;
        if (event.isCommand() || event.isCancelled() || event.isProxyCommand()) return;
        // experimental checking client version
        // if(player.getPendingConnection().getVersion() < 767) return;

        event.setCancelled(true);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF(event.getMessage());
            player.getServer().sendData("bansys:chatsign", b.toByteArray());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
