package com.example.economy.command;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionType;
import com.example.economy.storage.DatabaseManager;
import com.example.economy.util.AuditLogger;
import java.sql.PreparedStatement;
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

public class EcoAdminCommand implements TabExecutor {
    private final EconomyService economyService;
    private final DatabaseManager databaseManager;
    private final AuditLogger auditLogger;
    private final Runnable reloadHook;
    private final Map<UUID, String> pendingConfirmations = new HashMap<>();

    public EcoAdminCommand(EconomyService economyService, DatabaseManager databaseManager, AuditLogger auditLogger, Runnable reloadHook) {
        this.economyService = economyService;
        this.databaseManager = databaseManager;
        this.auditLogger = auditLogger;
        this.reloadHook = reloadHook;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("friendseconomy.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            sendEcoSuggestions(sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadHook.run();
            sender.sendMessage("Config reloaded.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("resetall")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Console must run /eco resetall confirm");
                return true;
            }
            pendingConfirmations.put(player.getUniqueId(), "resetall");
            sender.sendMessage("Type /eco confirm to execute resetall.");
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetall") && args[1].equalsIgnoreCase("confirm")) {
            return executeResetAll(sender);
        }
        if (sender instanceof Player p && args.length == 1 && args[0].equalsIgnoreCase("confirm")) {
            String pending = pendingConfirmations.remove(p.getUniqueId());
            if ("resetall".equals(pending)) {
                return executeResetAll(sender);
            }
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /eco <give|take|set|reset|resetall|reload>");
            return true;
        }
        try {
            String sub = args[0].toLowerCase();
            if (args.length < 3) {
                if (!sub.equals("reset")) {
                    sender.sendMessage("Usage: /eco " + sub + " <player> <amount>");
                    return true;
                }
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("Unknown player.");
                return true;
            }
            switch (sub) {
                case "give" -> {
                    double amount = Double.parseDouble(args[2]);
                    if (amount <= 0.0D) {
                        sender.sendMessage("Amount must be positive.");
                        return true;
                    }
                    boolean ok = economyService.deposit(target.getUniqueId(), amount, null, TransactionType.ADMIN_GIVE, "{\"admin\":true}");
                    if (!ok) {
                        sender.sendMessage("Could not apply give (max balance limit).");
                        return true;
                    }
                    auditLogger.log(sender.getName() + " gave " + amount + " to " + target.getUniqueId());
                }
                case "take" -> {
                    double amount = Double.parseDouble(args[2]);
                    if (amount <= 0.0D) {
                        sender.sendMessage("Amount must be positive.");
                        return true;
                    }
                    boolean ok = economyService.withdraw(target.getUniqueId(), amount, null, TransactionType.ADMIN_TAKE, "{\"admin\":true}");
                    if (!ok) {
                        sender.sendMessage("Could not apply take (insufficient balance).");
                        return true;
                    }
                    auditLogger.log(sender.getName() + " took " + amount + " from " + target.getUniqueId());
                }
                case "set" -> {
                    double amount = Double.parseDouble(args[2]);
                    economyService.setBalance(target.getUniqueId(), amount, null, target.getUniqueId(), TransactionType.ADMIN_SET, "{\"admin\":true}");
                    auditLogger.log(sender.getName() + " set " + target.getUniqueId() + " balance to " + amount);
                }
                case "reset" -> {
                    economyService.setBalance(target.getUniqueId(), 0.0D, null, target.getUniqueId(), TransactionType.ADMIN_RESET, "{\"admin\":true}");
                    auditLogger.log(sender.getName() + " reset " + target.getUniqueId());
                }
                default -> sender.sendMessage("Unknown subcommand.");
            }
            sender.sendMessage("Admin command executed.");
        } catch (Exception ex) {
            sender.sendMessage("Admin command failed. Check usage and values.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("give", "take", "set", "reset", "resetall", "reload", "confirm", "help"), args[0]);
        }
        if (args.length == 2 && List.of("give", "take", "set", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filterPrefix(names, args[1]);
        }
        if (args.length == 3 && List.of("give", "take", "set").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filterPrefix(List.of("100", "1000", "10000"), args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("resetall")) {
            return filterPrefix(List.of("confirm"), args[1]);
        }
        return Collections.emptyList();
    }

    private boolean executeResetAll(CommandSender sender) {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("UPDATE balances SET balance = 0");
                PreparedStatement tr = databaseManager.connection().prepareStatement(
                        "INSERT INTO transactions(actor_uuid, target_uuid, type, amount, meta_json, created_at) VALUES (NULL, NULL, 'ADMIN_RESET_ALL', 0, '{\"admin\":true}', strftime('%s','now') * 1000)")) {
            ps.executeUpdate();
            tr.executeUpdate();
            auditLogger.log(sender.getName() + " ran resetall");
            sender.sendMessage("All balances reset.");
        } catch (SQLException ex) {
            sender.sendMessage("Resetall failed.");
        }
        return true;
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

    private void sendEcoSuggestions(CommandSender sender) {
        sender.sendMessage("Eco admin options:");
        sendSuggestion(sender, "/eco give <player> 100");
        sendSuggestion(sender, "/eco take <player> 100");
        sendSuggestion(sender, "/eco set <player> 1000");
        sendSuggestion(sender, "/eco reset <player>");
        sendSuggestion(sender, "/eco resetall");
        sendSuggestion(sender, "/eco reload");
    }

    private void sendSuggestion(CommandSender sender, String command) {
        if (sender instanceof Player player) {
            player.sendMessage(
                    Component.text("- " + command, NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.suggestCommand(command)));
            return;
        }
        sender.sendMessage("- " + command);
    }
}
