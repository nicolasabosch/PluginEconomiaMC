package com.example.economy.command;

import com.example.economy.lottery.LotteryService;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class LotteryCommand implements TabExecutor {
    private final LotteryService lotteryService;

    public LotteryCommand(LotteryService lotteryService) {
        this.lotteryService = lotteryService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sendLotterySuggestions(player);
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("buy")) {
            player.sendMessage("Usage: /lottery buy <amount>");
            sendLotterySuggestions(player);
            return true;
        }
        try {
            int amount = Integer.parseInt(args[1]);
            if (lotteryService.buyTickets(player, amount)) {
                player.sendMessage("Tickets purchased.");
            } else {
                player.sendMessage("Could not buy tickets.");
            }
        } catch (NumberFormatException ex) {
            player.sendMessage("Invalid ticket amount.");
        } catch (SQLException ex) {
            player.sendMessage("Lottery error.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("buy"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            return filterPrefix(List.of("1", "5", "10"), args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> options, String partial) {
        String lc = partial.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lc)).toList();
    }

    private void sendLotterySuggestions(Player player) {
        String command = "/lottery buy 1";
        player.sendMessage(
                Component.text("- " + command, NamedTextColor.LIGHT_PURPLE)
                        .clickEvent(ClickEvent.suggestCommand(command)));
    }
}
