package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.listener.LoginListener;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponentmd5;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SpigotPlayerConnectionListener implements Listener {

    private final BanSystem banSystem;
    private final BanManager banManager;
    private final YamlDocument config;
    private final String banScreenRow;
    private final Plugin instance;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;
    private static Map<String, Boolean> vpnIpCache;
    private final LoginListener loginListener;

    public SpigotPlayerConnectionListener(BanSystem banSystem, BanManager banManager, YamlDocument config, String banScreen, Plugin instance, URLUtil urlUtil, ConfigurationUtil configurationUtil, Database sql, IDManager idManager) {
        this.banSystem = banSystem;
        this.banManager = banManager;
        this.config = config;
        this.banScreenRow = banScreen;
        this.instance = instance;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;

        net.coalcube.bansystem.core.textcomponent.TextComponent textComponent = new TextComponentmd5(configurationUtil);
        this.loginListener = new LoginListener(banSystem, banManager, configurationUtil, sql, idManager, urlUtil, textComponent);
        vpnIpCache = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent e) {
        UUID uuid = e.getUniqueId();
        InetAddress ip = e.getAddress();
        Database sql = BanSystem.getInstance().getSQL();
        if(!sql.isConnected()) return;

        Event event = loginListener.onJoin(uuid, e.getName(), ip);

        if(event.isCancelled()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, event.getCancelReason());
        }

    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        try {
            Ban ban = banManager.getBan(p.getUniqueId(), Type.NETWORK);
            if (ban != null) {
                e.setQuitMessage(null);
            }
        } catch (SQLException | ExecutionException | InterruptedException throwables) {
            throwables.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        User user = new SpigotUser(p);
        InetAddress ip = p.getAddress().getAddress();

        try {
            Event event = loginListener.onPostJoin(user, ip);
            if(event.isCancelled()) {
                e.setJoinMessage("");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.kickPlayer(event.getCancelReason());
                    }
                }.runTaskLater(BanSystemSpigot.getPlugin(), 40L);
            }
        } catch (SQLException | ExecutionException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}