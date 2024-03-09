package net.coalcube.bansystem.core.util;

import java.net.InetAddress;
import java.util.UUID;

public interface User {

    void sendMessage(String msg);

    boolean hasPermission(String perm);

    String getName();

    Object getRawUser();

    UUID getUniqueId();

    InetAddress getAddress();

    String getDisplayName();

}