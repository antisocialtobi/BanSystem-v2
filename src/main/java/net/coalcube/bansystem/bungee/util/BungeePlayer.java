package net.coalcube.bansystem.bungee.util;

import net.coalcube.bansystem.core.util.Player;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetAddress;
import java.util.UUID;

public class BungeePlayer implements Player {

    private ProxiedPlayer p;

    public BungeePlayer(ProxiedPlayer p) {
        this.p = p;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(String msg) {
        p.sendMessage(msg);
    }

    @Deprecated
    @Override
    public void disconnect(String msg) {
        p.disconnect(msg);
    }

    @Override
    public boolean hasPermission(String perm) {
        return p.hasPermission(perm);
    }

    @Override
    public boolean isOnline() {
        return (p != null && p.isConnected()) ? true : false;
    }

    @Override
    public String getName() {
        return p.getName();
    }

    @Override
    public Object getRawUser() {
        return p;
    }

    @Override
    public UUID getUniqueId() {
        return p.getUniqueId();
    }

    @Deprecated
    @Override
    public InetAddress getAddress() {
        return p.getAddress().getAddress();
    }

    @Override
    public String getDisplayName() {
        return p.getDisplayName();
    }

    @Override
    public boolean equals(Object o) {
        return p.equals(o);
    }
}
