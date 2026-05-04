package com.example.economy.sell;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionType;
import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SellCommand implements CommandExecutor, Listener {
    private static final String GUI_TITLE = "Quick Sell";
    private final PluginConfig pluginConfig;
    private final EconomyService economyService;
    private final Map<UUID, Map<Integer, Material>> slotMap = new HashMap<>();

    public SellCommand(PluginConfig pluginConfig, EconomyService economyService) {
        this.pluginConfig = pluginConfig;
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE);
        Map<Integer, Material> slots = new HashMap<>();
        int slot = 0;
        for (Map.Entry<Material, Double> entry : pluginConfig.sellPrices().entrySet()) {
            if (slot >= 54) {
                break;
            }
            ItemStack icon = new ItemStack(entry.getKey());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(entry.getKey().name() + " - "
                        + MoneyFormat.pretty(pluginConfig.currencySymbol(), entry.getValue()));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            slots.put(slot, entry.getKey());
            slot++;
        }
        slotMap.put(player.getUniqueId(), slots);
        player.openInventory(inventory);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        Map<Integer, Material> map = slotMap.get(player.getUniqueId());
        if (map == null) {
            return;
        }
        Material material = map.get(event.getRawSlot());
        if (material == null) {
            return;
        }
        int amount = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) {
                amount += stack.getAmount();
            }
        }
        if (amount <= 0) {
            player.sendMessage("You do not have that item.");
            return;
        }
        player.getInventory().remove(material);
        double total = amount * pluginConfig.sellPrices().getOrDefault(material, 0.0D);
        try {
            economyService.deposit(player.getUniqueId(), total, player.getUniqueId(), TransactionType.SELL_QUICK,
                    "{\"material\":\"" + material.name() + "\",\"amount\":" + amount + "}");
            player.sendMessage("Sold " + amount + "x " + material.name() + " for "
                    + MoneyFormat.pretty(pluginConfig.currencySymbol(), total));
        } catch (SQLException ex) {
            player.sendMessage("Could not complete sell.");
        }
    }
}
