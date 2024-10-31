package me.f64.playtime.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.f64.playtime.PlayTime;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Chat {
    private final DataStorage dataStorage;
    static PlayTime plugin;

    public Chat(PlayTime instance, DataStorage dataStorage) {
        this.dataStorage = dataStorage;
        plugin = instance;
    }

    @Contract("_ -> new")
    public static @NotNull String format(String commandLabel) {
        return ChatColor.translateAlternateColorCodes('&', commandLabel);
    }

    public static void message(@NotNull CommandSender sender, Player player, String commandLabel) {
        sender.sendMessage(PlaceholderAPI.setPlaceholders(player, format(commandLabel)));
    }

    public static void console(String commandLabel) {
        Bukkit.getConsoleSender().sendMessage(format(commandLabel));
    }

    public int ticksPlayed(@NotNull Player player) {
        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());

        String playerUUID = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        long sessionStart = plugin.Sessions.getOrDefault(playerUUID, currentTime);
        int sessionOnTime = (int) ((currentTime - sessionStart) / 1000);

        if (playerData != null) {
            return playerData.playTime() + sessionOnTime;
        } else {
            return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20;
        }
    }

    public int sessionsPlayed(@NotNull Player player) {
        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());

        if (playerData != null) {
            return playerData.joins();
        } else {
            return player.getStatistic(Statistic.LEAVE_GAME) + 1;
        }
    }
}
