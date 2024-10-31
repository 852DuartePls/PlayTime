package me.f64.playtime.utils;

import me.f64.playtime.PlayTime;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigHandler {
    private final PlayTime plugin;
    private final ConfigWrapper config;

    public ConfigHandler(PlayTime plugin) {
        this.plugin = plugin;
        this.config = new ConfigWrapper(plugin, null, "config.yml");
        createFile();
    }

    private void createFile() {
        config.createFile(null,
                """
                        # PlaytimeCommands By F64_Rx - Need Help? PM me on Spigot or post in the discussion.\r
                        \r
                        #    =================\r
                        #    | CONFIGURATION |\r
                        #    =================\r
                        \r
                        # Available placeholders:\r
                        #    %playtime_player% - returns the player name\r
                        #    %playtime_time% - shows time played\r
                        #    %playtime_timesjoined% - shows the amount of times the player has joined the server\r
                        #    %playtime_serveruptime% - shows the uptime of the server\r
                        #    %playtime_position% - shows the players current position\r
                        #    %playtime_top_#_name% - shows the name of the top 10\r
                        #    %playtime_top_#_time% - shows the time of the top 10\r
                        #    You can also use any other placeholder that PlaceholderAPI supports :) \r
                        """);
        setDefaults();
    }

    private void setDefaults() {
        FileConfiguration fileConfig = config.getConfig();
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
        fileConfig.addDefault("messages.plugin_version", List.of("&8[&bPlayTime&8] &bVersion: &7" + plugin.getDescription().getVersion()));
        fileConfig.addDefault("messages.no_permission", List.of("&8[&bPlayTime&8] &cYou don't have permission."));
        fileConfig.addDefault("messages.doesnt_exist", List.of("&8[&bPlayTime&8] &cPlayer %offlineplayer% has never joined before!"));
        fileConfig.addDefault("messages.player", List.of("&b%playtime_player%'s Stats are:", "&bPlayTime: &7%playtime_time%", "&bTimes Joined: &7%playtime_timesjoined%"));
        fileConfig.addDefault("messages.offline_players", List.of("&b%offlineplayer%'s Stats are:", "&bPlayTime: &7%offlinetime%", "&bTimes Joined: &7%offlinetimesjoined%", "&bLast Online: &7%lastonline%"));
        fileConfig.addDefault("messages.other_players", List.of("&b%playtime_player%'s Stats are:", "&bPlayTime: &7%playtime_time%", "&bTimes Joined: &7%playtime_timesjoined%"));
        fileConfig.addDefault("messages.playtimetop.header", List.of("&bTop &e10 &bplayers playtime:", ""));
        fileConfig.addDefault("messages.playtimetop.message", List.of("&a%position%. &b%player%: &e%playtime%"));
        fileConfig.addDefault("messages.playtimetop.footer", List.of(""));
        fileConfig.addDefault("messages.server_uptime", List.of("&8[&bPlayTime&8] &bServer's total uptime is %playtime_serveruptime%"));
        fileConfig.addDefault("messages.reload_config", List.of("&8[&bPlayTime&8] &bYou have successfully reloaded the config."));
        fileConfig.addDefault("placeholder.top.name", "none");
        fileConfig.addDefault("placeholder.top.time", "-");
        fileConfig.options().copyDefaults(true);
        config.saveConfig();
    }

    public FileConfiguration getConfig() {
        return config.getConfig();
    }

    public void reloadConfig() {
        config.reloadConfig();
    }
}
