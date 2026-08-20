package com.parrotcouriers.gui;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.config.ConfigManager;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 27-slot clean GUI for the owner to retrieve returned trade payments, tips, or recalled packages.
 */
public class ClaimGui implements InventoryHolder {

    public static final int PAYMENT_SLOT = 13;
    public static final int CONFIRM_SLOT = 22;

    private final ParrotCouriersPlugin plugin;
    private final CourierData courierData;
    private final Parrot parrot;
    private final Player owner;
    private final Inventory inventory;

    public ClaimGui(ParrotCouriersPlugin plugin, CourierData courierData, Parrot parrot, Player owner) {
        this.plugin = plugin;
        this.courierData = courierData;
        this.parrot = parrot;
        this.owner = owner;
        this.inventory = Bukkit.createInventory(this, 27, TextUtil.parse("<dark_gray>Courier Return • <green>Collect Items</green></dark_gray>"));
        initializeInventory();
    }

    private void initializeInventory() {
        ItemStack borderFiller = createItem(Material.BLACK_STAINED_GLASS_PANE, "<gray> </gray>");
        for (int i = 0; i < 27; i++) {
            if (i != PAYMENT_SLOT && i != CONFIRM_SLOT) {
                inventory.setItem(i, borderFiller);
            }
        }

        ItemStack payment = courierData.getPaymentReceived();
        ItemStack payload = courierData.getPayloadItem();

        if (payment != null && !payment.getType().isAir()) {
            ItemStack display = payment.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<green>Payment or tip received from delivery!</green>"));
                lore.add(TextUtil.parse("<yellow>Click 'Collect & Dismiss' to take items.</yellow>"));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(PAYMENT_SLOT, display);
        } else if (payload != null && !payload.getType().isAir()) {
            ItemStack display = payload.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<yellow>Recalled package returned safely.</yellow>"));
                lore.add(TextUtil.parse("<yellow>Click 'Collect & Dismiss' to recover items.</yellow>"));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(PAYMENT_SLOT, display);
        } else {
            ItemStack info = createItem(Material.HEART_OF_THE_SEA, "<green><b>Gift Delivered</b></green>",
                    "<gray>Your package was safely delivered!</gray>",
                    "<yellow>Click below to dismiss courier.</yellow>");
            inventory.setItem(PAYMENT_SLOT, info);
        }

        ItemStack finishButton = createItem(Material.LIME_STAINED_GLASS_PANE, "<green><b>[ Collect & Dismiss Courier ]</b></green>",
                "<gray>Collects items into your inventory and</gray>",
                "<gray>restores your parrot as a normal pet.</gray>");
        inventory.setItem(CONFIRM_SLOT, finishButton);
    }

    public void handleClaim() {
        ConfigManager cfg = plugin.getConfigManager();
        ItemStack payment = courierData.getPaymentReceived();
        ItemStack payload = courierData.getPayloadItem();

        if (payment != null && !payment.getType().isAir()) {
            giveItemToPlayer(owner, payment);
            courierData.setPaymentReceived(null);
        }
        if (payload != null && !payload.getType().isAir()) {
            giveItemToPlayer(owner, payload);
            courierData.setPayloadItem(null);
        }

        // Revert courier to regular pet
        plugin.getCourierManager().finalizeAndRevert(parrot, courierData);

        owner.closeInventory();
        if (cfg.isSoundsEnabled()) {
            owner.playSound(owner.getLocation(), cfg.getLockSound(), 1.0f, 1.0f);
            owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        }
        parrot.getWorld().spawnParticle(cfg.getLockParticle(), parrot.getLocation().add(0, 0.5, 0), cfg.getLockParticleCount(), 0.25, 0.25, 0.25, 0.04);

        TextUtil.sendMessage(owner, cfg.getMessage("payment-claimed", "<green>✔ Items claimed! Your parrot is resting safely.</green>"));
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        var leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            TextUtil.sendMessage(player, "<yellow>Your inventory was full, some items were dropped at your feet.</yellow>");
        }
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            if (lore.length > 0) {
                List<Component> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(TextUtil.parse(line));
                }
                meta.lore(loreList);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public CourierData getCourierData() {
        return courierData;
    }

    public Parrot getParrot() {
        return parrot;
    }
}
