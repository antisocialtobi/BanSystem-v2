package net.coalcube.bansystem.bungee.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.listener.ChatListener;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;
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
    private final BanManager banManager;
    private final YamlDocument config;
    private final BlacklistUtil blacklistUtil;
    private final Database sql;
    private final ConfigurationUtil configurationUtil;
    private final ChatListener chatListener;
    private final boolean signdChatBypass;

    public BungeeChatListener(BanSystem banSystem, BanManager banManager, YamlDocument config, Database sql, BlacklistUtil blacklistUtil, ConfigurationUtil configurationUtil, IDManager idManager) {
        this.banSystem = banSystem;
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.blacklistUtil = blacklistUtil;
        this.configurationUtil = configurationUtil;
        this.chatListener = new ChatListener(banSystem, banManager, configurationUtil, sql, blacklistUtil, idManager);

        signdChatBypass = config.getBoolean("signdChatBypass.enable");
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        ProxiedPlayer p = (ProxiedPlayer) e.getSender();
        User user = banSystem.getUser(p.getUniqueId());
        String msg = e.getMessage();

        Event event = chatListener.onChat(user, msg);

        e.setCancelled(event.isCancelled());

        // send message to Spigot server.

        if(!signdChatBypass) return;
        if(e.isCommand() || e.isCancelled() || e.isProxyCommand()) return;

        if(p.getPendingConnection().getVersion() < 767)
            return;

        e.setCancelled(true);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(b)) {
            out.writeUTF(e.getMessage());
            p.getServer().sendData("bansys:chatsign", b.toByteArray());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
