package net.coalcube.bansystem.core.listener;

public class Event {

    private boolean cancelled;
    private String cancelReason;

    public Event() {}

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelReason(String reason) {
        this.cancelReason = reason;
    }

    public String getCancelReason() {
        return cancelReason;
    }
}
