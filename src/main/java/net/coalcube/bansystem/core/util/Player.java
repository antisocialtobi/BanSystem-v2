package net.coalcube.bansystem.core.util;

import java.net.InetAddress;
import java.util.UUID;

public interface Player {

    void sendMessage(String msg);

    void disconnect(String msg);

    boolean hasPermission(String perm);

    boolean isOnline();

    String getName();

    Object getRawUser();

    UUID getUniqueId();

    InetAddress getAddress();

    String getDisplayName();

}
