package net.coalcube.bansystem.bungee.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.bungee.BanSystemBungee;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.listener.LoginListener;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponentmd5;
import net.coalcube.bansystem.core.util.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class BungeeLoginListener implements Listener {

    private final BanSystem banSystem;
    private final LoginListener loginListener;

    public BungeeLoginListener(BanSystem banSystem, BanManager banManager, YamlDocument config, Database sql, URLUtil urlUtil, ConfigurationUtil configurationUtil, IDManager idManager) {
        this.banSystem = banSystem;

        net.coalcube.bansystem.core.textcomponent.TextComponent textComponent = new TextComponentmd5(configurationUtil);
        this.loginListener = new LoginListener(banSystem, banManager, configurationUtil, sql, idManager, urlUtil, textComponent);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        PendingConnection con = event.getConnection();
        UUID uuid = con.getUniqueId();
        String username = con.getName();
        InetAddress ip = event.getConnection().getAddress().getAddress();

        event.registerIntent(BanSystemBungee.getInstance());
        new Thread(() -> {
            Event loginEvent = loginListener.onJoin(uuid, username, ip);
            if(loginEvent.isCancelled()) {
                event.setCancelled(loginEvent.isCancelled());
                event.setReason(new TextComponent(loginEvent.getCancelReason()));
            }
            event.completeIntent(BanSystemBungee.getInstance());
        }).start();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPostLogin(PostLoginEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        User user = banSystem.getUser(uuid);

        try {
            Event postEvent = loginListener.onPostJoin(user, user.getAddress());
            if(postEvent.isCancelled()) {
                user.disconnect(postEvent.getCancelReason());
            }
        } catch (SQLException | ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }


    }
}
