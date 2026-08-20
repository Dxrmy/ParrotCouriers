package com.parrotcouriers.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * Utility for safe serialization and deserialization of ItemStacks.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    /**
     * Serializes an ItemStack to a Base64 string.
     */
    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "";
        }
        try {
            // Prefer Paper native byte serialization if available, wrapped in Base64
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Throwable t) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                    dataOutput.writeObject(item);
                }
                return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    /**
     * Deserializes a Base64 string back into an ItemStack.
     */
    public static ItemStack deserialize(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Data);
            try {
                return ItemStack.deserializeBytes(bytes);
            } catch (Throwable t) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                    return (ItemStack) dataInput.readObject();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
