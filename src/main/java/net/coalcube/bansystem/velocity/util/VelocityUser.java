package net.coalcube.bansystem.velocity.util;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.coalcube.bansystem.core.util.User;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.TextComponent;

import java.net.InetAddress;
import java.util.UUID;

public class VelocityUser implements User {

    private final CommandSource sender;

    public VelocityUser(CommandSource sender) {
        this.sender = sender;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(String msg) {
        Component component = Component.text(msg);
        sender.sendMessage(component);
    }

    @Override
    public void sendMessage(TextComponent msg) {
        Component component = Component.text(msg.getText());
        sender.sendMessage(component);
    }

    @Override
    public boolean hasPermission(String perm) {
        return sender.hasPermission(perm);
    }

    @Override
    public String getName() {
        return sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
    }

    @Override
    public Object getRawUser() {
        return sender;
    }

    @Override
    public UUID getUniqueId() {
        return sender instanceof Player ? ((Player) sender).getUniqueId() : null;
    }

    @Deprecated
    @Override
    public InetAddress getAddress() {
        return ((Player) sender).getRemoteAddress().getAddress();
    }

    @Override
    public String getDisplayName() {
        return sender instanceof Player ? ((Player) sender).getUsername() : "CONSOLE";
    }

    @Override
    public boolean equals(Object o) {
        return sender.equals(o);
    }

}