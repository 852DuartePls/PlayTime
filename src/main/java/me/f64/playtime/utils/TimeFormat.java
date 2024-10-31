package me.f64.playtime.utils;

import me.f64.playtime.commands.PlaytimeCommands;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TimeFormat {
    public static @NotNull String getTime(@NotNull Duration duration) {
        FileConfiguration config = PlaytimeCommands.config.getConfig();
        final StringBuilder builder = new StringBuilder();

        long seconds = duration.getSeconds();
        long minutes = seconds / 60, hours = minutes / 60, days = hours / 24, weeks = days / 7;
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        days %= 7;

        if (weeks > 0) appendUnit(builder, config, "time.week", weeks);
        if (days > 0) appendUnit(builder, config, "time.day", days);
        if (hours > 0) appendUnit(builder, config, "time.hour", hours);
        if (minutes > 0) appendUnit(builder, config, "time.minute", minutes);
        if (config.getBoolean("time.second.enabled") && seconds > 0) {
            appendUnit(builder, config, "time.second", seconds);
        }

        return builder.toString();
    }

    private static void appendUnit(StringBuilder builder, @NotNull FileConfiguration config, String path, long value) {
        if (config.getBoolean(path + ".enabled")) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(value).append(Chat.format(config.getString(path + ".prefix")));
        }
    }

    public static @NotNull String Uptime() {
        long uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(ManagementFactory.getRuntimeMXBean().getUptime());
        return getTime(Duration.ofSeconds(uptimeSeconds));
    }
}
