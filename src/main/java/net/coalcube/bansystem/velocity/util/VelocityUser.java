package net.coalcube.bansystem.velocity.util;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.coalcube.bansystem.core.util.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
        if(getUniqueId() == null) {
            LegacyComponentSerializer lcs = LegacyComponentSerializer.legacySection();
            sender.sendMessage(lcs.deserialize(msg));
        } else {
            Component component = Component.text(msg);
            sender.sendMessage(component);
        }
    }

    @Override
    public void sendMessage(TextComponent msg) {
        if(getUniqueId() == null) {
            LegacyComponentSerializer lcs = LegacyComponentSerializer.legacySection();
            sender.sendMessage(lcs.deserialize(msg.getText()));
        } else {
            Component component = Component.text(msg.getText());
            sender.sendMessage(component);
        }

    }

    @Override
    public void disconnect(String message) {
        if(sender instanceof Player) {
            LegacyComponentSerializer lcs = LegacyComponentSerializer.legacySection();
            ((Player) sender).disconnect(lcs.deserialize(message));
        }
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
        if(sender == null)
            return null;
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