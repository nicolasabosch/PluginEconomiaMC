package com.example.economy.command;

import com.example.economy.economy.EconomyService;
import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class PayCommand implements TabExecutor {
    private final EconomyService economyService;
    private final PluginConfig pluginConfig;
    private final Map<UUID, PendingPay> pending = new HashMap<>();

    public PayCommand(EconomyService economyService, PluginConfig pluginConfig) {
        this.economyService = economyService;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sendPaySuggestions(player);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            return confirm(player);
        }
        if (args.length != 2) {
            player.sendMessage("Usage: /pay <player> <amount>");
            sendPaySuggestions(player);
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage("Unknown player.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("You cannot pay yourself.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid amount.");
            return true;
        }
        if (amount <= 0.0D) {
            player.sendMessage("Amount must be positive.");
            return true;
        }
        pending.put(player.getUniqueId(), new PendingPay(target.getUniqueId(), amount));
        player.sendMessage("Type /pay confirm to send " + MoneyFormat.pretty(pluginConfig.currencySymbol(), amount)
                + " to " + target.getName());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("confirm");
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            return filterPrefix(options, args[0]);
        }
        if (args.length == 2) {
            return filterPrefix(List.of("10", "100", "1000"), args[1]);
        }
        return Collections.emptyList();
    }

    private boolean confirm(Player player) {
        PendingPay pendingPay = pending.remove(player.getUniqueId());
        if (pendingPay == null) {
            player.sendMessage("No pending payment.");
            return true;
        }
        try {
            boolean ok = economyService.transfer(player.getUniqueId(), pendingPay.targetUuid(), pendingPay.amount(),
                    "{\"fromPayCommand\":true}");
            if (!ok) {
                player.sendMessage("Payment failed (insufficient funds or max balance limit reached).");
                return true;
            }
            player.sendMessage("Payment sent.");
            Player online = Bukkit.getPlayer(pendingPay.targetUuid());
            if (online != null) {
                online.sendMessage("You received " + MoneyFormat.pretty(pluginConfig.currencySymbol(), pendingPay.amount())
                        + " from " + player.getName());
            }
        } catch (SQLException ex) {
            player.sendMessage("Payment failed due to DB error.");
        }
        return true;
    }

    private record PendingPay(UUID targetUuid, double amount) {
    }

    private List<String> filterPrefix(List<String> options, String partial) {
        String lc = partial.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lc)) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private void sendPaySuggestions(Player player) {
        sendSuggestion(player, "/pay <player> 100");
        sendSuggestion(player, "/pay confirm");
    }

    private void sendSuggestion(Player player, String command) {
        player.sendMessage(
                Component.text("- " + command, NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand(command)));
    }
}
