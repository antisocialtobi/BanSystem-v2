package net.coalcube.bansystem.core.util;

import net.md_5.bungee.api.chat.TextComponent;

import java.net.InetAddress;
import java.util.UUID;

public interface User {

    void sendMessage(String msg);

    void sendMessage(TextComponent msg);

    void disconnect(String message);

    boolean hasPermission(String perm);

    String getName();

    Object getRawUser();

    UUID getUniqueId();

    InetAddress getAddress();

    String getDisplayName();
}