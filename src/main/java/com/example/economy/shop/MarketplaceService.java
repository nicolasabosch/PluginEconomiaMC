package com.example.economy.shop;

import com.example.economy.economy.EconomyService;
import com.example.economy.economy.TransactionType;
import com.example.economy.storage.DatabaseManager;
import com.example.economy.util.ItemSerialization;
import com.example.economy.util.PluginConfig;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MarketplaceService {
    private final DatabaseManager databaseManager;
    private final EconomyService economyService;
    private final PluginConfig pluginConfig;

    public MarketplaceService(DatabaseManager databaseManager, EconomyService economyService, PluginConfig pluginConfig) {
        this.databaseManager = databaseManager;
        this.economyService = economyService;
        this.pluginConfig = pluginConfig;
    }

    public synchronized long createListing(UUID sellerUuid, ItemStack stack, double price) throws SQLException, IOException {
        long now = Instant.now().toEpochMilli();
        long expiresAt = now + (pluginConfig.listingExpiryDays() * 24L * 60L * 60L * 1000L);
        String itemName = stack.getType().name();
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            itemName = stack.getItemMeta().getDisplayName();
        }
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "INSERT INTO listings(seller_uuid, item_base64, item_name, price, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sellerUuid.toString());
            ps.setString(2, ItemSerialization.serialize(stack));
            ps.setString(3, itemName);
            ps.setDouble(4, price);
            ps.setLong(5, now);
            ps.setLong(6, expiresAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1L;
    }

    public synchronized List<Listing> listPage(int page, String search) throws SQLException {
        int safePage = Math.max(1, page);
        int limit = pluginConfig.shopPageSize();
        int offset = (safePage - 1) * limit;
        String query = """
                SELECT id, seller_uuid, item_base64, item_name, price, created_at, expires_at
                FROM listings
                WHERE expires_at > ? AND LOWER(item_name) LIKE ?
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """;
        List<Listing> rows = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(query)) {
            ps.setLong(1, Instant.now().toEpochMilli());
            ps.setString(2, "%" + search.toLowerCase(Locale.ROOT) + "%");
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(readListing(rs));
                }
            }
        }
        return rows;
    }

    public synchronized Optional<Listing> findById(long id) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT id, seller_uuid, item_base64, item_name, price, created_at, expires_at FROM listings WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readListing(rs));
                }
            }
        }
        return Optional.empty();
    }

    public synchronized boolean deleteListing(long id) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("DELETE FROM listings WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public synchronized boolean buyListing(Player buyer, Listing listing) throws SQLException {
        if (listing.sellerUuid().equals(buyer.getUniqueId())) {
            return false;
        }
        if (buyer.getInventory().firstEmpty() == -1) {
            return false;
        }
        double taxPercent = pluginConfig.shopTaxPercent() / 100.0D;
        double taxAmount = listing.price() * taxPercent;
        double sellerNet = listing.price() - taxAmount;
        if (!economyService.withdraw(buyer.getUniqueId(), listing.price(), buyer.getUniqueId(), TransactionType.SHOP_PURCHASE,
                "{\"listing\":" + listing.id() + "}")) {
            return false;
        }
        economyService.deposit(listing.sellerUuid(), sellerNet, buyer.getUniqueId(), TransactionType.SHOP_SALE,
                "{\"listing\":" + listing.id() + "}");
        buyer.getInventory().addItem(listing.itemStack());
        deleteListing(listing.id());
        OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerUuid());
        String message = "Your listing sold for " + sellerNet + " after tax.";
        if (seller.isOnline() && seller.getPlayer() != null) {
            seller.getPlayer().sendMessage(message);
        } else {
            queueNotification(listing.sellerUuid(), message);
        }
        return true;
    }

    public synchronized void queueNotification(UUID playerUuid, String message) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "INSERT INTO pending_notifications(player_uuid, message, created_at) VALUES (?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, message);
            ps.setLong(3, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
    }

    public synchronized List<String> pullNotifications(UUID playerUuid) throws SQLException {
        List<String> rows = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT id, message FROM pending_notifications WHERE player_uuid = ? ORDER BY id ASC")) {
            ps.setString(1, playerUuid.toString());
            List<Long> ids = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                    rows.add(rs.getString("message"));
                }
            }
            for (Long id : ids) {
                try (PreparedStatement del = databaseManager.connection().prepareStatement(
                        "DELETE FROM pending_notifications WHERE id = ?")) {
                    del.setLong(1, id);
                    del.executeUpdate();
                }
            }
        }
        return rows;
    }

    public synchronized void processExpiredListings() throws SQLException {
        List<Listing> expired = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement("""
                SELECT id, seller_uuid, item_base64, item_name, price, created_at, expires_at
                FROM listings
                WHERE expires_at <= ?
                """)) {
            ps.setLong(1, Instant.now().toEpochMilli());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expired.add(readListing(rs));
                }
            }
        }
        for (Listing listing : expired) {
            OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.sellerUuid());
            if (seller.isOnline() && seller.getPlayer() != null && seller.getPlayer().getInventory().firstEmpty() != -1) {
                seller.getPlayer().getInventory().addItem(listing.itemStack());
                seller.getPlayer().sendMessage("Expired listing returned: " + listing.itemName());
            } else {
                queueReturn(listing.sellerUuid(), listing.itemStack(), "EXPIRED_LISTING");
                queueNotification(listing.sellerUuid(), "A listing expired and your item will be returned on join.");
            }
            deleteListing(listing.id());
        }
    }

    public synchronized int deliverPendingReturns(Player player) throws SQLException {
        int delivered = 0;
        List<Long> deliveredIds = new ArrayList<>();
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "SELECT id, item_base64 FROM pending_returns WHERE player_uuid = ? ORDER BY id ASC")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (player.getInventory().firstEmpty() == -1) {
                        break;
                    }
                    long id = rs.getLong("id");
                    String itemBase64 = rs.getString("item_base64");
                    try {
                        player.getInventory().addItem(ItemSerialization.deserialize(itemBase64));
                        deliveredIds.add(id);
                        delivered++;
                    } catch (IOException | ClassNotFoundException ex) {
                        deliveredIds.add(id);
                    }
                }
            }
        }
        for (Long id : deliveredIds) {
            try (PreparedStatement del = databaseManager.connection()
                    .prepareStatement("DELETE FROM pending_returns WHERE id = ?")) {
                del.setLong(1, id);
                del.executeUpdate();
            }
        }
        return delivered;
    }

    private void queueReturn(UUID playerUuid, ItemStack itemStack, String reason) throws SQLException {
        try (PreparedStatement ps = databaseManager.connection().prepareStatement(
                "INSERT INTO pending_returns(player_uuid, item_base64, reason, created_at) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, playerUuid.toString());
            try {
                ps.setString(2, ItemSerialization.serialize(itemStack));
            } catch (IOException ex) {
                throw new SQLException("Failed to serialize return item", ex);
            }
            ps.setString(3, reason);
            ps.setLong(4, Instant.now().toEpochMilli());
            ps.executeUpdate();
        }
    }

    private Listing readListing(ResultSet rs) throws SQLException {
        try {
            return new Listing(
                    rs.getLong("id"),
                    UUID.fromString(rs.getString("seller_uuid")),
                    ItemSerialization.deserialize(rs.getString("item_base64")),
                    rs.getString("item_name"),
                    rs.getDouble("price"),
                    rs.getLong("created_at"),
                    rs.getLong("expires_at"));
        } catch (IOException | ClassNotFoundException ex) {
            throw new SQLException("Failed to decode listing item", ex);
        }
    }
}
