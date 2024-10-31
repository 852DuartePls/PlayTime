package me.f64.playtime.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.f64.playtime.PlayTime;
import me.f64.playtime.utils.Chat;
import me.f64.playtime.utils.DataStorage;
import me.f64.playtime.utils.TimeFormat;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expansion extends PlaceholderExpansion {
    private static PlayTime plugin;
    private final DataStorage dataStorage;
    static Pattern topPlaceholder = Pattern.compile("top_([0-9]+)_(name|time)");

    public Expansion(PlayTime instance, DataStorage dataStorage) {
        plugin = instance;
        this.dataStorage = dataStorage;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playtime";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String commandLabel) {
        Chat chat = new Chat(plugin, dataStorage);
        long ticksPlayed = chat.ticksPlayed(player);
        UUID playerUUID = player.getUniqueId();

        return switch (commandLabel) {
            case "serveruptime" -> TimeFormat.Uptime();
            case "position" -> getPlayerPosition(playerUUID);
            case "player" -> player.getName();
            case "time" -> formatDuration(ticksPlayed);
            case "time_seconds" -> String.valueOf(convertTicksToSeconds(ticksPlayed));
            case "time_minutes" -> String.valueOf(convertTicksToMinutes(ticksPlayed));
            case "time_hours" -> String.valueOf(convertTicksToHours(ticksPlayed));
            case "time_days" -> String.valueOf(convertTicksToDays(ticksPlayed));
            case "time_weeks" -> String.valueOf(convertTicksToWeeks(ticksPlayed));
            case "session" -> {
                long sessionTime = Expansion.plugin.getPlayerSession(player);
                yield formatDuration(sessionTime);
            }
            case "timesjoined" -> String.valueOf(chat.sessionsPlayed(player));
            default -> {
                if (commandLabel.startsWith("top_")) {
                    yield handleTopPlaceholder(commandLabel);
                }
                yield "";
            }
        };
    }

    private @NotNull String getPlayerPosition(UUID playerUUID) {
        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
        List<DataStorage.PlayerData> allPlayers = dataStorage.loadAllPlayerData();

        if (playerData != null) {
            allPlayers.sort(Comparator.comparingInt(DataStorage.PlayerData::playTime).reversed());
            int position = allPlayers.indexOf(playerData);
            return String.valueOf(position + 1);
        }
        return "Player not found";
    }

    private String handleTopPlaceholder(String commandLabel) {
        Matcher matcher = topPlaceholder.matcher(commandLabel);
        if (matcher.find()) {
            try {
                int pos = Integer.parseInt(matcher.group(1));
                String type = matcher.group(2);
                return get(String.valueOf(pos), type).toString();
            } catch (NumberFormatException e) {
                return "Invalid number";
            }
        }
        return "";
    }

    private @NotNull String formatDuration(long ticks) {
        return TimeFormat.getTime(Duration.of(ticks, ChronoUnit.SECONDS));
    }

    private long convertTicksToSeconds(long ticks) {
        return Duration.of(ticks, ChronoUnit.SECONDS).getSeconds();
    }

    private long convertTicksToMinutes(long ticks) {
        return Duration.of(ticks, ChronoUnit.SECONDS).toMinutes();
    }

    private long convertTicksToHours(long ticks) {
        return Duration.of(ticks, ChronoUnit.SECONDS).toHours();
    }

    private long convertTicksToDays(long ticks) {
        return Duration.of(ticks, ChronoUnit.SECONDS).toDays();
    }

    private long convertTicksToWeeks(long ticks) {
        return Duration.of(ticks, ChronoUnit.SECONDS).toDays() / 7;
    }
}
