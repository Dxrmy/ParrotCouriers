package com.parrotcouriers.model;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.util.ItemSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Data model representing the state, payload, buffs, and attached letter of an active Courier Parrot.
 */
public class CourierData {

    private final UUID courierUuid;
    private UUID ownerUuid;
    private String ownerName;
    private TargetType targetType;
    private String targetPlayerName;
    private String targetWorldName;
    private double targetX;
    private double targetY;
    private double targetZ;
    private CourierState state;
    private ItemStack payloadItem;
    private ItemStack paymentRequired;
    private ItemStack paymentReceived;
    private ItemStack letterItem;
    private String deliveryNote;
    private boolean speedBoost;
    private boolean glowing;
    private boolean dimensionalTravel;
    private String originalParrotName;
    private String parrotVariant;
    private long createdAt;
    private long lastNotifiedAt;
    private long transitStartTime;
    private int stuckTicks;
    private int retryCount;
    private double lastTrackedX;
    private double lastTrackedY;
    private double lastTrackedZ;
    private long maxTransitTimeoutMs;
    private transient boolean isGuiOpen;

    public CourierData(UUID courierUuid) {
        this.courierUuid = courierUuid;
        this.state = CourierState.AWAITING_PAYLOAD;
        this.createdAt = System.currentTimeMillis();
        this.lastNotifiedAt = 0;
        this.transitStartTime = 0;
        this.stuckTicks = 0;
        this.retryCount = 0;
        this.lastTrackedX = 0;
        this.lastTrackedY = 0;
        this.lastTrackedZ = 0;
        this.maxTransitTimeoutMs = 0;
        this.isGuiOpen = false;
        this.speedBoost = false;
        this.glowing = false;
        this.dimensionalTravel = false;
    }

    public long getTransitStartTime() {
        return transitStartTime;
    }

    public void setTransitStartTime(long transitStartTime) {
        this.transitStartTime = transitStartTime;
    }

    public int getStuckTicks() {
        return stuckTicks;
    }

    public void setStuckTicks(int stuckTicks) {
        this.stuckTicks = stuckTicks;
    }

