package net.coalcube.bansystem.spigot.listener;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.BanManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class SpigotPlayerKickListener implements Listener {

    BanSystem banSystem;
    BanManager banManager;

    public SpigotPlayerKickListener(BanSystem banSystem, BanManager banManager) {
        this.banSystem = banSystem;
        this.banManager = banManager;
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String reason = event.getReason();

        event.setReason(reason.replaceAll("\\\\n", "\n"));
    }
}
