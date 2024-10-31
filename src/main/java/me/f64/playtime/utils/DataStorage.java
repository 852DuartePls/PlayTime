package me.f64.playtime.utils;

import me.f64.playtime.PlayTime;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataStorage {
    private final PlayTime plugin;
    private final String databaseUrl;

    public DataStorage(@NotNull PlayTime plugin) {
        this.plugin = plugin;
        this.databaseUrl = "jdbc:sqlite:" + plugin.dbFile.getAbsolutePath();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "playerName TEXT, " +
                    "time INTEGER, " +
                    "joins INTEGER, " +
                    "session INTEGER, " +
                    "lastOnline INTEGER)";
            statement.execute(sql);
            plugin.getLogger().info("Database initialized successfully!");
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not initialize database: " + e
                    + " -- If this the first time running this plugin, you can ignore this warning! :)");
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    public void savePlayerData(@NotNull Player player, int playTime, int joins, int session) {
        String query = "INSERT OR REPLACE INTO players (uuid, playerName, time, joins, session, lastOnline) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getName());
            statement.setInt(3, playTime);
            statement.setInt(4, joins);
            statement.setInt(5, session);
            statement.setLong(6, System.currentTimeMillis() / 1000);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not save player data: " + e);
        }
    }

    public PlayerData loadPlayerData(@NotNull UUID uuid) {
        String query = "SELECT * FROM players WHERE uuid = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String playerName = resultSet.getString("playerName");
                int playTime = resultSet.getInt("time");
                int joins = resultSet.getInt("joins");
                int session = resultSet.getInt("session");
                long lastOnline = resultSet.getLong("lastOnline");

                return new PlayerData(uuid, playerName, playTime, joins, session, lastOnline);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not load player data: " + e);
        }
        return null;
    }

    public List<PlayerData> loadAllPlayerData() {
        List<PlayerData> players = new ArrayList<>();
        String query = "SELECT * FROM players";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String playerName = resultSet.getString("playerName");
                int playTime = resultSet.getInt("time");
                int joins = resultSet.getInt("joins");
                int session = resultSet.getInt("session");
                long lastOnline = resultSet.getLong("lastOnline");

                players.add(new PlayerData(uuid, playerName, playTime, joins, session, lastOnline));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not load all player data: " + e);
        }
        return players;
    }

    public UUID getPlayerUUIDByName(String playerName) {
        String query = "SELECT uuid FROM players WHERE playerName = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not retrieve UUID for player " + playerName + ": " + e);
        }
        return null;
    }

    public record PlayerData(UUID uuid, String playerName, int playTime, int joins, int session, long lastOnline) {
    }
}
