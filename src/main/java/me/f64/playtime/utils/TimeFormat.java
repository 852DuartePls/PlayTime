package me.f64.playtime.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TimeFormat {
    private final FileConfiguration config;

    public TimeFormat(@NotNull FileConfiguration config) {
        this.config = config;
    }

    public @NotNull String getTime(@NotNull Duration duration) {
        final StringBuilder builder = new StringBuilder();

        long seconds = duration.getSeconds();
        long minutes = seconds / 60, hours = minutes / 60, days = hours / 24, weeks = days / 7;
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 7;

        if (weeks > 0) appendUnit(builder, "time.week", weeks);
        if (days > 0) appendUnit(builder, "time.day", days);
        if (hours > 0) appendUnit(builder, "time.hour", hours);
        if (minutes > 0) appendUnit(builder, "time.minute", minutes);
        if (config.getBoolean("time.second.enabled") && seconds > 0) {
            appendUnit(builder, "time.second", seconds);
        }

        return builder.toString();
    }

    private void appendUnit(StringBuilder builder, String path, long value) {
        if (config.getBoolean(path + ".enabled")) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(value).append(Chat.format(config.getString(path + ".prefix")));
        }
    }

    public @NotNull String Uptime() {
        long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        return getTime(Duration.ofSeconds(uptimeSeconds));
    }
}
