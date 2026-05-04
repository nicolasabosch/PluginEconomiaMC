package com.example.economy.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private FileConfiguration config;

    public PluginConfig(FileConfiguration config) {
        this.config = config;
    }

    public void reload(FileConfiguration config) {
        this.config = config;
    }

    public String currencySymbol() {
        return config.getString("currency.symbol", "$");
    }

    public double startingBalance() {
        return config.getDouble("currency.starting-balance", 0.0D);
    }

    public double maxBalance() {
        return config.getDouble("currency.max-balance", 1_000_000_000.0D);
    }

    public int historyDefaultLimit() {
        return config.getInt("currency.history-default-limit", 10);
    }

    public int historyMaxLimit() {
        return config.getInt("currency.history-max-limit", 50);
    }

    public String shopTitle() {
        return config.getString("shop.gui-title", "Player Marketplace");
    }

    public int shopPageSize() {
        return Math.max(9, Math.min(45, config.getInt("shop.page-size", 45)));
    }

    public double shopTaxPercent() {
        return Math.max(0.0D, config.getDouble("shop.tax-percent", 5.0D));
    }

    public int listingExpiryDays() {
        return Math.max(1, config.getInt("shop.listing-expiry-days", 7));
    }

    public int baltopPageSize() {
        return Math.max(5, Math.min(50, config.getInt("baltop.page-size", 10)));
    }

    public boolean lotteryEnabled() {
        return config.getBoolean("lottery.enabled", true);
    }

    public int lotteryDrawIntervalMinutes() {
        return Math.max(1, config.getInt("lottery.draw-interval-minutes", 60));
    }

    public double lotteryTicketPrice() {
        return Math.max(0.0D, config.getDouble("lottery.ticket-price", 100.0D));
    }

    public Map<Material, Double> sellPrices() {
        Map<Material, Double> prices = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("sell-prices");
        if (section == null) {
            return prices;
        }
        for (String key : section.getKeys(false)) {
            Optional.ofNullable(Material.matchMaterial(key)).ifPresent(material -> {
                double price = Math.max(0.0D, section.getDouble(key, 0.0D));
                prices.put(material, price);
            });
        }
        return prices;
    }
}
