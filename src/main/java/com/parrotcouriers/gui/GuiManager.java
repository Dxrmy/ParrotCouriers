package com.parrotcouriers.gui;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.model.CourierData;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles secure GUI interactions for Trade and Claim interfaces, preventing all dupes and exploits.
 */
public class GuiManager implements Listener {

    private final ParrotCouriersPlugin plugin;

    public GuiManager(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
    }

    public void openTradeGui(Player player, CourierData data, Parrot parrot) {
        TradeGui gui = new TradeGui(plugin, data, parrot, player);
        player.openInventory(gui.getInventory());
    }

    public void openClaimGui(Player player, CourierData data, Parrot parrot) {
        ClaimGui gui = new ClaimGui(plugin, data, parrot, player);
        player.openInventory(gui.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof TradeGui tradeGui) {
            handleTradeClick(event, tradeGui);
        } else if (inv.getHolder() instanceof ClaimGui claimGui) {
            handleClaimClick(event, claimGui);
        }
    }

    private void handleTradeClick(InventoryClickEvent event, TradeGui gui) {
        int rawSlot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();
        int topSize = topInv.getSize();

        // Prevent number key hotbar swapping into locked slots
        if (event.getClick() == ClickType.NUMBER_KEY && rawSlot < topSize && rawSlot != TradeGui.PAYMENT_INPUT_SLOT) {
            event.setCancelled(true);
            return;
        }

        // Prevent shift clicking into locked slots
        if (event.isShiftClick() && rawSlot >= topSize) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            ItemStack currentPayment = topInv.getItem(TradeGui.PAYMENT_INPUT_SLOT);
            if (currentPayment == null || currentPayment.getType().isAir()) {
                event.setCancelled(true);
                topInv.setItem(TradeGui.PAYMENT_INPUT_SLOT, clickedItem.clone());
                event.setCurrentItem(null);
            } else if (currentPayment.isSimilar(clickedItem)) {
                event.setCancelled(true);
                int maxStack = currentPayment.getMaxStackSize();
                int space = maxStack - currentPayment.getAmount();
                if (space > 0) {
                    int toAdd = Math.min(space, clickedItem.getAmount());
                    currentPayment.setAmount(currentPayment.getAmount() + toAdd);
                    clickedItem.setAmount(clickedItem.getAmount() - toAdd);
                    if (clickedItem.getAmount() <= 0) {
                        event.setCurrentItem(null);
                    }
                }
            } else {
                event.setCancelled(true);
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack placed = topInv.getItem(TradeGui.PAYMENT_INPUT_SLOT);
                gui.updateStatusAndButton(placed);
            });
            return;
        }

        if (rawSlot >= 0 && rawSlot < topSize) {
            if (rawSlot == TradeGui.CONFIRM_SLOT) {
                event.setCancelled(true);
                gui.handleConfirmClick();
                return;
            }

            if (rawSlot == TradeGui.LETTER_SLOT) {
                event.setCancelled(true);
                gui.handleLetterClick();
                return;
            }

            if (rawSlot != TradeGui.PAYMENT_INPUT_SLOT) {
                // Clicked on a border/payload/info slot
                event.setCancelled(true);
                return;
            }
        }

        // Allow interacting with player inventory or payment input slot
        // Update button & status after a 1-tick delay
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack placed = topInv.getItem(TradeGui.PAYMENT_INPUT_SLOT);
            gui.updateStatusAndButton(placed);
        });
    }

    private void handleClaimClick(InventoryClickEvent event, ClaimGui gui) {
        int rawSlot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();

        if (rawSlot >= 0 && rawSlot < topInv.getSize()) {
            event.setCancelled(true);
            if (rawSlot == ClaimGui.CONFIRM_SLOT || rawSlot == ClaimGui.PAYMENT_SLOT) {
                gui.handleClaim();
            }
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TradeGui gui) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < event.getView().getTopInventory().getSize() && rawSlot != TradeGui.PAYMENT_INPUT_SLOT) {
                    event.setCancelled(true);
                    return;
                }
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                ItemStack placed = event.getView().getTopInventory().getItem(TradeGui.PAYMENT_INPUT_SLOT);
                gui.updateStatusAndButton(placed);
            });
        } else if (event.getInventory().getHolder() instanceof ClaimGui) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TradeGui gui) {
            gui.handleClose();
        }
    }
}
