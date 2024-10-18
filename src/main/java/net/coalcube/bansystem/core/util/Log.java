package net.coalcube.bansystem.core.util;

import java.util.Date;

public class Log {

    Integer id;
    String target, creator, action, note;
    Date creationDate;

    public Log(Integer id, String target, String creator, String action, String note, Date creationDate) {
        this.id = id;
        this.target = target;
        this.creator = creator;
        this.action = action;
        this.note = note;
        this.creationDate = creationDate;
    }

    public String getTarget() {
        return target;
    }

    public String getCreator() {
        return creator;
    }

    public String getAction() {
        return action;
    }

    public String getNote() {
        return note;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Integer getId() {
        return id;
    }
}
