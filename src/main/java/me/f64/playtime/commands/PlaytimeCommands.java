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
    private static ConfigHandler config;

    public PlaytimeCommands(PlayTime instance, DataStorage dataStorage) {
        plugin = instance;
        PlaytimeCommands.dataStorage = dataStorage;
        PlaytimeCommands.config = new ConfigHandler(instance);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String commandLabel, String @NotNull [] args) {
        FileConfiguration config = PlaytimeCommands.config.getConfig();

        TimeFormat timeFormat = new TimeFormat(config);

        if (sender instanceof Player player) {
            if (args.length == 0) {
                if (player.hasPermission("playtime.check")) {
                    displayPlayerStats(sender, config);
                } else {
                    sendPluginVersion(sender, config);
                }
                return true;
            }

            return switch (args[0].toLowerCase()) {
                case "reload" -> handleReloadCommand(sender, config);
                case "uptime" -> handleUptimeCommand(sender, config, timeFormat);
                case "top" -> handleTopCommand(sender, config, timeFormat);
                default -> handleOtherPlayerStats(sender, args[0], config, timeFormat);
            };
        } else {
            sendPluginVersion(sender, config);
        }
        return true;
    }

    private void sendPluginVersion(CommandSender sender, FileConfiguration config) {
        if (sender instanceof Player player) {
            for (String pluginVersionMessage : config.getStringList("messages.plugin_version")) {
                Chat.message(sender, player, pluginVersionMessage);
            }
        } else {
            for (String pluginVersionMessage : config.getStringList("messages.plugin_version")) {
                Chat.console(pluginVersionMessage);
            }
        }
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
            for (String thisPlayer : config.getStringList("messages.player")) {
                Chat.console(thisPlayer);
            }
        }
    }

    private boolean handleReloadCommand(CommandSender sender, FileConfiguration config) {
        if (sender instanceof Player) {
            if (hasPermission(sender, "playtime.reload", config)) return false;
        }

        sendChatMessages(sender, config.getStringList("messages.reload_config"), Map.of());
        PlaytimeCommands.config.reloadConfig();
        return true;
    }

    private boolean handleUptimeCommand(CommandSender sender, FileConfiguration config, TimeFormat timeFormat) {
        if (sender instanceof Player) {
            if (hasPermission(sender, "playtime.uptime", config)) return false;
        }

        sendChatMessages(sender, config.getStringList("messages.server_uptime"), Map.of(
                "uptime", timeFormat.Uptime()
        ));
        return true;
    }

    private boolean handleTopCommand(CommandSender sender, FileConfiguration config, TimeFormat timeFormat) {
        if (sender instanceof Player) {
            if (hasPermission(sender, "playtime.checktop", config)) return false;

            TopPlayers[] topTen = getTopTen();
            checkOnlinePlayers(topTen);

            List<String> headerMessages = config.getStringList("messages.playtimetop.header");
            sendChatMessages(sender, headerMessages, Map.of());

            for (int i = 0; i < topTen.length; i++) {
                if (topTen[i].time == 0) {
                    break;
                }
                String formattedMessage = config.getStringList("messages.playtimetop.message").getFirst()
                        .replace("%position%", Integer.toString(i + 1))
                        .replace("%player%", topTen[i].name)
                        .replace("%playtime%", timeFormat.getTime(Duration.of(topTen[i].time, ChronoUnit.SECONDS)));
                sendChatMessages(sender, List.of(formattedMessage), Map.of());
            }

            List<String> footerMessages = config.getStringList("messages.playtimetop.footer");
            sendChatMessages(sender, footerMessages, Map.of());
        } else {
            TopPlayers[] topTen = getTopTen();
            checkOnlinePlayers(topTen);

            List<String> headerMessages = config.getStringList("messages.playtimetop.header");
            for (String header : headerMessages) {
                Chat.console(header);
            }

            for (int i = 0; i < topTen.length; i++) {
                if (topTen[i].time == 0) {
                    break;
                }
                String formattedMessage = config.getStringList("messages.playtimetop.message").getFirst()
                        .replace("%position%", Integer.toString(i + 1))
                        .replace("%player%", topTen[i].name)
                        .replace("%playtime%", timeFormat.getTime(Duration.of(topTen[i].time, ChronoUnit.SECONDS)));
                Chat.console(formattedMessage);
            }

            List<String> footerMessages = config.getStringList("messages.playtimetop.footer");
            for (String footer : footerMessages) {
                Chat.console(footer);
            }
        }
        return true;
    }

    private boolean handleOtherPlayerStats(@NotNull CommandSender sender, String targetName, FileConfiguration config, TimeFormat timeFormat) {
        if (!sender.hasPermission("playtime.checkothers")) {
            sendNoPermissionMessage(sender, config);
            return false;
        }

        Player targetPlayer = plugin.getServer().getPlayer(targetName);
        if (targetPlayer == null) {
            String storedTime = getPlayerTime(targetName);
            String storedJoins = getPlayerJoins(targetName);
            if (storedTime == null || storedJoins == null) {
                sendChatMessages(sender, config.getStringList("messages.doesnt_exist"),
                        Map.of("%offlineplayer%", targetName));
            } else {
                sendChatMessages(sender, config.getStringList("messages.offline_players"),
                        Map.of(
                                "%offlineplayer%", targetName,
                                "%offlinetime%", timeFormat.getTime(Duration.of(Integer.parseInt(storedTime), ChronoUnit.SECONDS)),
                                "%offlinetimesjoined%", storedJoins
                        ));
            }
        } else {
            sendChatMessages(sender, config.getStringList("messages.other_players"),
                    Map.of("%playtime_player%", targetPlayer.getName()));
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission, FileConfiguration config) {
        if (sender instanceof Player player) {
            if (!player.hasPermission(permission)) {
                sendNoPermissionMessage(player, config);
                return true;
            }
        }
        return false;
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

    public String getPlayerTime(String name) {
        Player player = plugin.getServer().getPlayer(name);
        if (player != null) {
            UUID playerUUID = player.getUniqueId();
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.playTime()) : "0";
        }

        try {
            UUID playerUUID = UUID.fromString(name);
            DataStorage.PlayerData playerData = dataStorage.loadPlayerData(playerUUID);
            return playerData != null ? String.valueOf(playerData.playTime()) : "0";
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID format for player " + name);
        }
        return "0";
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

            if (sender instanceof Player) {
                Chat.message(sender, (Player) sender, message);
            } else {
                Chat.console(message);
            }
        }
    }
}