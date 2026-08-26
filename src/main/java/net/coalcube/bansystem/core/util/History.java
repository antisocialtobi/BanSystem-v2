package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;

import java.net.InetAddress;
import java.util.Date;
import java.util.UUID;

public class History {

    private final HistoryType historyType;
    private final UUID player;
    private final String creator;
    private final String reason;
    private final Date createDate;
    private final Long duration;
    private final Type type;
    private final InetAddress ip;
    private final String id;

    public History(HistoryType historyType, UUID player, String creator, String reason, Long createDate, Long duration, Type type, InetAddress ip, String id) {
        this.historyType = historyType;
        this.player = player;
        this.creator = creator;
        this.reason = reason;
        this.createDate = new Date(createDate);
        this.duration = duration;
        this.type = type;
        this.ip = ip;
        this.id = id;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public UUID getPlayer() {
        return player;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Long getDuration() {
        return duration;
    }

    public InetAddress getIp() {
        return ip;
    }

    public String getCreator() {
        String creatorTmp;
        try {
            creatorTmp = UUIDFetcher.getName(UUID.fromString(creator));
        } catch (IllegalArgumentException e) {
            creatorTmp = creator;
        }
        return creatorTmp;
    }

    public String getReason() {
        return reason;
    }

    public Type getType() {
        return type;
    }

    public Date getEndDate() {
        if (getDuration() == -1) {
            return null;
        }
        return new Date(getCreateDate().getTime() + getDuration());
    }

    public String getId() {
        if (id == null) {
            return "Not available";
        }
        return id;
    }
}
