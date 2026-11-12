package net.coalcube.bansystem.spigot.util;

import net.coalcube.bansystem.core.util.User;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.UUID;

public class SpigotUser implements User {

    private final CommandSender sender;

    public SpigotUser(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(String msg) {
        sender.sendMessage(msg);
    }

    @Override
    public void sendMessage(TextComponent msg) {
        if (sender instanceof Player)
            ((Player) sender).spigot().sendMessage(msg);
        else
            sender.sendMessage(msg.getText());
    }

    @Override
    public void disconnect(String message) {
        if (sender instanceof Player)
            ((Player) sender).kickPlayer(message);
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
        return sender instanceof Player ? ((Player) sender).getUniqueId() : null;
    }

    @Override
    public InetAddress getAddress() {
        return sender instanceof Player ? ((Player) sender).getAddress().getAddress() : null;
    }

    @Override
    public String getDisplayName() {
        return sender instanceof Player ? ((Player) sender).getDisplayName() : null;
    }

    @Override
    public boolean equals(Object o) {
        return sender.equals(o);
    }

}