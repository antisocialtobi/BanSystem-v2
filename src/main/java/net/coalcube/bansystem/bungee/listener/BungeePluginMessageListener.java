package net.coalcube.bansystem.bungee.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeePluginMessageListener implements Listener {

    @EventHandler
    public void onChat(PluginMessageEvent event) {
        if (event.getSender() instanceof ProxiedPlayer && event.getTag().equalsIgnoreCase("bansys:chatsign"))
            event.setCancelled(true);

    }
}
