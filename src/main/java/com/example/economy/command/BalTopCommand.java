package com.example.economy.command;

import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import com.example.economy.storage.DatabaseManager;

public class BalTopCommand implements CommandExecutor {
    private final DatabaseManager databaseManager;
    private final PluginConfig pluginConfig;

    public BalTopCommand(DatabaseManager databaseManager, PluginConfig pluginConfig) {
        this.databaseManager = databaseManager;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
            }
        }
        int size = pluginConfig.baltopPageSize();
        int offset = (page - 1) * size;
        sender.sendMessage("BalTop page " + page + ":");
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT player_uuid, balance FROM balances ORDER BY balance DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = offset + 1;
                while (rs.next()) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(java.util.UUID.fromString(rs.getString("player_uuid")));
                    String name = offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName();
                    sender.sendMessage(rank + ". " + name + " - "
                            + MoneyFormat.pretty(pluginConfig.currencySymbol(), rs.getDouble("balance")));
                    rank++;
                }
            }
        } catch (SQLException ex) {
            sender.sendMessage("Error loading leaderboard.");
        }
        return true;
    }
}
