package net.coalcube.bansystem.core.util;

import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;

public class History {

    private UUID player;
    private String creator, reason;
    private Date createDate, duration;
    private Type type;
    private InetAddress ip;

    public History(UUID player, String creator, String reason, Long createDate, Long duration, Type type, InetAddress ip) {
        this.player = player;
        this.creator = creator;
        this.reason = reason;
        this.createDate = new Date(createDate);
        this.duration = new Date(duration);
        this.type = type;
        this.ip = ip;
    }

    public UUID getPlayer() {
        return player;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Date getDuration() {
        return duration;
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getCreator() {
        return creator;
    }

    public String getReason() {
        return reason;
    }

    public Type getType() {
        return type;
    }
}
