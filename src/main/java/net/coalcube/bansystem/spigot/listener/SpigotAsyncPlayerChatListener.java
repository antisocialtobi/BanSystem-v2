package net.coalcube.bansystem.spigot.listener;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class SpigotAsyncPlayerChatListener implements Listener {

    private final BanSystem banSystem;
    private final ChatListener chatListener;

    public SpigotAsyncPlayerChatListener(BanSystem banSystem,
                                         YamlDocument config,
                                         BanManager banManager,
                                         Database sql,
                                         BlacklistUtil blacklistUtil,
                                         ConfigurationUtil configurationUtil,
                                         IDManager idManager) {
        this.banSystem = banSystem;
        this.chatListener = new ChatListener(banSystem, banManager, configurationUtil, sql, blacklistUtil, idManager);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) throws IOException, SQLException, ExecutionException, InterruptedException {
        User user = banSystem.getUser(e.getPlayer().getUniqueId());
        String msg = e.getMessage();

        Event event = chatListener.onChat(user, msg);
        if (event.isCancelled()) {
            e.setCancelled(true);
        }
    }
}