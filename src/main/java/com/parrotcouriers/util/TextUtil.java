package com.parrotcouriers.util;

import com.parrotcouriers.ParrotCouriersPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Utility for formatting messages using Adventure Component / MiniMessage.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {}

    /**
     * Parse MiniMessage format into Component.
     */
    public static Component parse(String message) {
        if (message == null) return Component.empty();
        return MINI_MESSAGE.deserialize(message);
    }

    /**
     * Send message to player, respecting prefix configuration.
     */
    public static void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        ParrotCouriersPlugin plugin = ParrotCouriersPlugin.getPlugin(ParrotCouriersPlugin.class);
        boolean showPrefix = plugin.getConfigManager().isShowPrefix();
        String prefix = showPrefix ? plugin.getConfigManager().getChatPrefix() : "";
        player.sendMessage(MINI_MESSAGE.deserialize(prefix + message));
    }

    /**
     * Send action bar message to player.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        player.sendActionBar(MINI_MESSAGE.deserialize(message));
    }

    /**
     * Convert Component to plain text string.
     */
    public static String toPlain(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
