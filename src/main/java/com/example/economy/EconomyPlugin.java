package com.example.economy;

import com.example.economy.command.BalTopCommand;
import com.example.economy.command.BalanceCommand;
import com.example.economy.command.EcoAdminCommand;
import com.example.economy.command.HistoryCommand;
import com.example.economy.command.JobsCommand;
import com.example.economy.command.LotteryCommand;
import com.example.economy.command.PayCommand;
import com.example.economy.economy.EconomyService;
import com.example.economy.jobs.JobsListener;
import com.example.economy.jobs.JobsService;
import com.example.economy.lottery.LotteryService;
import com.example.economy.sell.SellCommand;
import com.example.economy.shop.MarketplaceService;
import com.example.economy.shop.NotificationsListener;
import com.example.economy.shop.ShopCommand;
import com.example.economy.storage.DatabaseManager;
import com.example.economy.util.AuditLogger;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private PluginConfig pluginConfig;
    private EconomyService economyService;
    private MarketplaceService marketplaceService;
    private LotteryService lotteryService;
    private JobsService jobsService;
    private AuditLogger auditLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        pluginConfig = new PluginConfig(getConfig());
        auditLogger = new AuditLogger(this);
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
            databaseManager.initSchema();
        } catch (SQLException ex) {
            getLogger().severe("Failed to initialize database: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economyService = new EconomyService(databaseManager, pluginConfig);
        marketplaceService = new MarketplaceService(databaseManager, economyService, pluginConfig);
        lotteryService = new LotteryService(databaseManager, economyService, pluginConfig);
        jobsService = new JobsService(databaseManager, economyService, getConfig());

        SellCommand sellCommand = new SellCommand(pluginConfig, economyService);
        ShopCommand shopCommand = new ShopCommand(marketplaceService, pluginConfig);

        registerCommand("balance", new BalanceCommand(economyService, pluginConfig));
        registerCommand("pay", new PayCommand(economyService, pluginConfig));
        registerCommand("sell", sellCommand);
        registerCommand("shop", shopCommand);
        registerCommand("history", new HistoryCommand(economyService, pluginConfig));
        registerCommand("baltop", new BalTopCommand(databaseManager, pluginConfig));
        registerCommand("lottery", new LotteryCommand(lotteryService));
        registerCommand("jobs", new JobsCommand(jobsService));
        registerCommand("eco", new EcoAdminCommand(economyService, databaseManager, auditLogger, () -> {
            // Reuse the same config wrapper instances so command/listener references stay valid after reload.
            reloadConfig();
            pluginConfig.reload(getConfig());
            jobsService.reload(getConfig());
        }));

        Bukkit.getPluginManager().registerEvents(sellCommand, this);
        Bukkit.getPluginManager().registerEvents(shopCommand, this);
        Bukkit.getPluginManager().registerEvents(new NotificationsListener(marketplaceService), this);
        Bukkit.getPluginManager().registerEvents(new JobsListener(jobsService), this);

        // These run on the main thread because they may touch player inventories/messages (Bukkit API is not thread-safe).
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                marketplaceService.processExpiredListings();
            } catch (SQLException ex) {
                getLogger().warning("Expiry task failed: " + ex.getMessage());
            }
        }, 20L * 60L, 20L * 60L * 10L);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!pluginConfig.lotteryEnabled()) {
                return;
            }
            try {
                lotteryService.drawIfNeeded();
            } catch (SQLException ex) {
                getLogger().warning("Lottery task failed: " + ex.getMessage());
            }
        }, 20L * 60L, 20L * 60L);
    }

    @Override
    public void onDisable() {
        databaseManager.close();
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            if (executor instanceof TabCompleter completer) {
                command.setTabCompleter(completer);
            }
        } else {
            getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }
}
