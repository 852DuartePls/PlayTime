package me.f64.playtime;

import me.f64.playtime.commands.PlaytimeCommands;
import me.f64.playtime.placeholderapi.Expansion;
import me.f64.playtime.utils.Chat;
import me.f64.playtime.utils.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class PlayTime extends JavaPlugin implements Listener {
    public static Plugin plugin;
    private DataStorage dataStorage;
    private Chat chat;
    public File pluginFolder = getDataFolder();
    public File dataFolder = new File(pluginFolder, "data");
    public File dbFile = new File(dataFolder, "players.db");

    public HashMap<String, Long> Sessions = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        dataStorage = new DataStorage(this);
        chat = new Chat(this, dataStorage);
        PluginCommand command = getCommand("playtime");
        if (command != null) {
            command.setExecutor(new PlaytimeCommands(this, dataStorage));
            command.setTabCompleter(new PlaytimeCommands(this, dataStorage));
        }
        checkStorage();
        placeholderAPI();
    }

    private void placeholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Chat.console("&7[PlayTime] &bPlaceholderAPI &awas found&7! Registering Placeholders.");
            new Expansion(this, dataStorage).register();
            Bukkit.getPluginManager().registerEvents(this, this);
        } else {
            Chat.console("&7[PlayTime] &bPlaceholderAPI &cwas not found&7! Disabling Plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(this::savePlayer);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        savePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        if (!Sessions.containsKey(uuid)) {
            Sessions.put(uuid, System.currentTimeMillis());
        }

        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());

        int playTime;
        int joins;

        if (playerData == null) {
            playTime = 0;
            joins = 1;
        } else {
            playTime = playerData.playTime();
            joins = playerData.joins() + 1;
        }
        int sessionOnTime = (int) (System.currentTimeMillis() - Sessions.get(uuid)) / 1000;
        playTime += sessionOnTime;

        dataStorage.savePlayerData(player, playTime, joins, sessionOnTime);

    }

    public int getPlayerSession(final @NotNull Player player) {
        String name = player.getName();
        UUID uuid = player.getUniqueId();
        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(uuid);

        if (playerData == null) {
            return 0;
        }

        final Player onlinePlayer = PlayTime.plugin.getServer().getPlayer(name);
        if (onlinePlayer != null) {
            final int session = playerData.session();
            final int current = chat.ticksPlayed(onlinePlayer);
            return current - session;
        } else {
            return playerData.playTime() - playerData.session();
        }
    }

    private void checkStorage() {
        if (!pluginFolder.exists()) {
            if (pluginFolder.mkdirs()) {
                this.getLogger().info("PlayTime folder created!");
            } else {
                this.getLogger().warning("Could not create PlayTime folder!");
            }
        }

        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                this.getLogger().info("PlayTime data folder created!");
            } else {
                this.getLogger().warning("Could not create PlayTime data folder!");
            }
        }

        if (!dbFile.exists()) {
            try {
                if (dbFile.createNewFile()) {
                    this.getLogger().info("PlayTime data file created!");
                } else {
                    this.getLogger().warning("Could not create PlayTime data file!");
                }
            } catch (IOException e) {
                this.getLogger().warning("Could not create PlayTime data for players!");
            }
        }

        this.dataStorage = new DataStorage(this);
    }

    public void savePlayer(@NotNull Player player) {
        String uuid = player.getUniqueId().toString();

        if (!Sessions.containsKey(uuid)) {
            return;
        }

        int sessionOnTime = (int) (System.currentTimeMillis() - Sessions.get(uuid)) / 1000; // in seconds
        Sessions.remove(uuid);

        DataStorage.PlayerData playerData = dataStorage.loadPlayerData(player.getUniqueId());
        if (playerData != null) {
            int time = playerData.playTime() + sessionOnTime;
            int joins = playerData.joins();

            dataStorage.savePlayerData(player, time, joins, sessionOnTime);
        } else {
            dataStorage.savePlayerData(player, sessionOnTime, 1, sessionOnTime);
        }
    }
}