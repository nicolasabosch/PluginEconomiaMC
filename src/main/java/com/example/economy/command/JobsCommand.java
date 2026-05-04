package com.example.economy.command;

import com.example.economy.jobs.JobsService;
import java.sql.SQLException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor {
    private final JobsService jobsService;

    public JobsCommand(JobsService jobsService) {
        this.jobsService = jobsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
                player.sendMessage("Jobs: Miner, Lumberjack, Farmer, Hunter, Fisher");
                return true;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
                boolean ok = jobsService.selectJob(player.getUniqueId(), args[1]);
                player.sendMessage(ok ? "Job selected: " + args[1] : "Unknown job.");
                return true;
            }
            String current = jobsService.getSelectedJob(player.getUniqueId());
            player.sendMessage("Current job: " + (current == null ? "None" : current));
        } catch (SQLException ex) {
            player.sendMessage("Jobs error.");
        }
        return true;
    }
}
