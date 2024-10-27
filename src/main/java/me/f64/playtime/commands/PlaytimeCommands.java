package me.f64.playtime.commands;

import me.f64.playtime.PlayTime;
import me.f64.playtime.utils.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PlaytimeCommands implements TabExecutor {
    private static PlayTime plugin;
    private static DataStorage dataStorage;
    public static ConfigWrapper config;

    public PlaytimeCommands(PlayTime instance, DataStorage dataStorage) {
        plugin = instance;
        PlaytimeCommands.config = new ConfigWrapper(instance, null, "config.yml");
        PlaytimeCommands.dataStorage = dataStorage;
        PlaytimeCommands.config.createFile(null,
                """
                        # PlaytimeCommands By F64_Rx - Need Help? PM me on Spigot or post in the discussion.\r
                        \r
                        #     =================\r
                        #     | CONFIGURATION |\r
                        #     =================\r
                        \r
                        #    available placeholders\r
                        #    %playtime_player% - returns the player name\r
                        #    %offlineplayer% - returns the offline player name\r
                        #    %offlinetime% - shows offline time of a player\r
                        #    %offlinetimesjoined% - shows the amount of joins a player has had\r
                        #    %playtime_time% - shows time played\r
                        #    %playtime_timesjoined% - shows the amount of times the player has joined the server\r
                        #    %playtime_serveruptime% - shows the uptime of the server\r
                        #    %playtime_position% - shows the players current position\r
                        #    %playtime_top_#_name% - shows the name of the top 10\r
                        #    %playtime_top_#_time% - shows the time of the top 10\r
                        #    You can also use any other placeholder that PlaceholderAPI supports :) \r
                        """);
        FileConfiguration fileConfig = PlaytimeCommands.config.getConfig();
        fileConfig.addDefault("time.second.enabled", true);
        fileConfig.addDefault("time.second.prefix", "s");
        fileConfig.addDefault("time.minute.enabled", true);
        fileConfig.addDefault("time.minute.prefix", "m");
        fileConfig.addDefault("time.hour.enabled", true);
        fileConfig.addDefault("time.hour.prefix", "h");
        fileConfig.addDefault("time.day.enabled", true);
        fileConfig.addDefault("time.day.prefix", "d");
        fileConfig.addDefault("time.week.enabled", true);
        fileConfig.addDefault("time.week.prefix", "w");
        fileConfig.addDefault("messages.no_permission", List.of("&8[&bPlayTime&8] &cYou don't have permission."));
        fileConfig.addDefault("messages.doesnt_exist", List.of("&8[&bPlayTime&8] &cPlayer %offlineplayer% has not joined before!"));
        fileConfig.addDefault("messages.player", Arrays.asList("&b%playtime_player%'s Stats are:", "&bPlayTime: &7%playtime_time%", "&bTimes Joined: &7%playtime_timesjoined%"));
        fileConfig.addDefault("messages.offline_players", Arrays.asList("&b%offlineplayer%'s Stats are:", "&bPlayTime: &7%offlinetime%", "&bTimes Joined: &7%offlinetimesjoined%"));
        fileConfig.addDefault("messages.other_players", Arrays.asList("&b%playtime_player%'s Stats are:", "&bPlayTime: &7%playtime_time%", "&bTimes Joined: &7%playtime_timesjoined%"));
        fileConfig.addDefault("messages.playtimetop.header", Arrays.asList("&bTop &e10 &bplayers playtime:", ""));
        fileConfig.addDefault("messages.playtimetop.message", List.of("&a%position%. &b%player%: &e%playtime%"));
        fileConfig.addDefault("messages.playtimetop.footer", List.of(""));
        fileConfig.addDefault("messages.server_uptime", List.of("&8[&bPlayTime&8] &bServer's total uptime is %playtime_serveruptime%"));
        fileConfig.addDefault("messages.reload_config", List.of("&8[&bPlayTime&8] &bYou have successfully reloaded the config."));
        fileConfig.addDefault("placeholder.top.name", "none");
        fileConfig.addDefault("placeholder.top.time", "-");
        fileConfig.options().copyDefaults(true);
        PlaytimeCommands.config.saveConfig();
    }

    public String getPlayerTime(String name) {
        Player player = plugin.getServer().getPlayer(name);
        if (player != null) {
            UUID playerUUID = player.getUniqueId();
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.playTime()) : null;
        }

        try {
            UUID playerUUID = UUID.fromString(name);
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.playTime()) : null;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID format for player " + name);
        }
        return null;
    }

    public String getPlayerJoins(String name) {
        Player player = plugin.getServer().getPlayer(name);
        if (player != null) {
            UUID playerUUID = player.getUniqueId();
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.joins()) : null;
        }

        try {
            UUID playerUUID = UUID.fromString(name);
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.joins()) : null;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID format for player " + name);
        }
        return null;
    }

    public static TopPlayers @NotNull [] getTopTen() {
        List<DataStorage.PlayerData> allPlayers = dataStorage.loadAllPlayerData();
        List<TopPlayers> topPlayersList = new ArrayList<>();

        for (DataStorage.PlayerData data : allPlayers) {
            topPlayersList.add(new TopPlayers(data.playerName(), data.uuid().toString(), data.playTime()));
        }

        topPlayersList.sort(Comparator.comparingInt(e -> e.time));
        Collections.reverse(topPlayersList);

        return topPlayersList.stream()
                .limit(10)
                .toArray(TopPlayers[]::new);
    }


    public static void checkOnlinePlayers(TopPlayers[] top10) {
        Chat chat = new Chat(plugin, dataStorage);
        for (Player player : plugin.getServer().getOnlinePlayers())
            if (chat.ticksPlayed(player) > (top10.length == 0 ? 0 : top10[top10.length - 1].time)) {
                TopPlayers top = new TopPlayers(player.getName(), player.getUniqueId().toString(),
                        chat.ticksPlayed(player));
                for (int i = 0; i < top10.length; ++i)
                    if (top10[i].time <= top.time)
                        if (top10[i].uuid.equals(top.uuid)) {
                            top10[i] = top;
                            break;
                        } else {
                            TopPlayers temp = top10[i];
                            top10[i] = (top = temp);
                        }
            }
    }

    private void sendChatMessages(CommandSender sender, @NotNull List<String> messages, Map<String, String> replacements) {
        for (String message : messages) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace(entry.getKey(), entry.getValue());
            }
            Chat.message(sender, sender instanceof Player ? (Player) sender : null, message);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String commandLabel, String[] args) {
        if (commandSender instanceof Player player) {
            FileConfiguration config = PlaytimeCommands.config.getConfig();

            if (command.getName().equalsIgnoreCase("playtime")) {
                if (!player.hasPermission("playtime.check")) {
                    sendNoPermissionMessage(commandSender, config);
                    return true;
                }

                if (args.length == 0) {
                    displayPlayerStats(commandSender, config);
                    return true;
                }

                return switch (args[0].toLowerCase()) {
                    case "reload" -> handleReloadCommand(player, config);
                    case "uptime" -> handleUptimeCommand(player, config);
                    case "top" -> handleTopCommand(player, config);
                    default -> handleOtherPlayerStats(commandSender, player, args[0], config);
                };
            }
        }
        return false;
    }

    private void sendNoPermissionMessage(CommandSender commandSender, FileConfiguration config) {
        if (commandSender instanceof Player player) {
            for (String noPermission : config.getStringList("messages.no_permission")) {
                Chat.message(commandSender, player, noPermission);
            }
        }
    }

    private void displayPlayerStats(CommandSender commandSender, FileConfiguration config) {
        if (commandSender instanceof Player player) {
            for (String thisPlayer : config.getStringList("messages.player")) {
                Chat.message(commandSender, player, thisPlayer);
            }
        } else {
            plugin.getLogger().warning("You cannot execute this command without specifying a player from the console.");
        }
    }

    private boolean handleReloadCommand(@NotNull Player player, FileConfiguration config) {
        if (!player.hasPermission("playtime.reload")) {
            sendNoPermissionMessage(player, config);
            return false;
        }
        for (String reloadConfig : config.getStringList("messages.reload_config")) {
            Chat.message(player, player, reloadConfig);
        }
        PlaytimeCommands.config.reloadConfig();
        return true;
    }

    private boolean handleUptimeCommand(@NotNull Player player, FileConfiguration config) {
        if (!player.hasPermission("playtime.uptime")) {
            sendNoPermissionMessage(player, config);
            return false;
        }
        for (String serverUptime : config.getStringList("messages.server_uptime")) {
            Chat.message(player, player, serverUptime);
        }
        return true;
    }

    private boolean handleTopCommand(@NotNull Player player, FileConfiguration config) {
        if (!player.hasPermission("playtime.checktop")) {
            sendNoPermissionMessage(player, config);
            return false;
        }

        TopPlayers[] topTen = getTopTen();
        checkOnlinePlayers(topTen);

        for (String header : config.getStringList("messages.playtimetop.header")) {
            Chat.message(player, player, header);
        }

        for (int i = 0; i < topTen.length; i++) {
            if (topTen[i].time == 0) {
                break;
            }
            for (String message : config.getStringList("messages.playtimetop.message")) {
                Chat.message(player, player, message
                        .replace("%position%", Integer.toString(i + 1))
                        .replace("%player%", topTen[i].name)
                        .replace("%playtime%", TimeFormat.getTime(Duration.of(topTen[i].time, ChronoUnit.SECONDS))));
            }
        }

        for (String footer : config.getStringList("messages.playtimetop.footer")) {
            Chat.message(player, player, footer);
        }
        return true;
    }

    private boolean handleOtherPlayerStats(CommandSender commandSender, @NotNull Player player, String targetName, FileConfiguration config) {
        if (!player.hasPermission("playtime.checkothers")) {
            sendNoPermissionMessage(player, config);
            return false;
        }

        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        if (targetPlayer == null) {
            String storedTime = getPlayerTime(targetName);
            String storedJoins = getPlayerJoins(targetName);
            if (storedTime == null || storedJoins == null) {
                sendChatMessages(commandSender, config.getStringList("messages.doesnt_exist"),
                        Map.of("%offlineplayer%", targetName));
            } else {
                sendChatMessages(commandSender, config.getStringList("messages.offline_players"),
                        Map.of(
                                "%offlineplayer%", targetName,
                                "%offlinetime%", TimeFormat.getTime(Duration.of(Integer.parseInt(storedTime), ChronoUnit.SECONDS)),
                                "%offlinetimesjoined%", storedJoins
                        ));
            }
        } else {
            sendChatMessages(player, config.getStringList("messages.other_players"),
                    Map.of("%playtime_player%", targetPlayer.getName()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> tabComplete = new ArrayList<>();

        if (commandSender.hasPermission("playtime.reload")) {
            tabComplete.add("reload");
        }
        if (commandSender.hasPermission("playtime.uptime")) {
            tabComplete.add("uptime");
        }
        if (commandSender.hasPermission("playtime.checktop")) {
            tabComplete.add("top");
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            tabComplete.add(player.getName());
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], tabComplete, new ArrayList<>());
        }

        return null;
    }
}