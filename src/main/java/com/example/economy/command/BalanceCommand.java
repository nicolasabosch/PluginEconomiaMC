package com.example.economy.command;

import com.example.economy.economy.EconomyService;
import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {
    private final EconomyService economyService;
    private final PluginConfig pluginConfig;

    public BalanceCommand(EconomyService economyService, PluginConfig pluginConfig) {
        this.economyService = economyService;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        try {
            double balance = economyService.getBalance(player.getUniqueId());
            player.sendMessage("Balance: " + MoneyFormat.pretty(pluginConfig.currencySymbol(), balance));
        } catch (SQLException ex) {
            player.sendMessage("Error loading balance.");
        }
        return true;
    }
}
