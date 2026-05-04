package com.example.economy.lottery;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionType;
import com.example.economy.storage.DatabaseManager;
import com.example.economy.util.PluginConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LotteryService {
    private final DatabaseManager databaseManager;
    private final EconomyService economyService;
    private final PluginConfig pluginConfig;
    private final Random random = new Random();

    public LotteryService(DatabaseManager databaseManager, EconomyService economyService, PluginConfig pluginConfig) {
        this.databaseManager = databaseManager;
        this.economyService = economyService;
        this.pluginConfig = pluginConfig;
    }

    public synchronized long currentRoundId() throws SQLException {
        try (PreparedStatement ps = databaseManager.connection()
                .prepareStatement("SELECT id FROM lottery_rounds WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        long drawAt = Instant.now().toEpochMilli() + pluginConfig.lotteryDrawIntervalMinutes() * 60_000L;
        try (PreparedStatement create = databaseManager.connection().prepareStatement(
                "INSERT INTO lottery_rounds(status, pot, draw_at) VALUES ('OPEN', 0, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            create.setLong(1, drawAt);
            create.executeUpdate();
            try (ResultSet keys = create.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public synchronized boolean buyTickets(Player player, int amount) throws SQLException {
        if (amount <= 0) {
            return false;
        }
        double cost = pluginConfig.lotteryTicketPrice() * amount;
        if (!economyService.withdraw(player.getUniqueId(), cost, player.getUniqueId(), TransactionType.LOTTERY_TICKET,
                "{\"tickets\":" + amount + "}")) {
            return false;
        }
        long round = currentRoundId();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "INSERT INTO lottery_tickets(round_id, player_uuid, amount, created_at) VALUES (?, ?, ?, ?)")) {
            ps.setLong(1, round);
            ps.setString(2, player.getUniqueId().toString());
            ps.setInt(3, amount);
            ps.setLong(4, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
        try (PreparedStatement pot = databaseManager.connection()
                .prepareStatement("UPDATE lottery_rounds SET pot = pot + ? WHERE id = ?")) {
            pot.setDouble(1, cost);
            pot.setLong(2, round);
            pot.executeUpdate();
        }
        return true;
    }

    public synchronized void drawIfNeeded() throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT id, pot, draw_at FROM lottery_rounds WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1");
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return;
            }
            long roundId = rs.getLong("id");
            double pot = rs.getDouble("pot");
            long drawAt = rs.getLong("draw_at");
            if (Instant.now().toEpochMilli() < drawAt) {
                return;
            }
            UUID winner = pickWinner(roundId);
            if (winner != null && pot > 0.0D) {
                economyService.deposit(winner, pot, null, TransactionType.LOTTERY_WIN, "{\"round\":" + roundId + "}");
                Bukkit.broadcastMessage("Lottery winner: " + Bukkit.getOfflinePlayer(winner).getName() + " won " + pot);
                try (PreparedStatement upd = databaseManager.connection()
                        .prepareStatement("UPDATE lottery_rounds SET status = 'CLOSED', winner_uuid = ? WHERE id = ?")) {
                    upd.setString(1, winner.toString());
                    upd.setLong(2, roundId);
                    upd.executeUpdate();
                }
            } else {
                try (PreparedStatement upd = databaseManager.connection()
                        .prepareStatement("UPDATE lottery_rounds SET status = 'CLOSED' WHERE id = ?")) {
                    upd.setLong(1, roundId);
                    upd.executeUpdate();
                }
            }
            currentRoundId();
        }
    }

    private UUID pickWinner(long roundId) throws SQLException {
        List<UUID> pool = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT player_uuid, amount FROM lottery_tickets WHERE round_id = ?")) {
            ps.setLong(1, roundId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    int amount = rs.getInt("amount");
                    for (int i = 0; i < amount; i++) {
                        pool.add(uuid);
                    }
                }
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
