package com.example.economy.jobs;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionType;
import com.example.economy.storage.DatabaseManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class JobsService {
    private final DatabaseManager databaseManager;
    private final EconomyService economyService;
    private FileConfiguration config;

    public JobsService(DatabaseManager databaseManager, EconomyService economyService, FileConfiguration config) {
        this.databaseManager = databaseManager;
        this.economyService = economyService;
        this.config = config;
    }

    public void reload(FileConfiguration config) {
        this.config = config;
    }

    public String getSelectedJob(UUID playerUuid) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection()
                .prepareStatement("SELECT job_key FROM jobs_state WHERE player_uuid = ?")) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("job_key");
                }
            }
        }
        return null;
    }

    public boolean selectJob(UUID playerUuid, String job) throws SQLException {
        if (config.getConfigurationSection("jobs." + job) == null) {
            return false;
        }
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("""
                INSERT INTO jobs_state(player_uuid, job_key, switched_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_uuid) DO UPDATE SET job_key = excluded.job_key, switched_at = excluded.switched_at
                """)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, job);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
            return true;
        }
    }

    public void rewardBlock(UUID playerUuid, Material material) throws SQLException {
        String job = getSelectedJob(playerUuid);
        if (job == null) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection("jobs." + job + ".materials");
        if (section == null) {
            return;
        }
        double payout = section.getDouble(material.name(), 0.0D);
        if (payout > 0.0D) {
            economyService.deposit(playerUuid, payout, null, TransactionType.JOB_PAYOUT,
                    "{\"job\":\"" + job + "\",\"material\":\"" + material.name() + "\"}");
        }
    }

    public void rewardMob(UUID playerUuid, EntityType entityType) throws SQLException {
        String job = getSelectedJob(playerUuid);
        if (job == null) {
            return;
        }
        ConfigurationSection section = config.getConfigurationSection("jobs." + job + ".mobs");
        if (section == null) {
            return;
        }
        double payout = section.getDouble(entityType.name(), 0.0D);
        if (payout > 0.0D) {
            economyService.deposit(playerUuid, payout, null, TransactionType.JOB_PAYOUT,
                    "{\"job\":\"" + job + "\",\"mob\":\"" + entityType.name() + "\"}");
        }
    }

    public void rewardFish(UUID playerUuid) throws SQLException {
        String job = getSelectedJob(playerUuid);
        if (job == null) {
            return;
        }
        double payout = config.getDouble("jobs." + job + ".fish-catch", 0.0D);
        if (payout > 0.0D) {
            economyService.deposit(playerUuid, payout, null, TransactionType.JOB_PAYOUT,
                    "{\"job\":\"" + job + "\",\"action\":\"fish\"}");
        }
    }
}
