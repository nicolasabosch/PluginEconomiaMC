package com.example.economy.command;

import com.example.economy.jobs.JobsService;
import java.sql.SQLException;
import java.util.ArrayList;
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

public class JobsCommand implements TabExecutor {
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
            if (args.length == 0) {
                String current = jobsService.getSelectedJob(player.getUniqueId());
                player.sendMessage("Current job: " + (current == null ? "None" : current));
                player.sendMessage("Use /jobs select to see available jobs.");
                return true;
            }
            if (args[0].equalsIgnoreCase("list")) {
                sendJobSuggestions(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("select") && args.length == 1) {
                // Show ready-to-copy commands so players can click/auto-complete quickly.
                sendJobSuggestions(player);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(List.of("select", "list"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            return filterPrefix(jobsService.availableJobs(), args[1]);
        }
        return Collections.emptyList();
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

    private void sendJobSuggestions(Player player) {
        List<String> jobs = jobsService.availableJobs();
        if (jobs.isEmpty()) {
            player.sendMessage("No jobs configured.");
            return;
        }
        player.sendMessage("Available jobs. Click a line to autocomplete:");
        for (String job : jobs) {
            String command = "/jobs select " + job;
            player.sendMessage(
                    Component.text("- " + command, NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand(command)));
        }
    }
}
