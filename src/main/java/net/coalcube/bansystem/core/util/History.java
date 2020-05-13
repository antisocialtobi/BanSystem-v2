package net.coalcube.bansystem.core.util;

import java.net.Inet4Address;
import java.util.Date;
import java.util.UUID;

public class History {

    private UUID player;
    private String creator, reason;
    private Date create_date, end_date;
    private Type type;
    private Inet4Address ip;

    public History(UUID player, String creator, String reason, Long create_date, Long end_date, Type type, Inet4Address ip) {
        this.player = player;
        this.creator = creator;
        this.reason = reason;
        this.create_date = new Date(create_date);
        this.end_date = new Date(end_date);
        this.type = type;
        this.ip = ip;
    }

    public UUID getPlayer() {
        return player;
    }

    public Date getCreate_date() {
        return create_date;
    }

    public Date getEnd_date() {
        return end_date;
    }

    public Inet4Address getIp() {
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
