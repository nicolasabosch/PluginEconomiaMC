package com.example.economy.command;

import com.example.economy.lottery.LotteryService;
import java.sql.SQLException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LotteryCommand implements CommandExecutor {
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
        if (args.length != 2 || !args[0].equalsIgnoreCase("buy")) {
            player.sendMessage("Usage: /lottery buy <amount>");
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
}
