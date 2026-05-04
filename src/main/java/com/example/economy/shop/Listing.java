package com.example.economy.shop;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public record Listing(
        long id,
        UUID sellerUuid,
        ItemStack itemStack,
        String itemName,
        double price,
        long createdAt,
        long expiresAt) {
}
