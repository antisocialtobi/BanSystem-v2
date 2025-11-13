package net.coalcube.bansystem.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Formats a remaining ban duration (given in milliseconds) into a human-readable string using
 * messages supplied by {@link ConfigurationUtil}.
 *
 * <p>The output follows the pattern:
 *
 * <pre>
 * 1 day, 2 hours and 3 minutes
 * 5 seconds
 * never
 * </pre>
 *
 * <p>The class is immutable and thread-safe.
 */
public final class TimeFormatUtil {

    private static final long MILLIS_PER_SECOND = 1_000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;

    private final ConfigurationUtil config;

    public TimeFormatUtil(ConfigurationUtil config) {
        this.config = Objects.requireNonNull(config, "ConfigurationUtil must not be null");
    }

    /**
     * Formats the remaining time.
     *
     * @param remainingMillis the remaining time in milliseconds. Use {@code -1L} for "never" and
     *                        {@code 0L} (or negative) for "now".
     * @return a localised, human-readable representation of the duration
     */
    public String formatRemainingTime(long remainingMillis) {
        if (remainingMillis == -1L) {
            return this.config.getMessage("TimeFormat.never");
        }

        if (remainingMillis <= 0L) {
            return this.formatUnit("TimeFormat.seconds", "%sec%", 0);
        }

        long seconds = remainingMillis / MILLIS_PER_SECOND;
        long minutes = seconds / SECONDS_PER_MINUTE;
        long hours = minutes / MINUTES_PER_HOUR;
        long days = hours / HOURS_PER_DAY;

        seconds %= SECONDS_PER_MINUTE;
        minutes %= MINUTES_PER_HOUR;
        hours %= HOURS_PER_DAY;

        List<String> parts = new ArrayList<>(4);
        if (days > 0) {
            parts.add(this.formatUnit("TimeFormat.days", "%day%", days));
        }
        if (hours > 0) {
            parts.add(this.formatUnit("TimeFormat.hours", "%hour%", hours));
        }
        if (minutes > 0) {
            parts.add(this.formatUnit("TimeFormat.minutes", "%min%", minutes));
        }
        if (seconds > 0) {
            parts.add(this.formatUnit("TimeFormat.seconds", "%sec%", seconds));
        }

        if (parts.isEmpty()) {
            return this.formatUnit("TimeFormat.seconds", "%sec%", 0);
        }
        return this.joinTimeParts(parts);
    }

    private String formatUnit(String messageKey, String placeholder, long value) {
        return this.config.getMessage(messageKey).replace(placeholder, String.valueOf(value));
    }

    /**
     * Joins time parts in descending order using commas and "and".
     *
     * <p>Examples:
     * <ul>
     *   <li>["1 day"] → "1 day"
     *   <li>["1 day", "2 hours"] → "1 day and 2 hours"
     *   <li>["1 day", "2 hours", "3 minutes"] → "1 day, 2 hours and 3 minutes"
     * </ul>
     */
    private String joinTimeParts(List<String> parts) {
        int size = parts.size();
        if (size == 1) {
            return parts.get(0);
        }

        String and = " " + this.config.getMessage("and") + " ";
        if (size == 2) {
            return parts.get(0) + and + parts.get(1);
        }

        List<String> firstParts = parts.subList(0, size - 1);
        String lastPart = parts.get(size - 1);
        return String.join(", ", firstParts) + and + lastPart;
    }
}
