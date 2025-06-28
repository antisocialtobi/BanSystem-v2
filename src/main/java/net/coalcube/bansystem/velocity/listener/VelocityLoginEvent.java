package net.coalcube.bansystem.velocity.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.listener.LoginListener;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponent;
import net.coalcube.bansystem.core.textcomponent.TextComponentKyori;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.IDManager;
import net.coalcube.bansystem.core.util.URLUtil;
import net.coalcube.bansystem.core.util.User;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class VelocityLoginEvent {

    private final BanSystem banSystem;
    private final LoginListener loginListener;

    public VelocityLoginEvent(BanSystem banSystem,
                              BanManager banManager,
                              YamlDocument config,
                              Database sql,
                              URLUtil urlUtil,
                              ConfigurationUtil configurationUtil,
                              IDManager idManager) {
        this.banSystem = banSystem;
        TextComponent textComponent = new TextComponentKyori(configurationUtil);

        this.loginListener = new LoginListener(banSystem, banManager, configurationUtil, sql, idManager, urlUtil, textComponent);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerLogin(com.velocitypowered.api.event.connection.LoginEvent e) throws SQLException, IOException, ExecutionException, InterruptedException {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        User user = banSystem.getUser(uuid);
        InetAddress ip = player.getRemoteAddress().getAddress();

        Event loginEvent = loginListener.onJoin(uuid, player.getUsername(), ip);
        if(loginEvent.isCancelled()) {
            Component component = Component.text(loginEvent.getCancelReason());

            e.setResult(ResultedEvent.ComponentResult.denied(component));
        }
    }
    @Subscribe(order = PostOrder.EARLY)
    public void onPostPlayerLogin(com.velocitypowered.api.event.connection.PostLoginEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        User user = banSystem.getUser(uuid);
        InetAddress ip = player.getRemoteAddress().getAddress();

        Event event = null;
        try {
            event = loginListener.onPostJoin(user, ip);
        } catch (SQLException | ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if(event.isCancelled()) {
            user.disconnect(event.getCancelReason());
        }
    }
}