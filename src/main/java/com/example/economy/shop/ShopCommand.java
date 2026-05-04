package com.example.economy.shop;

import com.example.economy.util.MoneyFormat;
import com.example.economy.util.PluginConfig;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopCommand implements CommandExecutor, Listener {
    private final MarketplaceService marketplaceService;
    private final PluginConfig pluginConfig;
    private final Map<UUID, ListingContext> current = new HashMap<>();

    public ShopCommand(MarketplaceService marketplaceService, PluginConfig pluginConfig) {
        this.marketplaceService = marketplaceService;
        this.pluginConfig = pluginConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    player.sendMessage("Hold an item to list.");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("Usage: /shop list <price>");
                    return true;
                }
                double price;
                try {
                    price = Double.parseDouble(args[1]);
                } catch (NumberFormatException ex) {
                    player.sendMessage("Price must be numeric.");
                    return true;
                }
                if (price <= 0.0D) {
                    player.sendMessage("Price must be positive.");
                    return true;
                }
                ItemStack listingItem = hand.clone();
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                long id = marketplaceService.createListing(player.getUniqueId(), listingItem, price);
                player.sendMessage("Listed item with ID #" + id);
                return true;
            }
            if (args.length > 1 && args[0].equalsIgnoreCase("cancel")) {
                long id;
                try {
                    id = Long.parseLong(args[1]);
                } catch (NumberFormatException ex) {
                    player.sendMessage("Listing id must be numeric.");
                    return true;
                }
                var listingOpt = marketplaceService.findById(id);
                if (listingOpt.isEmpty()) {
                    player.sendMessage("Listing not found.");
                    return true;
                }
                Listing listing = listingOpt.get();
                if (!listing.sellerUuid().equals(player.getUniqueId())) {
                    player.sendMessage("You can only cancel your own listings.");
                    return true;
                }
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("No free inventory slot.");
                    return true;
                }
                player.getInventory().addItem(listing.itemStack());
                marketplaceService.deleteListing(id);
                player.sendMessage("Listing cancelled and returned.");
                return true;
            }
            String search = "";
            int page = 1;
            if (args.length > 0 && args[0].equalsIgnoreCase("search")) {
                search = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                if (search.isBlank()) {
                    player.sendMessage("Usage: /shop search <text>");
                    return true;
                }
            } else if (args.length > 0) {
                try {
                    page = Math.max(1, Integer.parseInt(args[0]));
                } catch (NumberFormatException ex) {
                    player.sendMessage("Page must be numeric.");
                    return true;
                }
            }
            openPage(player, page, search);
        } catch (Exception ex) {
            player.sendMessage("Shop operation failed.");
        }
        return true;
    }

    private void openPage(Player player, int page, String search) throws SQLException {
        int size = 54;
        String title = pluginConfig.shopTitle() + " p" + page;
        Inventory inv = Bukkit.createInventory(null, size, title);
        List<Listing> listings = marketplaceService.listPage(page, search == null ? "" : search);
        Map<Integer, Long> slotToId = new HashMap<>();
        for (int i = 0; i < Math.min(45, listings.size()); i++) {
            Listing listing = listings.get(i);
            ItemStack icon = listing.itemStack().clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(listing.itemName() + " #" + listing.id());
                meta.setLore(List.of(
                        "Price: " + MoneyFormat.pretty(pluginConfig.currencySymbol(), listing.price()),
                        "Seller: " + Bukkit.getOfflinePlayer(listing.sellerUuid()).getName(),
                        "Click to buy"));
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
            slotToId.put(i, listing.id());
        }
        current.put(player.getUniqueId(), new ListingContext(page, search, slotToId));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().startsWith(pluginConfig.shopTitle())) {
            return;
        }
        event.setCancelled(true);
        ListingContext context = current.get(player.getUniqueId());
        if (context == null) {
            return;
        }
        Long listingId = context.slotToListingId().get(event.getRawSlot());
        if (listingId == null) {
            return;
        }
        try {
            var listingOpt = marketplaceService.findById(listingId);
            if (listingOpt.isEmpty()) {
                player.sendMessage("Listing no longer available.");
                return;
            }
            boolean bought = marketplaceService.buyListing(player, listingOpt.get());
            if (!bought) {
                player.sendMessage("Could not buy listing.");
                return;
            }
            player.sendMessage("Purchase completed.");
            openPage(player, context.page(), context.search());
        } catch (SQLException ex) {
            player.sendMessage("Purchase failed.");
        }
    }

    private record ListingContext(int page, String search, Map<Integer, Long> slotToListingId) {
    }
}
