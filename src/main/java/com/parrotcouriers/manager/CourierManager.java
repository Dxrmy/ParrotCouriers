package com.parrotcouriers.manager;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.model.CourierState;
import com.parrotcouriers.model.DeliveryRecord;
import com.parrotcouriers.model.TargetType;
import com.parrotcouriers.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active Parrot Couriers, world-aware coordinate targeting, delivery history, and perches.
 */
public class CourierManager {

    private final ParrotCouriersPlugin plugin;
    private final Map<UUID, CourierData> couriers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> perches = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> perchPriorities = new ConcurrentHashMap<>();
    private final List<DeliveryRecord> history = new ArrayList<>();
    private final File dataFile;
    private final File historyFile;
    private final File perchFile;

    public CourierManager(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "couriers.yml");
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        this.perchFile = new File(plugin.getDataFolder(), "perches.yml");
    }

    /**
     * Start configuring a tamed parrot as a courier.
     */
    public CourierData registerNewCourier(Parrot parrot, Player owner, String targetRaw) {
        UUID parrotUuid = parrot.getUniqueId();
        CourierData data = new CourierData(parrotUuid);
        data.setOwnerUuid(owner.getUniqueId());
        data.setOwnerName(owner.getName());
        data.setOriginalParrotName(parrot.getCustomName());
        data.setParrotVariant(parrot.getVariant().name());

        String cleaned = targetRaw.trim().replaceAll("[,;]", " ");
        String[] parts = cleaned.split("\\s+");

        if (parts.length >= 4) {
            try {
                String worldSpecifier = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                World resolved = resolveWorld(worldSpecifier, owner.getWorld());
                data.setTargetType(TargetType.COORDINATES);
                data.setTargetCoordinates(resolved, x, y, z);
            } catch (NumberFormatException e) {
                data.setTargetType(TargetType.PLAYER);
                data.setTargetPlayerName(targetRaw.trim());
            }
        } else if (parts.length == 3) {
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                data.setTargetType(TargetType.COORDINATES);
                data.setTargetCoordinates(owner.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                data.setTargetType(TargetType.PLAYER);
                data.setTargetPlayerName(targetRaw.trim());
            }
        } else {
            data.setTargetType(TargetType.PLAYER);
            data.setTargetPlayerName(targetRaw.trim());
        }

        data.setState(CourierState.AWAITING_PAYLOAD);
        couriers.put(parrotUuid, data);

        data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
        saveAll();
        return data;
    }

    private World resolveWorld(String name, World fallback) {
        if (name == null || name.isEmpty()) return fallback;
        World exact = Bukkit.getWorld(name);
        if (exact != null) return exact;

        String lower = name.toLowerCase();
        if (lower.equals("nether") || lower.equals("the_nether") || lower.equals("world_nether")) {
            World nether = Bukkit.getWorld("world_nether");
            if (nether == null) nether = Bukkit.getWorld("the_nether");
            if (nether != null) return nether;
        }
        if (lower.equals("end") || lower.equals("the_end") || lower.equals("world_the_end")) {
            World end = Bukkit.getWorld("world_the_end");
            if (end == null) end = Bukkit.getWorld("the_end");
            if (end != null) return end;
        }
        if (lower.equals("overworld") || lower.equals("world")) {
            World overworld = Bukkit.getWorld("world");
            if (overworld != null) return overworld;
        }

        for (World w : Bukkit.getWorlds()) {
            if (w.getName().equalsIgnoreCase(name) || w.getName().toLowerCase().endsWith(lower)) {
                return w;
            }
        }
        return fallback;
    }

    public CourierData getCourier(UUID uuid) {
        return couriers.get(uuid);
    }

    public CourierData getCourier(Entity entity) {
        if (entity == null) return null;
        return couriers.get(entity.getUniqueId());
    }

    public boolean isCourier(Entity entity) {
        if (!(entity instanceof Parrot parrot)) return false;
        if (couriers.containsKey(parrot.getUniqueId())) return true;
        NamespacedKey key = new NamespacedKey(plugin, "is_courier");
        return parrot.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    public Collection<CourierData> getAllCouriers() {
        return Collections.unmodifiableCollection(couriers.values());
    }

    public List<CourierData> getCouriersByOwner(UUID ownerUuid) {
        List<CourierData> list = new ArrayList<>();
        for (CourierData data : couriers.values()) {
            if (Objects.equals(data.getOwnerUuid(), ownerUuid)) {
                list.add(data);
            }
        }
        return list;
    }

    public void setPlayerPerch(UUID playerUuid, Location loc) {
        if (loc != null) {
            perches.put(playerUuid, loc);
        } else {
            perches.remove(playerUuid);
        }
        savePerches();
    }

    public Location getPlayerPerch(UUID playerUuid) {
        return perches.get(playerUuid);
    }

    public void setPerchPriority(UUID playerUuid, boolean priority) {
        perchPriorities.put(playerUuid, priority);
        savePerches();
    }

    public boolean isPerchPrioritized(UUID playerUuid) {
        if (perchPriorities.containsKey(playerUuid)) {
            return perchPriorities.get(playerUuid);
        }
        return plugin.getConfigManager().isPrioritizePerchesDefault();
    }

    public void addHistoryEntry(DeliveryRecord record) {
        history.add(0, record);
        if (history.size() > 50) {
            history.remove(history.size() - 1);
        }
        saveHistory();
    }

    public List<DeliveryRecord> getHistoryForPlayer(String playerName) {
        List<DeliveryRecord> list = new ArrayList<>();
        for (DeliveryRecord r : history) {
            if (r.senderName().equalsIgnoreCase(playerName) || r.recipientName().equalsIgnoreCase(playerName)) {
                list.add(r);
            }
        }
        return list;
    }

    public void activateCourierMode(Parrot parrot, CourierData data) {
        data.setState(CourierState.IN_TRANSIT_TO_DESTINATION);
        
        parrot.setTamed(false);
        parrot.setOwner(null);
        parrot.setTarget(null);
        parrot.getPathfinder().stopPathfinding();

        parrot.setInvulnerable(true);
        parrot.setSitting(false);
        parrot.setRemoveWhenFarAway(false);
        if (data.isGlowing()) {
            parrot.setGlowing(true);
        }

        String targetDisplay = (data.getTargetType() == TargetType.COORDINATES)
                ? String.format("%.0f, %.0f, %.0f (%s)", data.getTargetX(), data.getTargetY(), data.getTargetZ(), data.getTargetWorldName())
                : data.getTargetPlayerName();

        if (plugin.getConfigManager().isShowNametags()) {
            parrot.customName(TextUtil.parse("<gold><b>[Courier]</b></gold> <gray>→</gray> <yellow>" + targetDisplay + "</yellow>"));
            parrot.setCustomNameVisible(true);
        } else {
            parrot.customName(null);
            parrot.setCustomNameVisible(false);
        }

        data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
        saveAll();
    }

    public void finalizeAndRevert(Parrot parrot, CourierData data) {
        data.setState(CourierState.COMPLETED);
        couriers.remove(data.getCourierUuid());

        if (parrot != null && parrot.isValid()) {
            PersistentDataContainer pdc = parrot.getPersistentDataContainer();
            pdc.remove(new NamespacedKey(plugin, "is_courier"));
            pdc.remove(new NamespacedKey(plugin, "courier_state"));
            pdc.remove(new NamespacedKey(plugin, "payload_item"));
            pdc.remove(new NamespacedKey(plugin, "payment_required"));
            pdc.remove(new NamespacedKey(plugin, "payment_received"));
            pdc.remove(new NamespacedKey(plugin, "letter_item"));
            pdc.remove(new NamespacedKey(plugin, "delivery_note"));
            pdc.remove(new NamespacedKey(plugin, "speed_boost"));
            pdc.remove(new NamespacedKey(plugin, "glowing_boost"));
            pdc.remove(new NamespacedKey(plugin, "dimensional_travel"));
            pdc.remove(new NamespacedKey(plugin, "original_name"));
            pdc.remove(new NamespacedKey(plugin, "parrot_variant"));

            parrot.setInvulnerable(false);
            parrot.setGlowing(false);

            if (data.getParrotVariant() != null) {
                try {
                    parrot.setVariant(Parrot.Variant.valueOf(data.getParrotVariant()));
                } catch (Exception ignored) {}
            }
            
            if (data.getOwnerUuid() != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(data.getOwnerUuid());
                parrot.setTamed(true);
                parrot.setOwner(owner);
            }

            if (data.getOriginalParrotName() != null && !data.getOriginalParrotName().isEmpty()) {
                parrot.customName(TextUtil.parse(data.getOriginalParrotName()));
                parrot.setCustomNameVisible(true);
            } else {
                parrot.customName(null);
                parrot.setCustomNameVisible(false);
            }
            parrot.getPathfinder().stopPathfinding();
            parrot.setVelocity(new Vector(0, 0, 0));
            parrot.setSitting(true);
        }

        saveAll();
    }

    public Parrot findParrotEntity(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity instanceof Parrot parrot && parrot.isValid()) {
            return parrot;
        }
        CourierData data = couriers.get(uuid);
        if (data != null && (data.getLastTrackedX() != 0 || data.getLastTrackedZ() != 0)) {
            World world = (data.getTargetWorldName() != null) ? Bukkit.getWorld(data.getTargetWorldName()) : null;
            if (world != null) {
                int chunkX = ((int) data.getLastTrackedX()) >> 4;
                int chunkZ = ((int) data.getLastTrackedZ()) >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.getChunkAt(chunkX, chunkZ);
                }
                Entity e = Bukkit.getEntity(uuid);
                if (e instanceof Parrot p && p.isValid()) {
                    return p;
                }
            }
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntitiesByClass(Parrot.class)) {
                if (e.getUniqueId().equals(uuid) && e.isValid()) {
                    return (Parrot) e;
                }
            }
        }
        return null;
    }

    public void loadAll() {
        loadCouriers();
        loadPerches();
        loadHistory();
    }

    private void loadCouriers() {
        couriers.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.isConfigurationSection("couriers")) return;

        for (String key : config.getConfigurationSection("couriers").getKeys(false)) {
            try {
                Map<String, Object> map = config.getConfigurationSection("couriers." + key).getValues(false);
                CourierData data = CourierData.fromMap(map);
                if (data != null && data.getState() != CourierState.COMPLETED) {
                    couriers.put(data.getCourierUuid(), data);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load courier entry: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + couriers.size() + " active parrot courier(s).");
    }

    private void loadPerches() {
        perches.clear();
        perchPriorities.clear();
        if (!perchFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(perchFile);
        if (!config.isConfigurationSection("perches")) return;
        for (String key : config.getConfigurationSection("perches").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                Location loc = config.getLocation("perches." + key + ".location");
                if (loc != null) perches.put(uuid, loc);
                if (config.contains("perches." + key + ".priority")) {
                    perchPriorities.put(uuid, config.getBoolean("perches." + key + ".priority"));
                }
            } catch (Exception ignored) {}
        }
    }

    private void loadHistory() {
        history.clear();
        if (!historyFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
        List<?> list = config.getList("history");
        if (list != null) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    try {
                        long ts = ((Number) map.get("timestamp")).longValue();
                        String sender = (String) map.get("sender");
                        String recipient = (String) map.get("recipient");
                        String payload = (String) map.get("payload");
                        String payment = (String) map.get("payment");
                        boolean gift = Boolean.TRUE.equals(map.get("gift"));
                        history.add(new DeliveryRecord(ts, sender, recipient, payload, payment, gift));
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public void saveAll() {
        saveCouriers();
        savePerches();
        saveHistory();
    }

    private void saveCouriers() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, CourierData> entry : couriers.entrySet()) {
            config.createSection("couriers." + entry.getKey().toString(), entry.getValue().toMap());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save couriers.yml: " + e.getMessage());
        }
    }

    private void savePerches() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Location> entry : perches.entrySet()) {
            String path = "perches." + entry.getKey().toString();
            config.set(path + ".location", entry.getValue());
            if (perchPriorities.containsKey(entry.getKey())) {
                config.set(path + ".priority", perchPriorities.get(entry.getKey()));
            }
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(perchFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save perches.yml: " + e.getMessage());
        }
    }

    private void saveHistory() {
        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DeliveryRecord r : history) {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", r.timestamp());
            map.put("sender", r.senderName());
            map.put("recipient", r.recipientName());
            map.put("payload", r.payloadSummary());
            map.put("payment", r.paymentSummary());
            map.put("gift", r.wasGift());
            list.add(map);
        }
        config.set("history", list);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            config.save(historyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save history.yml: " + e.getMessage());
        }
    }
}
