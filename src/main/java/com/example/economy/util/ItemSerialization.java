package com.example.economy.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class ItemSerialization {
    private ItemSerialization() {
    }

    public static String serialize(ItemStack itemStack) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(output)) {
            bukkitOut.writeObject(itemStack);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        }
    }

    public static ItemStack deserialize(String base64) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream input = new ByteArrayInputStream(data);
                BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(input)) {
            return (ItemStack) bukkitIn.readObject();
        }
    }
}
