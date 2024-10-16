package net.coalcube.bansystem.core.ban;

import java.util.Date;
import java.util.UUID;

public class Ban {

    private Type type;
    private String reason, creator, ip;
    private UUID player;
    private Date creationdate;
    private long duration;
    private String id;

    public Ban(String id, UUID player, Type type, String reason, String creator, String ip, Date creationdate, long duration) {
        this.id = id;
        this.player = player;
        this.reason = reason;
        this.creationdate = creationdate;
        this.creator = creator;
        this.ip = ip;
        this.type = type;
        this.duration = duration;
    }

    public Ban(String id, UUID player, Type type, String reason, UUID creator, String ip, Date creationdate, long duration) {
        this.id = id;
        this.player = player;
        this.reason = reason;
        this.creationdate = creationdate;
        this.creator = creator.toString();
        this.ip = ip;
        this.type = type;
        this.duration = duration;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public Date getCreationdate() {
        return creationdate;
    }

    public void setCreationdate(Date creationdate) {
        this.creationdate = creationdate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getEnd() {
        return (duration == -1) ? duration : creationdate.getTime() + duration;
    }

    public long getRemainingTime() {
        return (getEnd() == -1) ? -1 : getEnd() - System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }
}
