package net.coalcube.bansystem.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.listener.ChatListener;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.util.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class VelocityChatEvent {

    private final BanSystem banSystem;
    private final ChatListener chatListener;

    public VelocityChatEvent(BanSystem banSystem,
                             ProxyServer server,
                             BanManager banManager,
                             YamlDocument config,
                             Database sql,
                             BlacklistUtil blacklistUtil,
                             ConfigurationUtil configurationUtil,
                             IDManager idManager) {
        this.banSystem = banSystem;

        this.chatListener = new ChatListener(banSystem, banManager, configurationUtil, sql, blacklistUtil, idManager);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerChat(com.velocitypowered.api.event.player.PlayerChatEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        User user = banSystem.getUser(e.getPlayer().getUniqueId());
        String msg = e.getMessage();

        Event event = chatListener.onChat(user, msg);

        if(event.isCancelled())
            e.setResult(com.velocitypowered.api.event.player.PlayerChatEvent.ChatResult.denied());
    }
}
