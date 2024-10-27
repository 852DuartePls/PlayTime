package me.f64.playtime;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import me.f64.playtime.utils.DataStorage;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import me.f64.playtime.commands.PlaytimeCommands;
import me.f64.playtime.placeholderapi.Expansion;
import me.f64.playtime.utils.Chat;

public class PlayTime extends JavaPlugin implements Listener {
    public static Plugin plugin;
    private DataStorage dataStorage;
    public File pluginFolder = getDataFolder();
    public File dataFolder = new File(pluginFolder, "data");
    public File dbFile = new File(dataFolder, "players.db");

    public HashMap<String, Long> Sessions = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        PluginCommand command = getCommand("playtime");
        if (command != null) {
            command.setExecutor(new PlaytimeCommands(this));
            command.setTabCompleter(new PlaytimeCommands(this));
        }
        checkStorage();
        placeholderAPI();
    }

    private void placeholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Chat.console("&7[PlayTime] &bPlaceholderAPI &awas found&7! Registering Placeholders.");
            new Expansion(this).register();
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        savePlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        JSONObject target = new JSONObject();
        Sessions.put(player.getUniqueId().toString(), System.currentTimeMillis());

        Chat chat = new Chat(this);
        if (!(player.hasPlayedBefore())) {
            target.put("uuid", player.getUniqueId().toString());
            target.put("lastName", player.getName());
            target.put("time", chat.ticksPlayed(player) + 1);
            target.put("joins", player.getStatistic(Statistic.LEAVE_GAME) + 1);
            target.put("session", chat.ticksPlayed(player));
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> writePlayer(target));
        } else {
            final JSONParser jsonParser = new JSONParser();
            try {
                final FileReader reader = new FileReader(getPlayerPath(player.getName()));
                final JSONObject playerJSON = (JSONObject) jsonParser.parse(reader);
                reader.close();

                boolean changed = false;
                if (chat.ticksPlayed(player) + 1 > Integer.parseInt(playerJSON.get("time").toString())) {
                    target.put("time", chat.ticksPlayed(player) + 1);
                    changed = true;
                } else {
                    target.put("time", Integer.parseInt(playerJSON.get("time").toString()));
                }

                if (player.getStatistic(Statistic.LEAVE_GAME) > Integer.parseInt(playerJSON.get("joins").toString())) {
                    target.put("joins", player.getStatistic(Statistic.LEAVE_GAME));
                    changed = true;
                } else {
                    target.put("joins", Integer.parseInt(playerJSON.get("joins").toString()));
                }
                if (changed) {
                    target.put("uuid", player.getUniqueId().toString());
                    target.put("lastName", player.getName());
                    target.put("session", chat.ticksPlayed(player));
                    Bukkit.getScheduler().runTaskAsynchronously(this, () -> writePlayer(target));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public int getPlayerSession(final String name) {
        final JSONParser jsonParser = new JSONParser();
        try {
            final FileReader reader = new FileReader(getPlayerPath(name));
            final JSONObject player = (JSONObject) jsonParser.parse(reader);
            reader.close();

            if (player.get("lastName").equals(name)) {
                Chat chat = new Chat(this);
                final Player p = PlayTime.plugin.getServer().getPlayer(name);
                final int session = Integer.parseInt(player.get("session").toString());
                final int current = chat.ticksPlayed(p);
                return current - session;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
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

    public void savePlayer(Player player) {
        JSONObject target = new JSONObject();

        String uuid = player.getUniqueId().toString();
        int sessionOnTime = (int) (System.currentTimeMillis() - Sessions.get(uuid)) / 50;
        Sessions.remove(uuid);

        try {
            FileReader reader = new FileReader(getPlayerPath(player.getName()));

            JSONParser jsonParser = new JSONParser();
            JSONObject oldData = (JSONObject) jsonParser.parse(reader);
            reader.close();

            target.put("uuid", uuid);
            target.put("lastName", player.getName());
            target.put("time", Integer.parseInt(oldData.get("time").toString()) + sessionOnTime);
            target.put("joins", Integer.parseInt(oldData.get("joins").toString()) + 1);
            target.put("session", sessionOnTime);
        } catch (Exception e) {
            e.printStackTrace();

            // try legacy method
            Chat chat = new Chat(this);
            target.put("uuid", uuid);
            target.put("lastName", player.getName());
            target.put("time", chat.ticksPlayed(player));
            target.put("joins", player.getStatistic(Statistic.LEAVE_GAME) + 1);
            target.put("session", chat.ticksPlayed(player));
        }

        if (!Bukkit.getPluginManager().isPluginEnabled(this))
            writePlayer(target);
        else
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writePlayer(target));
    }

    private void writePlayer(JSONObject target) {
        String playerPath = getPlayerPath((String) target.get("lastName"));

        if (Bukkit.getPluginManager().isPluginEnabled(this) && Bukkit.isPrimaryThread()) {
            final JSONObject finalTarget = target;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writePlayer(finalTarget));
            return;
        }

        JSONParser jsonParser = new JSONParser();
        try {
            File userdataFile = new File(playerPath);
            if (!userdataFile.exists()) {
                try {
                    FileWriter writer = new FileWriter(userdataFile.getAbsoluteFile());
                    writer.write("{}");
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileReader reader = new FileReader(playerPath);
            JSONObject oldData = (JSONObject) jsonParser.parse(reader);
            reader.close();

            if (oldData.get("time") == null || Integer.parseInt(target.get("time").toString()) > Integer
                    .parseInt(oldData.get("time").toString())) {
                FileWriter writer = new FileWriter(playerPath);
                writer.write(target.toJSONString());
                writer.flush();
                writer.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}