    public void incrementStuckTicks() {
        this.stuckTicks++;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public double getLastTrackedX() {
        return lastTrackedX;
    }

    public double getLastTrackedY() {
        return lastTrackedY;
    }

    public double getLastTrackedZ() {
        return lastTrackedZ;
    }

    public void setLastTrackedPosition(double x, double y, double z) {
        this.lastTrackedX = x;
        this.lastTrackedY = y;
        this.lastTrackedZ = z;
    }

    public long getMaxTransitTimeoutMs() {
        return maxTransitTimeoutMs;
    }

    public void setMaxTransitTimeoutMs(long maxTransitTimeoutMs) {
        this.maxTransitTimeoutMs = maxTransitTimeoutMs;
    }

    public UUID getCourierUuid() {
        return courierUuid;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName;
    }

    public String getTargetWorldName() {
        return targetWorldName;
    }

    public void setTargetWorldName(String targetWorldName) {
        this.targetWorldName = targetWorldName;
    }

    public double getTargetX() {
        return targetX;
    }

    public void setTargetX(double targetX) {
        this.targetX = targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetY(double targetY) {
        this.targetY = targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public void setTargetZ(double targetZ) {
        this.targetZ = targetZ;
    }

    public void setTargetCoordinates(World world, double x, double y, double z) {
        this.targetWorldName = (world != null) ? world.getName() : "world";
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public Location getTargetLocation(Server server) {
        if (targetWorldName == null) return null;
        World world = server.getWorld(targetWorldName);
        if (world == null) return null;
        return new Location(world, targetX, targetY, targetZ);
    }

    public CourierState getState() {
        return state;
    }

    public void setState(CourierState state) {
        this.state = state;
    }

    public String getParrotVariant() {
        return parrotVariant;
    }

    public void setParrotVariant(String parrotVariant) {
        this.parrotVariant = parrotVariant;
    }

    public ItemStack getPayloadItem() {
        return payloadItem;
    }

    public void setPayloadItem(ItemStack payloadItem) {
        this.payloadItem = payloadItem;
    }

    public ItemStack getPaymentRequired() {
        return paymentRequired;
    }

    public void setPaymentRequired(ItemStack paymentRequired) {
        this.paymentRequired = paymentRequired;
    }

    public ItemStack getPaymentReceived() {
        return paymentReceived;
    }

    public void setPaymentReceived(ItemStack paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public ItemStack getLetterItem() {
        return letterItem;
    }

    public void setLetterItem(ItemStack letterItem) {
        this.letterItem = letterItem;
    }

    public String getDeliveryNote() {
        return deliveryNote;
    }

    public void setDeliveryNote(String deliveryNote) {
        this.deliveryNote = deliveryNote;
    }

    public boolean isSpeedBoost() {
        return speedBoost;
    }

    public void setSpeedBoost(boolean speedBoost) {
        this.speedBoost = speedBoost;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public boolean isDimensionalTravel() {
        return dimensionalTravel;
    }

    public void setDimensionalTravel(boolean dimensionalTravel) {
        this.dimensionalTravel = dimensionalTravel;
    }

    public String getOriginalParrotName() {
        return originalParrotName;
    }

    public void setOriginalParrotName(String originalParrotName) {
        this.originalParrotName = originalParrotName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(long lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public boolean isGuiOpen() {
        return isGuiOpen;
    }

    public void setGuiOpen(boolean guiOpen) {
        isGuiOpen = guiOpen;
    }

    /**
     * Checks if the provided item satisfies the required payment.
     */
    public boolean isPaymentSatisfied(ItemStack placedItem) {
        if (paymentRequired == null || paymentRequired.getType().isAir() || paymentRequired.getAmount() <= 0) {
            return true; // Free gift delivery
        }
        if (placedItem == null || placedItem.getType().isAir()) {
            return false;
        }
        if (placedItem.getType() != paymentRequired.getType()) {
            return false;
        }
        if (placedItem.getAmount() < paymentRequired.getAmount()) {
            return false;
        }
        if (paymentRequired.hasItemMeta()) {
            if (!placedItem.hasItemMeta()) return false;
            return Objects.equals(placedItem.getItemMeta(), paymentRequired.getItemMeta());
        }
        return true;
    }

    /**
     * Persist this data model into an Entity's PersistentDataContainer.
     */
    public void saveToPdc(PersistentDataContainer pdc, ParrotCouriersPlugin plugin) {
        NamespacedKey keyIsCourier = new NamespacedKey(plugin, "is_courier");
        NamespacedKey keyState = new NamespacedKey(plugin, "courier_state");
        NamespacedKey keyOwnerUuid = new NamespacedKey(plugin, "owner_uuid");
        NamespacedKey keyOwnerName = new NamespacedKey(plugin, "owner_name");
        NamespacedKey keyTargetType = new NamespacedKey(plugin, "target_type");
        NamespacedKey keyTargetPlayer = new NamespacedKey(plugin, "target_player");
        NamespacedKey keyTargetWorld = new NamespacedKey(plugin, "target_world");
        NamespacedKey keyTargetX = new NamespacedKey(plugin, "target_x");
        NamespacedKey keyTargetY = new NamespacedKey(plugin, "target_y");
        NamespacedKey keyTargetZ = new NamespacedKey(plugin, "target_z");
        NamespacedKey keyPayload = new NamespacedKey(plugin, "payload_item");
        NamespacedKey keyPaymentReq = new NamespacedKey(plugin, "payment_required");
        NamespacedKey keyPaymentRec = new NamespacedKey(plugin, "payment_received");
        NamespacedKey keyLetter = new NamespacedKey(plugin, "letter_item");
        NamespacedKey keyNote = new NamespacedKey(plugin, "delivery_note");
        NamespacedKey keySpeed = new NamespacedKey(plugin, "speed_boost");
        NamespacedKey keyGlow = new NamespacedKey(plugin, "glowing_boost");
        NamespacedKey keyDim = new NamespacedKey(plugin, "dimensional_travel");
        NamespacedKey keyOrigName = new NamespacedKey(plugin, "original_name");
        NamespacedKey keyVariant = new NamespacedKey(plugin, "parrot_variant");

        pdc.set(keyIsCourier, PersistentDataType.BOOLEAN, true);
        if (state != null) pdc.set(keyState, PersistentDataType.STRING, state.name());
        if (ownerUuid != null) pdc.set(keyOwnerUuid, PersistentDataType.STRING, ownerUuid.toString());
        if (ownerName != null) pdc.set(keyOwnerName, PersistentDataType.STRING, ownerName);
        if (targetType != null) pdc.set(keyTargetType, PersistentDataType.STRING, targetType.name());
        if (targetPlayerName != null) pdc.set(keyTargetPlayer, PersistentDataType.STRING, targetPlayerName);
        if (targetWorldName != null) pdc.set(keyTargetWorld, PersistentDataType.STRING, targetWorldName);
        pdc.set(keyTargetX, PersistentDataType.DOUBLE, targetX);
        pdc.set(keyTargetY, PersistentDataType.DOUBLE, targetY);
        pdc.set(keyTargetZ, PersistentDataType.DOUBLE, targetZ);
        pdc.set(keySpeed, PersistentDataType.BOOLEAN, speedBoost);
        pdc.set(keyGlow, PersistentDataType.BOOLEAN, glowing);
        pdc.set(keyDim, PersistentDataType.BOOLEAN, dimensionalTravel);

        if (payloadItem != null) {
            pdc.set(keyPayload, PersistentDataType.STRING, ItemSerializer.serialize(payloadItem));
        } else {
            pdc.remove(keyPayload);
        }

        if (paymentRequired != null) {
            pdc.set(keyPaymentReq, PersistentDataType.STRING, ItemSerializer.serialize(paymentRequired));
        } else {
            pdc.remove(keyPaymentReq);
        }

        if (paymentReceived != null) {
            pdc.set(keyPaymentRec, PersistentDataType.STRING, ItemSerializer.serialize(paymentReceived));
        } else {
            pdc.remove(keyPaymentRec);
        }

        if (letterItem != null) {
            pdc.set(keyLetter, PersistentDataType.STRING, ItemSerializer.serialize(letterItem));
        } else {
            pdc.remove(keyLetter);
        }

        if (deliveryNote != null) {
            pdc.set(keyNote, PersistentDataType.STRING, deliveryNote);
        } else {
            pdc.remove(keyNote);
        }

        if (originalParrotName != null) {
            pdc.set(keyOrigName, PersistentDataType.STRING, originalParrotName);
        }

        if (parrotVariant != null) {
            pdc.set(keyVariant, PersistentDataType.STRING, parrotVariant);
        }
    }

    /**
     * Reconstruct CourierData from PersistentDataContainer.
     */
    public static CourierData loadFromPdc(UUID uuid, PersistentDataContainer pdc, ParrotCouriersPlugin plugin) {
        NamespacedKey keyIsCourier = new NamespacedKey(plugin, "is_courier");
        if (!pdc.has(keyIsCourier, PersistentDataType.BOOLEAN)) {
            return null;
        }

        CourierData data = new CourierData(uuid);
        NamespacedKey keyState = new NamespacedKey(plugin, "courier_state");
        NamespacedKey keyOwnerUuid = new NamespacedKey(plugin, "owner_uuid");
        NamespacedKey keyOwnerName = new NamespacedKey(plugin, "owner_name");
        NamespacedKey keyTargetType = new NamespacedKey(plugin, "target_type");
        NamespacedKey keyTargetPlayer = new NamespacedKey(plugin, "target_player");
        NamespacedKey keyTargetWorld = new NamespacedKey(plugin, "target_world");
        NamespacedKey keyTargetX = new NamespacedKey(plugin, "target_x");
        NamespacedKey keyTargetY = new NamespacedKey(plugin, "target_y");
        NamespacedKey keyTargetZ = new NamespacedKey(plugin, "target_z");
        NamespacedKey keyPayload = new NamespacedKey(plugin, "payload_item");
        NamespacedKey keyPaymentReq = new NamespacedKey(plugin, "payment_required");
        NamespacedKey keyPaymentRec = new NamespacedKey(plugin, "payment_received");
        NamespacedKey keyLetter = new NamespacedKey(plugin, "letter_item");
        NamespacedKey keyNote = new NamespacedKey(plugin, "delivery_note");
        NamespacedKey keySpeed = new NamespacedKey(plugin, "speed_boost");
        NamespacedKey keyGlow = new NamespacedKey(plugin, "glowing_boost");
        NamespacedKey keyDim = new NamespacedKey(plugin, "dimensional_travel");
        NamespacedKey keyOrigName = new NamespacedKey(plugin, "original_name");
        NamespacedKey keyVariant = new NamespacedKey(plugin, "parrot_variant");

        if (pdc.has(keyState, PersistentDataType.STRING)) {
            try {
                data.setState(CourierState.valueOf(pdc.get(keyState, PersistentDataType.STRING)));
            } catch (Exception ignored) {}
        }

        if (pdc.has(keyOwnerUuid, PersistentDataType.STRING)) {
            try {
                data.setOwnerUuid(UUID.fromString(pdc.get(keyOwnerUuid, PersistentDataType.STRING)));
            } catch (Exception ignored) {}
        }

        if (pdc.has(keyOwnerName, PersistentDataType.STRING)) {
            data.setOwnerName(pdc.get(keyOwnerName, PersistentDataType.STRING));
        }

        if (pdc.has(keyTargetType, PersistentDataType.STRING)) {
            try {
                data.setTargetType(TargetType.valueOf(pdc.get(keyTargetType, PersistentDataType.STRING)));
            } catch (Exception ignored) {}
        }

        if (pdc.has(keyTargetPlayer, PersistentDataType.STRING)) {
            data.setTargetPlayerName(pdc.get(keyTargetPlayer, PersistentDataType.STRING));
        }

        if (pdc.has(keyTargetWorld, PersistentDataType.STRING)) {
            data.setTargetWorldName(pdc.get(keyTargetWorld, PersistentDataType.STRING));
        }

        if (pdc.has(keyTargetX, PersistentDataType.DOUBLE)) {
            data.setTargetX(pdc.get(keyTargetX, PersistentDataType.DOUBLE));
        }

        if (pdc.has(keyTargetY, PersistentDataType.DOUBLE)) {
            data.setTargetY(pdc.get(keyTargetY, PersistentDataType.DOUBLE));
        }

        if (pdc.has(keyTargetZ, PersistentDataType.DOUBLE)) {
            data.setTargetZ(pdc.get(keyTargetZ, PersistentDataType.DOUBLE));
        }

        if (pdc.has(keySpeed, PersistentDataType.BOOLEAN)) {
            data.setSpeedBoost(Boolean.TRUE.equals(pdc.get(keySpeed, PersistentDataType.BOOLEAN)));
        }

        if (pdc.has(keyGlow, PersistentDataType.BOOLEAN)) {
            data.setGlowing(Boolean.TRUE.equals(pdc.get(keyGlow, PersistentDataType.BOOLEAN)));
        }

        if (pdc.has(keyDim, PersistentDataType.BOOLEAN)) {
            data.setDimensionalTravel(Boolean.TRUE.equals(pdc.get(keyDim, PersistentDataType.BOOLEAN)));
        }

        if (pdc.has(keyPayload, PersistentDataType.STRING)) {
            data.setPayloadItem(ItemSerializer.deserialize(pdc.get(keyPayload, PersistentDataType.STRING)));
        }

        if (pdc.has(keyPaymentReq, PersistentDataType.STRING)) {
            data.setPaymentRequired(ItemSerializer.deserialize(pdc.get(keyPaymentReq, PersistentDataType.STRING)));
        }

        if (pdc.has(keyPaymentRec, PersistentDataType.STRING)) {
            data.setPaymentReceived(ItemSerializer.deserialize(pdc.get(keyPaymentRec, PersistentDataType.STRING)));
        }

        if (pdc.has(keyLetter, PersistentDataType.STRING)) {
            data.setLetterItem(ItemSerializer.deserialize(pdc.get(keyLetter, PersistentDataType.STRING)));
        }

        if (pdc.has(keyNote, PersistentDataType.STRING)) {
            data.setDeliveryNote(pdc.get(keyNote, PersistentDataType.STRING));
        }

        if (pdc.has(keyOrigName, PersistentDataType.STRING)) {
            data.setOriginalParrotName(pdc.get(keyOrigName, PersistentDataType.STRING));
        }

        if (pdc.has(keyVariant, PersistentDataType.STRING)) {
            data.setParrotVariant(pdc.get(keyVariant, PersistentDataType.STRING));
        }

        return data;
    }

    /**
     * Map representation for YAML config persistence.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("courierUuid", courierUuid.toString());
        if (ownerUuid != null) map.put("ownerUuid", ownerUuid.toString());
        if (ownerName != null) map.put("ownerName", ownerName);
        if (targetType != null) map.put("targetType", targetType.name());
        if (targetPlayerName != null) map.put("targetPlayerName", targetPlayerName);
        if (targetWorldName != null) map.put("targetWorldName", targetWorldName);
        map.put("targetX", targetX);
        map.put("targetY", targetY);
        map.put("targetZ", targetZ);
        if (state != null) map.put("state", state.name());
        map.put("speedBoost", speedBoost);
        map.put("glowing", glowing);
        map.put("dimensionalTravel", dimensionalTravel);
        if (payloadItem != null) map.put("payloadItem", ItemSerializer.serialize(payloadItem));
        if (paymentRequired != null) map.put("paymentRequired", ItemSerializer.serialize(paymentRequired));
        if (paymentReceived != null) map.put("paymentReceived", ItemSerializer.serialize(paymentReceived));
        if (letterItem != null) map.put("letterItem", ItemSerializer.serialize(letterItem));
        if (deliveryNote != null) map.put("deliveryNote", deliveryNote);
        if (originalParrotName != null) map.put("originalParrotName", originalParrotName);
        if (parrotVariant != null) map.put("parrotVariant", parrotVariant);
        map.put("createdAt", createdAt);
        return map;
    }

    /**
     * Load model from YAML map.
     */
    public static CourierData fromMap(Map<String, Object> map) {
        if (map == null || !map.containsKey("courierUuid")) return null;
        UUID uuid = UUID.fromString((String) map.get("courierUuid"));
        CourierData data = new CourierData(uuid);

        if (map.containsKey("ownerUuid")) {
            data.setOwnerUuid(UUID.fromString((String) map.get("ownerUuid")));
        }
        if (map.containsKey("ownerName")) {
            data.setOwnerName((String) map.get("ownerName"));
        }
        if (map.containsKey("targetType")) {
            data.setTargetType(TargetType.valueOf((String) map.get("targetType")));
        }
        if (map.containsKey("targetPlayerName")) {
            data.setTargetPlayerName((String) map.get("targetPlayerName"));
        }
        if (map.containsKey("targetWorldName")) {
            data.setTargetWorldName((String) map.get("targetWorldName"));
        }
        if (map.containsKey("targetX")) {
            data.setTargetX(((Number) map.get("targetX")).doubleValue());
        }
        if (map.containsKey("targetY")) {
            data.setTargetY(((Number) map.get("targetY")).doubleValue());
        }
        if (map.containsKey("targetZ")) {
            data.setTargetZ(((Number) map.get("targetZ")).doubleValue());
        }
        if (map.containsKey("speedBoost")) {
            data.setSpeedBoost((Boolean) map.get("speedBoost"));
        }
        if (map.containsKey("glowing")) {
            data.setGlowing((Boolean) map.get("glowing"));
        }
        if (map.containsKey("dimensionalTravel")) {
            data.setDimensionalTravel((Boolean) map.get("dimensionalTravel"));
        }
        if (map.containsKey("state")) {
            data.setState(CourierState.valueOf((String) map.get("state")));
        }
        if (map.containsKey("payloadItem")) {
            data.setPayloadItem(ItemSerializer.deserialize((String) map.get("payloadItem")));
        }
        if (map.containsKey("paymentRequired")) {
            data.setPaymentRequired(ItemSerializer.deserialize((String) map.get("paymentRequired")));
        }
        if (map.containsKey("paymentReceived")) {
            data.setPaymentReceived(ItemSerializer.deserialize((String) map.get("paymentReceived")));
        }
        if (map.containsKey("letterItem")) {
            data.setLetterItem(ItemSerializer.deserialize((String) map.get("letterItem")));
        }
        if (map.containsKey("deliveryNote")) {
            data.setDeliveryNote((String) map.get("deliveryNote"));
        }
        if (map.containsKey("originalParrotName")) {
            data.setOriginalParrotName((String) map.get("originalParrotName"));
        }
        if (map.containsKey("parrotVariant")) {
            data.setParrotVariant((String) map.get("parrotVariant"));
        }
        if (map.containsKey("createdAt")) {
            data.setCreatedAt(((Number) map.get("createdAt")).longValue());
        }

        return data;
    }
}
