package com.parrotcouriers.gui;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.config.ConfigManager;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.model.CourierState;
import com.parrotcouriers.model.DeliveryRecord;
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
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive 27-slot GUI for the recipient to inspect payload, read attached letters,
 * deposit payment, or provide optional tips.
 */
public class TradeGui implements InventoryHolder {

    public static final int LETTER_SLOT = 4;
    public static final int PAYLOAD_SLOT = 10;
    public static final int INFO_SLOT = 13;
    public static final int PAYMENT_INPUT_SLOT = 16;
    public static final int CONFIRM_SLOT = 22;

    private final ParrotCouriersPlugin plugin;
    private final CourierData courierData;
    private final Parrot parrot;
    private final Player recipient;
    private final Inventory inventory;

    public TradeGui(ParrotCouriersPlugin plugin, CourierData courierData, Parrot parrot, Player recipient) {
        this.plugin = plugin;
        this.courierData = courierData;
        this.parrot = parrot;
        this.recipient = recipient;
        this.courierData.setGuiOpen(true);
        this.inventory = Bukkit.createInventory(this, 27, TextUtil.parse("<dark_gray>Delivery from <gold>" + courierData.getOwnerName() + "</gold></dark_gray>"));
        initializeInventory();
    }

    private void initializeInventory() {
        // High-contrast dark border for chest background
        ItemStack borderFiller = createItem(Material.BLACK_STAINED_GLASS_PANE, "<gray> </gray>");
        for (int i = 0; i < 27; i++) {
            if (i != LETTER_SLOT && i != PAYLOAD_SLOT && i != INFO_SLOT && i != PAYMENT_INPUT_SLOT && i != CONFIRM_SLOT) {
                inventory.setItem(i, borderFiller);
            }
        }

        // Slot 4: Attached Letter / Note (if present)
        if (courierData.getLetterItem() != null) {
            ItemStack letterDisplay = courierData.getLetterItem().clone();
            ItemMeta meta = letterDisplay.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<yellow>Attached note from <gold>" + courierData.getOwnerName() + "</gold></yellow>"));
                lore.add(TextUtil.parse("<green>Click to read letter!</green>"));
                meta.lore(lore);
                letterDisplay.setItemMeta(meta);
            }
            inventory.setItem(LETTER_SLOT, letterDisplay);
        } else {
            inventory.setItem(LETTER_SLOT, borderFiller);
        }

