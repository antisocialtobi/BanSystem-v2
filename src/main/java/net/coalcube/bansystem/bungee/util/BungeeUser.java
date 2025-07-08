package net.coalcube.bansystem.bungee.util;

import net.coalcube.bansystem.core.util.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.util.UUID;

public class BungeeUser implements User {

    private final CommandSender sender;

    public BungeeUser(CommandSender sender) {
        this.sender = sender;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(String msg) {
        sender.sendMessage(msg);
    }

    @Override
    public void sendMessage(TextComponent msg) {
        sender.sendMessage(msg);
    }

    @Override
    public void disconnect(String message) {
        if(sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).disconnect(message);
    }

    @Override
    public boolean hasPermission(String perm) {
        return sender.hasPermission(perm);
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    public Object getRawUser() {
        return sender;
    }

    @Override
    public UUID getUniqueId() {
        return sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getUniqueId() : null;
    }

    @Deprecated
    @Override
    public InetAddress getAddress() {
        return ((ProxiedPlayer) sender).getAddress().getAddress();
    }

    @Override
    public String getDisplayName() {
        return ((ProxiedPlayer) sender).getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        return sender.equals(o);
    }

}