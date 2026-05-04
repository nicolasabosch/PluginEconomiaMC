package com.example.economy.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "economy.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        connection.setAutoCommit(true);
    }

    public Connection connection() {
        return connection;
    }

    public void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS balances (
                    player_uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    actor_uuid TEXT,
                    target_uuid TEXT,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    meta_json TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS listings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    item_base64 TEXT NOT NULL,
                    item_name TEXT NOT NULL,
                    price REAL NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_listings_expires ON listings(expires_at)");
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pending_notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    message TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pending_returns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    item_base64 TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS jobs_state (
                    player_uuid TEXT PRIMARY KEY,
                    job_key TEXT NOT NULL,
                    switched_at INTEGER NOT NULL
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lottery_rounds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    status TEXT NOT NULL,
                    pot REAL NOT NULL,
                    draw_at INTEGER NOT NULL,
                    winner_uuid TEXT
                )
                """);
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lottery_tickets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    round_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