        // Slot 10: Payload preview
        if (courierData.getPayloadItem() != null) {
            ItemStack displayPayload = courierData.getPayloadItem().clone();
            ItemMeta meta = displayPayload.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>From: <gold>" + courierData.getOwnerName() + "</gold></gray>"));
                lore.add(TextUtil.parse("<green>Complete trade to collect this item.</green>"));
                meta.lore(lore);
                displayPayload.setItemMeta(meta);
            }
            inventory.setItem(PAYLOAD_SLOT, displayPayload);
        }

        updateStatusAndButton(null);
    }

    public void updateStatusAndButton(ItemStack placedItem) {
        boolean isSatisfied = courierData.isPaymentSatisfied(placedItem);
        ItemStack required = courierData.getPaymentRequired();
        boolean isGift = required == null || required.getType().isAir() || required.getAmount() <= 0;

        // Slot 13: Info item
        ItemStack info;
        if (isGift) {
            if (placedItem != null && !placedItem.getType().isAir()) {
                String tipName = formatItemName(placedItem);
                info = createItem(Material.HEART_OF_THE_SEA, "<green><b>Free Gift + Tip</b></green>",
                        "<gray>Package is a free gift!</gray>",
                        "<gold>Tip added: " + tipName + " x" + placedItem.getAmount() + "</gold>",
                        "<yellow>Click Accept below to receive your gift.</yellow>");
            } else {
                info = createItem(Material.HEART_OF_THE_SEA, "<green><b>Free Gift Delivery</b></green>",
                        "<gray>No payment required by sender!</gray>",
                        "<gray>You can place an optional tip on the right.</gray>",
                        "<yellow>Click Accept below to receive your gift.</yellow>");
            }
        } else {
            String reqName = formatItemName(required);
            String status = isSatisfied ? "<green>✔ Ready</green>" : "<red>Waiting for payment...</red>";
            info = createItem(Material.GOLD_INGOT, "<gold><b>Required Payment</b></gold>",
                    "<gray>Item: <gold>" + reqName + " x" + required.getAmount() + "</gold></gray>",
                    "<gray>Status: " + status + "</gray>",
                    "<yellow>Place the exact items in the slot on the right.</yellow>");
        }
        inventory.setItem(INFO_SLOT, info);

        // Slot 22: Action Button
        ItemStack confirmButton;
        if (isSatisfied) {
            confirmButton = createItem(Material.LIME_STAINED_GLASS_PANE, "<green><b>[ Accept Delivery ]</b></green>",
                    "<gray>Click to receive package and complete trade.</gray>");
        } else {
            confirmButton = createItem(Material.RED_STAINED_GLASS_PANE, "<red><b>[ Payment Required ]</b></red>",
                    "<gray>Place required items into slot on right.</gray>");
        }
        inventory.setItem(CONFIRM_SLOT, confirmButton);
    }

    public void handleLetterClick() {
        if (courierData.getLetterItem() == null) return;
        ItemStack letter = courierData.getLetterItem();
        
        ItemStack viewableBook = null;
        if (letter.getType() == Material.WRITTEN_BOOK) {
            viewableBook = letter.clone();
        } else if (letter.getType() == Material.WRITABLE_BOOK && letter.getItemMeta() instanceof org.bukkit.inventory.meta.WritableBookMeta writable) {
            viewableBook = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) viewableBook.getItemMeta();
            if (meta != null) {
                if (writable.hasPages()) {
                    meta.setPages(writable.getPages());
                }
                meta.title(Component.text(courierData.getDeliveryNote() != null ? courierData.getDeliveryNote() : "Courier Letter"));
                meta.author(Component.text(courierData.getOwnerName() != null ? courierData.getOwnerName() : "Courier"));
                viewableBook.setItemMeta(meta);
            }
        }

        if (viewableBook != null) {
            final ItemStack bookToOpen = viewableBook;
            courierData.setGuiOpen(false);
            recipient.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                recipient.openBook(bookToOpen);
                recipient.playSound(recipient.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            });
        }
    }

    public void handleConfirmClick() {
        ItemStack placed = inventory.getItem(PAYMENT_INPUT_SLOT);
        if (!courierData.isPaymentSatisfied(placed)) {
            recipient.playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
            TextUtil.sendMessage(recipient, "<red>Please place the required payment before accepting the delivery!</red>");
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();

        // 1. Deliver Payload to recipient
        ItemStack payload = courierData.getPayloadItem();
        if (payload != null) {
            giveItemToPlayer(recipient, payload);
            // CRITICAL FIX: Clear payload from courier immediately to eliminate item dupe glitch!
            courierData.setPayloadItem(null);
        }

        // 2. Deliver attached letter if present
        if (courierData.getLetterItem() != null) {
            giveItemToPlayer(recipient, courierData.getLetterItem());
            courierData.setLetterItem(null);
        }

        // 3. Process payment or optional tip
        boolean isGift = courierData.getPaymentRequired() == null || courierData.getPaymentRequired().getType().isAir();
        ItemStack paymentToStore = null;

        if (placed != null && !placed.getType().isAir()) {
            if (!isGift) {
                // Paid delivery: consume exact required payment amount
                paymentToStore = placed.clone();
                int reqAmount = courierData.getPaymentRequired().getAmount();
                paymentToStore.setAmount(reqAmount);
                placed.setAmount(placed.getAmount() - reqAmount);
                if (placed.getAmount() > 0) {
                    giveItemToPlayer(recipient, placed);
                }
            } else {
                // Free gift with optional tip: consume full tip
                paymentToStore = placed.clone();
            }
            inventory.setItem(PAYMENT_INPUT_SLOT, null);
        }
        courierData.setPaymentReceived(paymentToStore);

        // 4. Add transaction record to history ledger
        String payloadStr = (payload != null) ? formatItemName(payload) + " x" + payload.getAmount() : "Unknown";
        String payStr = (paymentToStore != null)
                ? formatItemName(paymentToStore) + " x" + paymentToStore.getAmount() + (isGift ? " (Tip)" : "")
                : (isGift ? "Free Gift" : "None");
        plugin.getCourierManager().addHistoryEntry(new DeliveryRecord(System.currentTimeMillis(), courierData.getOwnerName(), recipient.getName(), payloadStr, payStr, isGift));

        // 5. Transition courier state to return flight
        courierData.setState(CourierState.IN_TRANSIT_TO_OWNER);
        courierData.setGuiOpen(false);
        courierData.setTransitStartTime(System.currentTimeMillis());
        courierData.saveToPdc(parrot.getPersistentDataContainer(), plugin);
        plugin.getCourierManager().saveAll();

        // 6. Update Parrot nameplate
        if (cfg.isShowNametags()) {
            parrot.customName(TextUtil.parse("<green>[Returning Courier]</green> <yellow>" + courierData.getOwnerName() + "</yellow>"));
            parrot.setCustomNameVisible(true);
        }

        recipient.closeInventory();
        if (cfg.isSoundsEnabled()) {
            recipient.playSound(recipient.getLocation(), cfg.getCompleteSound(), 1.0f, 1.2f);
        }
        parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.5, 0), 15, 0.25, 0.25, 0.25, 0.04);

        if (isGift && paymentToStore != null) {
            TextUtil.sendMessage(recipient, "<green>✔ Free delivery accepted! (Tip of <gold>" + formatItemName(paymentToStore) + " x" + paymentToStore.getAmount() + "</gold> sent to <yellow>" + courierData.getOwnerName() + "</yellow>).</green>");
        } else {
            TextUtil.sendMessage(recipient, cfg.getMessage("trade-completed-recipient", "<green>✔ Trade accepted! Package received.</green>"));
        }

        // 7. Notify owner if online
        Player owner = (courierData.getOwnerUuid() != null) ? Bukkit.getPlayer(courierData.getOwnerUuid()) : null;
        if (owner != null && owner.isOnline()) {
            if (isGift && paymentToStore != null) {
                TextUtil.sendMessage(owner, "<green>✔ Your courier delivered the package to <yellow>" + recipient.getName() + "</yellow> and received a tip of <gold>" + formatItemName(paymentToStore) + " x" + paymentToStore.getAmount() + "</gold>!</green>");
            } else {
                String msgOwner = cfg.getMessage("trade-completed-owner", "<green>✔ Your courier delivered the package to <yellow>%recipient%</yellow> and is returning!</green>")
                        .replace("%recipient%", recipient.getName());
                TextUtil.sendMessage(owner, msgOwner);
            }
        }
    }

    public void handleClose() {
        courierData.setGuiOpen(false);
        ItemStack placed = inventory.getItem(PAYMENT_INPUT_SLOT);
        if (placed != null && !placed.getType().isAir()) {
            inventory.setItem(PAYMENT_INPUT_SLOT, null);
            giveItemToPlayer(recipient, placed);
        }
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

    private static String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return TextUtil.toPlain(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
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
