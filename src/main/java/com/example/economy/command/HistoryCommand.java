package com.example.economy.command;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionRecord;
import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HistoryCommand implements CommandExecutor {
    private final EconomyService economyService;
    private final PluginConfig pluginConfig;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public HistoryCommand(EconomyService economyService, PluginConfig pluginConfig) {
        this.economyService = economyService;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        int limit = pluginConfig.historyDefaultLimit();
        if (args.length >= 1) {
            try {
                limit = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            List<TransactionRecord> rows = economyService.history(player.getUniqueId(), limit);
            player.sendMessage("Recent transactions:");
            for (TransactionRecord row : rows) {
                String date = Instant.ofEpochMilli(row.createdAt()).atZone(ZoneId.systemDefault()).format(formatter);
                player.sendMessage("#" + row.id() + " " + row.type().name() + " "
                        + MoneyFormat.pretty(pluginConfig.currencySymbol(), row.amount()) + " at " + date);
            }
        } catch (SQLException ex) {
            player.sendMessage("Error loading history.");
        }
        return true;
    }
}
