package com.example.economy.economy;

import com.example.economy.storage.DatabaseManager;
import com.example.economy.util.PluginConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EconomyService {
    private final DatabaseManager databaseManager;
    private final PluginConfig pluginConfig;

    public EconomyService(DatabaseManager databaseManager, PluginConfig pluginConfig) {
        this.databaseManager = databaseManager;
        this.pluginConfig = pluginConfig;
    }

    public synchronized double getBalance(UUID playerUuid) throws SQLException {
        Connection connection = databaseManager.connection();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM balances WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        setBalance(playerUuid, pluginConfig.startingBalance(), null, null, TransactionType.ADMIN_SET, "{\"reason\":\"init\"}");
        return pluginConfig.startingBalance();
    }

    public synchronized boolean deposit(UUID playerUuid, double amount, UUID actor, TransactionType type, String metaJson)
            throws SQLException {
        if (amount <= 0.0D) {
            return false;
        }
        double balance = getBalance(playerUuid);
        double maxBalance = pluginConfig.maxBalance();
        if (balance + amount > maxBalance) {
            return false;
        }
        setBalance(playerUuid, balance + amount, actor, playerUuid, type, metaJson);
        return true;
    }

    public synchronized boolean withdraw(UUID playerUuid, double amount, UUID actor, TransactionType type, String metaJson)
            throws SQLException {
        if (amount <= 0.0D) {
            return false;
        }
        double balance = getBalance(playerUuid);
        if (balance < amount) {
            return false;
        }
        setBalance(playerUuid, balance - amount, actor, playerUuid, type, metaJson);
        return true;
    }

    public synchronized boolean transfer(UUID from, UUID to, double amount, String metaJson) throws SQLException {
        if (amount <= 0.0D || from.equals(to)) {
            return false;
        }
        Connection connection = databaseManager.connection();
        connection.setAutoCommit(false);
        try {
            double fromBalance = getBalance(from);
            double toBalance = getBalance(to);
            if (fromBalance < amount || toBalance + amount > pluginConfig.maxBalance()) {
                connection.rollback();
                connection.setAutoCommit(true);
                return false;
            }
            updateBalance(from, fromBalance - amount);
            updateBalance(to, toBalance + amount);
            insertTransaction(from, to, TransactionType.PAY_SENT, amount, metaJson);
            insertTransaction(from, to, TransactionType.PAY_RECEIVED, amount, metaJson);
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException ex) {
            connection.rollback();
            connection.setAutoCommit(true);
            throw ex;
        }
    }

    public synchronized void setBalance(UUID playerUuid, double amount, UUID actorUuid, UUID targetUuid,
            TransactionType type, String metaJson) throws SQLException {
        double finalAmount = Math.max(0.0D, Math.min(pluginConfig.maxBalance(), amount));
        updateBalance(playerUuid, finalAmount);
        insertTransaction(actorUuid, targetUuid, type, finalAmount, metaJson);
    }

    private void updateBalance(UUID playerUuid, double amount) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("""
                INSERT INTO balances(player_uuid, balance, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET balance=excluded.balance, updated_at=excluded.updated_at
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setDouble(2, amount);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
    }

    public synchronized List<TransactionRecord> history(UUID playerUuid, int limit) throws SQLException {
        int clamped = Math.max(1, Math.min(pluginConfig.historyMaxLimit(), limit));
        List<TransactionRecord> rows = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("""
                SELECT id, actor_uuid, target_uuid, type, amount, meta_json, created_at
                FROM transactions
                WHERE actor_uuid = ? OR target_uuid = ?
                ORDER BY id DESC
                LIMIT ?
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerUuid.toString());
            ps.setInt(3, clamped);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TransactionRecord(
                            rs.getLong("id"),
                            rs.getString("actor_uuid"),
                            rs.getString("target_uuid"),
                            TransactionType.valueOf(rs.getString("type")),
                            rs.getDouble("amount"),
                            rs.getString("meta_json"),
                            rs.getLong("created_at")));
                }
            }
        }
        return rows;
    }

    private void insertTransaction(UUID actorUuid, UUID targetUuid, TransactionType type, double amount, String metaJson)
            throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("""
                INSERT INTO transactions(actor_uuid, target_uuid, type, amount, meta_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, actorUuid == null ? null : actorUuid.toString());
            ps.setString(2, targetUuid == null ? null : targetUuid.toString());
            ps.setString(3, type.name());
            ps.setDouble(4, amount);
            ps.setString(5, metaJson == null ? "{}" : metaJson);
            ps.setLong(6, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
    }
}
