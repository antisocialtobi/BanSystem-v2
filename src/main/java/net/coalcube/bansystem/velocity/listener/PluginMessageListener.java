package net.coalcube.bansystem.velocity.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PluginMessageListener implements Listener {

    @EventHandler
    public void onChat(PluginMessageEvent event){
        if(event.getSender() instanceof ProxiedPlayer && event.getTag().equalsIgnoreCase("bansys:chatsign"))
            event.setCancelled(true);

    }
}
