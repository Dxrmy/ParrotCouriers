package com.parrotcouriers.listener;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.config.ConfigManager;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.model.CourierState;
import com.parrotcouriers.model.TargetType;
import com.parrotcouriers.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.Objects;

/**
 * Handles player interactions with parrots: registration, payload, payment, letters, berry buffs,
 * sneaking delivery acceptance, invulnerability protection, and taming protection.
 */
public class CourierListener implements Listener {

    private final ParrotCouriersPlugin plugin;

    public CourierListener(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Parrot parrot)) return;

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();
        CourierData courier = plugin.getCourierManager().getCourier(parrot);
        ConfigManager cfg = plugin.getConfigManager();

        // Active Flight & In-Transit Protection: Block all interactions & renaming during flight
        if (courier != null) {
            CourierState state = courier.getState();
            if (state == CourierState.IN_TRANSIT_TO_DESTINATION || state == CourierState.IN_TRANSIT_TO_OWNER) {
                event.setCancelled(true);
                TextUtil.sendMessage(player, "<red>This courier is in active flight! Use <yellow>/courier recall</yellow> to cancel the flight first.</red>");
                return;
            }
            if ((state == CourierState.WAITING_FOR_RECIPIENT || state == CourierState.WAITING_FOR_OWNER) && handItem.getType() == Material.NAME_TAG) {
                event.setCancelled(true);
                TextUtil.sendMessage(player, "<red>Cannot rename a courier that is currently waiting to complete a delivery!</red>");
                return;
            }
        }

        // Case 1: Renaming a tamed parrot with Name Tag to initiate Courier registration
        if (handItem.getType() == Material.NAME_TAG && handItem.hasItemMeta()) {
            ItemMeta meta = handItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String targetRaw = TextUtil.toPlain(meta.displayName()).trim();
                String prefix = cfg.getNamePrefix();

                boolean hasPrefix = !prefix.isEmpty() && targetRaw.startsWith(prefix);
                if (cfg.isRequirePrefix() && !hasPrefix) {
                    // Normal cosmetic naming, do not register as courier
                    return;
                }

                if (hasPrefix) {
                    targetRaw = targetRaw.substring(prefix.length()).trim();
                }

                if (!targetRaw.isEmpty() && isTamedBy(parrot, player)) {
                    event.setCancelled(true);
                    CourierData newCourier = plugin.getCourierManager().registerNewCourier(parrot, player, targetRaw);

                    // Force parrot to sit, stop pathfinding, and freeze in place
                    parrot.getPathfinder().stopPathfinding();
                    parrot.setVelocity(new Vector(0, 0, 0));
                    parrot.setSitting(true);

                    if (cfg.isSoundsEnabled()) {
                        parrot.getWorld().playSound(parrot.getLocation(), cfg.getAmbientSound(), 1.0f, 1.2f);
                    }
                    parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.4, 0), 8, 0.2, 0.2, 0.2, 0.05);

                    String targetTypeDesc = (newCourier.getTargetType() == TargetType.COORDINATES) ? "Coordinates" : "Player";
                    String targetName = (newCourier.getTargetType() == TargetType.COORDINATES)
                            ? String.format("%.0f, %.0f, %.0f (%s)", newCourier.getTargetX(), newCourier.getTargetY(), newCourier.getTargetZ(), newCourier.getTargetWorldName())
                            : newCourier.getTargetPlayerName();

                    TextUtil.sendMessage(player, cfg.getMessage("registered-title", "<green><b>Parrot Courier Registered!</b></green>"));
                    TextUtil.sendMessage(player, cfg.getMessage("registered-destination", "<gray>Destination (<yellow>%type%</yellow>): <gold>%target%</gold></gray>")
                            .replace("%type%", targetTypeDesc)
                            .replace("%target%", targetName));
                    TextUtil.sendMessage(player, cfg.getMessage("step1-prompt", "<yellow><b>Step 1:</b> Right-click with your package to send (or sneak-click with berries/fruit/book for buffs).</yellow>"));
                    return;
                }
            }
        }

        // If not a courier, return
        if (courier == null) return;

        // Ensure only owner can configure courier in setup phase
        if (courier.getState() == CourierState.AWAITING_PAYLOAD || courier.getState() == CourierState.AWAITING_PAYMENT) {
            if (!Objects.equals(courier.getOwnerUuid(), player.getUniqueId())) {
                TextUtil.sendMessage(player, "<red>This parrot is currently being configured by its owner.</red>");
                event.setCancelled(true);
                return;
            }
            if (cfg.isFreezeDuringSetup()) {
                parrot.setSitting(true);
                parrot.setVelocity(new Vector(0, 0, 0));
            }
        }

        // SNEAK INTERACTIONS: Feeding Buffs & Attaching Letters
        if (player.isSneaking() && (courier.getState() == CourierState.AWAITING_PAYLOAD || courier.getState() == CourierState.AWAITING_PAYMENT)) {
            // Sweet Berry speed buff
            if (handItem.getType() == Material.SWEET_BERRIES) {
                event.setCancelled(true);
                courier.setSpeedBoost(true);
                handItem.subtract(1);
                parrot.getWorld().playSound(parrot.getLocation(), Sound.ENTITY_PARROT_EAT, 1.0f, 1.4f);
                parrot.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, parrot.getLocation().add(0, 0.4, 0), 10, 0.2, 0.2, 0.2, 0.05);
                TextUtil.sendMessage(player, "<green>✔ Sweet Berry fed! Courier speed boosted by +85%.</green>");
                return;
            }

            // Glow Berry spectral buff
            if (handItem.getType() == Material.GLOW_BERRIES) {
                event.setCancelled(true);
                courier.setGlowing(true);
                parrot.setGlowing(true);
                handItem.subtract(1);
                parrot.getWorld().playSound(parrot.getLocation(), Sound.ENTITY_PARROT_EAT, 1.0f, 1.4f);
                parrot.getWorld().spawnParticle(Particle.WAX_OFF, parrot.getLocation().add(0, 0.4, 0), 12, 0.2, 0.2, 0.2, 0.05);
                TextUtil.sendMessage(player, "<green>✔ Glow Berry fed! Courier is now glowing for night delivery.</green>");
                return;
            }

            // Chorus Fruit dimensional travel buff
            if (handItem.getType() == Material.CHORUS_FRUIT) {
                event.setCancelled(true);
                courier.setDimensionalTravel(true);
                handItem.subtract(1);
                parrot.getWorld().playSound(parrot.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.2f);
                parrot.getWorld().spawnParticle(Particle.PORTAL, parrot.getLocation().add(0, 0.4, 0), 18, 0.3, 0.3, 0.3, 0.05);
                TextUtil.sendMessage(player, "<green>✔ Chorus Fruit fed! Courier is infused with cross-dimensional travel abilities.</green>");
                return;
            }

            // Attaching a Letter (Written Book / Book and Quill / Paper with custom name)
            if (courier.getLetterItem() == null && (handItem.getType() == Material.WRITTEN_BOOK || handItem.getType() == Material.WRITABLE_BOOK || (handItem.getType() == Material.PAPER && handItem.hasItemMeta()))) {
                event.setCancelled(true);
                courier.setLetterItem(handItem.clone());
                if (handItem.getItemMeta() instanceof BookMeta bookMeta && bookMeta.hasTitle()) {
                    courier.setDeliveryNote(bookMeta.getTitle());
                } else if (handItem.hasItemMeta() && handItem.getItemMeta().hasDisplayName()) {
                    courier.setDeliveryNote(TextUtil.toPlain(handItem.getItemMeta().displayName()));
                }
                player.getInventory().setItemInMainHand(null);
                parrot.getWorld().playSound(parrot.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
                parrot.getWorld().spawnParticle(Particle.ENCHANT, parrot.getLocation().add(0, 0.5, 0), 10, 0.2, 0.2, 0.2, 0.05);
                TextUtil.sendMessage(player, "<green>✔ Letter attached! The recipient can read your letter upon delivery.</green>");
                return;
            }

            // Free Gift Delivery (Empty hand sneak right-click during AWAITING_PAYMENT)
            if (courier.getState() == CourierState.AWAITING_PAYMENT && handItem.getType().isAir()) {
                event.setCancelled(true);
                courier.setPaymentRequired(null);
                courier.setTransitStartTime(System.currentTimeMillis());
                TextUtil.sendMessage(player, cfg.getMessage("gift-delivery-set", "<green>✔ Free gift delivery (no payment required)!</green>"));

                // Play configured particles and lock trade
                parrot.getWorld().spawnParticle(cfg.getLockParticle(), parrot.getLocation().add(0, 0.5, 0), cfg.getLockParticleCount(), 0.3, 0.3, 0.3, 0.05);
                if (cfg.isSoundsEnabled()) {
                    parrot.getWorld().playSound(parrot.getLocation(), cfg.getLockSound(), 1.0f, 1.0f);
                    parrot.getWorld().playSound(parrot.getLocation(), cfg.getChimeSound(), 1.0f, 1.5f);
                }

                plugin.getCourierManager().activateCourierMode(parrot, courier);

                String targetDisplay = (courier.getTargetType() == TargetType.COORDINATES)
                        ? String.format("%.0f, %.0f, %.0f (%s)", courier.getTargetX(), courier.getTargetY(), courier.getTargetZ(), courier.getTargetWorldName())
                        : courier.getTargetPlayerName();

                TextUtil.sendMessage(player, cfg.getMessage("trade-locked", "<green><b>Trade locked!</b> Courier is taking flight to <gold>%target%</gold>...</green>")
                        .replace("%target%", targetDisplay));
                return;
            }
        }

        // NORMAL RIGHT-CLICK: Loading Payload
        if (courier.getState() == CourierState.AWAITING_PAYLOAD) {
            event.setCancelled(true);
            if (handItem.getType().isAir()) {
                TextUtil.sendMessage(player, "<yellow>Please right-click with the item or shulker box you wish to send!</yellow>");
                return;
            }

            // Store payload item (entire stack in hand or shulker)
            ItemStack payload = handItem.clone();
            courier.setPayloadItem(payload);
            courier.setState(CourierState.AWAITING_PAYMENT);
            courier.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            plugin.getCourierManager().saveAll();

            if (cfg.isFreezeDuringSetup()) {
                parrot.setSitting(true);
                parrot.setVelocity(new Vector(0, 0, 0));
            }

            // Consume from hand
            player.getInventory().setItemInMainHand(null);
            if (cfg.isSoundsEnabled()) {
                player.playSound(player.getLocation(), cfg.getLockSound(), 1.0f, 1.2f);
            }
            parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.4, 0), 10, 0.2, 0.2, 0.2, 0.05);

            String itemName = formatItemName(payload);
            TextUtil.sendMessage(player, cfg.getMessage("payload-loaded", "<green>✔ Package loaded: <gold>%item% x%amount%</gold>!</green>")
                    .replace("%item%", itemName)
                    .replace("%amount%", String.valueOf(payload.getAmount())));
            TextUtil.sendMessage(player, cfg.getMessage("step2-prompt", "<yellow><b>Step 2:</b> Right-click with your payment item & quantity (or sneak-click empty hand for free delivery).</yellow>"));
            return;
        }

        // NORMAL RIGHT-CLICK: Setting Payment requirement and locking trade
        if (courier.getState() == CourierState.AWAITING_PAYMENT) {
            event.setCancelled(true);

            if (!handItem.getType().isAir()) {
                // Required payment template (exact item in hand)
                ItemStack req = handItem.clone();
                courier.setPaymentRequired(req);
                String reqName = formatItemName(req);
                TextUtil.sendMessage(player, cfg.getMessage("payment-set", "<green>✔ Required payment: <gold>%item% x%amount%</gold>.</green>")
                        .replace("%item%", reqName)
                        .replace("%amount%", String.valueOf(req.getAmount())));
            } else {
                TextUtil.sendMessage(player, "<yellow>Right-click with your payment item or sneak right-click with empty hand for a free delivery.</yellow>");
                return;
            }

            courier.setTransitStartTime(System.currentTimeMillis());

            // Play configured particles and lock trade
            parrot.getWorld().spawnParticle(cfg.getLockParticle(), parrot.getLocation().add(0, 0.5, 0), cfg.getLockParticleCount(), 0.3, 0.3, 0.3, 0.05);
            if (cfg.isSoundsEnabled()) {
                parrot.getWorld().playSound(parrot.getLocation(), cfg.getLockSound(), 1.0f, 1.0f);
                parrot.getWorld().playSound(parrot.getLocation(), cfg.getChimeSound(), 1.0f, 1.5f);
            }

            // Activate courier mode & launch
            plugin.getCourierManager().activateCourierMode(parrot, courier);

            String targetDisplay = (courier.getTargetType() == TargetType.COORDINATES)
                    ? String.format("%.0f, %.0f, %.0f (%s)", courier.getTargetX(), courier.getTargetY(), courier.getTargetZ(), courier.getTargetWorldName())
                    : courier.getTargetPlayerName();

            TextUtil.sendMessage(player, cfg.getMessage("trade-locked", "<green><b>Trade locked!</b> Courier is taking flight to <gold>%target%</gold>...</green>")
                    .replace("%target%", targetDisplay));
            TextUtil.sendMessage(player, cfg.getMessage("trade-locked-sub", "<gray>Courier is invulnerable and flying to: <yellow>%target%</yellow>.</gray>")
                    .replace("%target%", targetDisplay));
            return;
        }

        // Case 4: Right-clicking waiting courier (open GUI)
        if (courier.getState() == CourierState.WAITING_FOR_RECIPIENT) {
            event.setCancelled(true);
            if (courier.getTargetType() == TargetType.COORDINATES || (courier.getTargetPlayerName() != null && courier.getTargetPlayerName().equalsIgnoreCase(player.getName()))) {
                plugin.getGuiManager().openTradeGui(player, courier, parrot);
            } else {
                TextUtil.sendMessage(player, "<red>This courier is waiting for <yellow>" + courier.getTargetPlayerName() + "</yellow> to trade.</red>");
            }
            return;
        }

        if (courier.getState() == CourierState.WAITING_FOR_OWNER) {
            event.setCancelled(true);
            if (Objects.equals(courier.getOwnerUuid(), player.getUniqueId())) {
                plugin.getGuiManager().openClaimGui(player, courier, parrot);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        double radius = plugin.getConfigManager().getInteractionRadius();

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Parrot parrot)) continue;

            CourierData courier = plugin.getCourierManager().getCourier(parrot);
            if (courier == null) continue;

            // Recipient trade interaction
            if (courier.getState() == CourierState.WAITING_FOR_RECIPIENT) {
                if (courier.getTargetType() == TargetType.COORDINATES || (courier.getTargetPlayerName() != null && courier.getTargetPlayerName().equalsIgnoreCase(player.getName()))) {
                    plugin.getGuiManager().openTradeGui(player, courier, parrot);
                    return;
                }
            }

            // Owner return claim interaction
            if (courier.getState() == CourierState.WAITING_FOR_OWNER) {
                if (Objects.equals(courier.getOwnerUuid(), player.getUniqueId())) {
                    plugin.getGuiManager().openClaimGui(player, courier, parrot);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Parrot parrot) {
            if (plugin.getCourierManager().isCourier(parrot)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (event.getEntity() instanceof Parrot parrot) {
            if (plugin.getCourierManager().isCourier(parrot)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isTamedBy(Parrot parrot, Player player) {
        return parrot.isTamed() && parrot.getOwner() != null && parrot.getOwner().getUniqueId().equals(player.getUniqueId());
    }

    private static String formatItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return TextUtil.toPlain(item.getItemMeta().displayName());
        }
        String name = item.getType().name().replace('_', ' ').toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
