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
        return plugin.getDescription().getAuthors().toString();
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
    public String onPlaceholderRequest(Player player, String commandLabel) {
        Chat chat = new Chat(plugin, dataStorage);

        long ticksPlayed = chat.ticksPlayed(player);

        if (commandLabel.equals("serveruptime")) {
            return TimeFormat.Uptime();
        }

        UUID playerUUID = player.getUniqueId();


        if (commandLabel.equals("position")) {
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            List<DataStorage.PlayerData> allPlayers = dataStorage.loadAllPlayerData();

            if (playerData != null) {
                allPlayers.sort(Comparator.comparingInt(DataStorage.PlayerData::playTime).reversed());
                int position = allPlayers.indexOf(playerData);
                return String.valueOf(position + 1);
            }
            return "Player not found";
        }
        if (commandLabel.startsWith("top_")) {
            Matcher matcher = topPlaceholder.matcher(commandLabel);
            if (matcher.find()) {
                int pos = Integer.parseInt(matcher.group(1));
                String type = matcher.group(2);
                return get(String.valueOf(pos), type).toString();
            }
        }

        switch (commandLabel) {
            case "player" -> {
                return player.getName();
            }
            case "time" -> {
                return TimeFormat.getTime(Duration.of(ticksPlayed, ChronoUnit.SECONDS));
            }
            case "time_seconds" -> {
                return String.valueOf(Duration.of(ticksPlayed, ChronoUnit.SECONDS).getSeconds());
            }
            case "time_minutes" -> {
                long minutes = Duration.of(ticksPlayed, ChronoUnit.SECONDS).toMinutes();
                return String.valueOf(minutes);
            }
            case "time_hours" -> {
                long hours = Duration.of(ticksPlayed, ChronoUnit.SECONDS).toHours();
                return String.valueOf(hours);
            }
            case "time_days" -> {
                long days = Duration.of(ticksPlayed, ChronoUnit.SECONDS).toDays();
                return String.valueOf(days);
            }
            case "time_weeks" -> {
                long weeks = Duration.of(ticksPlayed, ChronoUnit.SECONDS).toDays() / 7;
                return String.valueOf(weeks);
            }
            case "session" -> {
                long sessionTime = Expansion.plugin.getPlayerSession(player);
                return TimeFormat.getTime(Duration.of(sessionTime, ChronoUnit.SECONDS));
            }
            case "timesjoined" -> {
                return String.valueOf(chat.sessionsPlayed(player));
            }
            default -> {
                return "";
            }
        }
    }
}
