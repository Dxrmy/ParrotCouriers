package com.parrotcouriers.flight;

import com.parrotcouriers.ParrotCouriersPlugin;
import com.parrotcouriers.config.ConfigManager;
import com.parrotcouriers.model.CourierData;
import com.parrotcouriers.model.CourierState;
import com.parrotcouriers.model.TargetType;
import com.parrotcouriers.util.TextUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flight engine handling courier movement, pathfinding waypoints,
 * obstacle avoidance, chunk loading, and arrival detection.
 */
public class FlightEngine extends BukkitRunnable {

    private final ParrotCouriersPlugin plugin;
    private long tickCounter = 0;

    private final Map<UUID, CachedPath> pathCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> activeChunkTickets = new ConcurrentHashMap<>();

    public FlightEngine(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
    }

    private static class CachedPath {
        List<Vector> waypoints;
        int currentIndex;
        Location lastTarget;
        long lastCalculated;

        CachedPath(List<Vector> waypoints, Location target) {
            this.waypoints = waypoints;
            this.currentIndex = 0;
            this.lastTarget = target.clone();
            this.lastCalculated = System.currentTimeMillis();
        }

        boolean isValid(Location currentTarget) {
            if (waypoints == null || waypoints.isEmpty()) return false;
            if (System.currentTimeMillis() - lastCalculated > 3000) return false;
            return lastTarget.getWorld().equals(currentTarget.getWorld()) && lastTarget.distanceSquared(currentTarget) < 4.0 * 4.0;
        }

        Vector getNextWaypoint(Location cur) {
            while (currentIndex < waypoints.size()) {
                Vector wp = waypoints.get(currentIndex);
                if (cur.toVector().distanceSquared(wp) < 1.8 * 1.8) {
                    currentIndex++;
                } else {
                    return wp;
                }
            }
            return (waypoints.isEmpty()) ? null : waypoints.get(waypoints.size() - 1);
        }
    }

    @Override
    public void run() {
        tickCounter++;
        ConfigManager cfg = plugin.getConfigManager();

        for (CourierData data : plugin.getCourierManager().getAllCouriers()) {
            Parrot parrot = plugin.getCourierManager().findParrotEntity(data.getCourierUuid());
            if (parrot == null || !parrot.isValid()) {
                pathCache.remove(data.getCourierUuid());
                continue;
            }

            // Keep chunk loaded while courier is active in transit
            updateChunkTicket(parrot, data);

            CourierState state = data.getState();

            if ((state == CourierState.AWAITING_PAYLOAD || state == CourierState.AWAITING_PAYMENT) && cfg.isFreezeDuringSetup()) {
                if (!parrot.isSitting()) {
                    parrot.setSitting(true);
                    parrot.setVelocity(new Vector(0, 0, 0));
                }
                continue;
            }

            parrot.setInvulnerable(true);
            parrot.setRemoveWhenFarAway(false);
            parrot.setPersistent(true);

            if (data.isGlowing()) {
                parrot.setGlowing(true);
            }

            if (state == CourierState.IN_TRANSIT_TO_DESTINATION) {
                handleTransitToDestination(parrot, data, cfg);
            } else if (state == CourierState.WAITING_FOR_RECIPIENT) {
                handleWaitingForRecipient(parrot, data, cfg, tickCounter % cfg.getActionBarIntervalTicks() == 0);
            } else if (state == CourierState.IN_TRANSIT_TO_OWNER) {
                handleTransitToOwner(parrot, data, cfg);
            } else if (state == CourierState.WAITING_FOR_OWNER) {
                handleWaitingForOwner(parrot, data, cfg, tickCounter % cfg.getActionBarIntervalTicks() == 0);
            }
        }
    }

    private void updateChunkTicket(Parrot parrot, CourierData data) {
        World world = parrot.getWorld();
        int chunkX = parrot.getLocation().getBlockX() >> 4;
        int chunkZ = parrot.getLocation().getBlockZ() >> 4;
        long chunkKey = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);

        Long current = activeChunkTickets.get(data.getCourierUuid());
        if (current == null || current != chunkKey) {
            if (current != null) {
                int oldX = (int) (current >> 32);
                int oldZ = (int) (long) current;
                world.removePluginChunkTicket(oldX, oldZ, plugin);
            }
            world.addPluginChunkTicket(chunkX, chunkZ, plugin);
            activeChunkTickets.put(data.getCourierUuid(), chunkKey);
        }
    }

    public void releaseChunkTicket(UUID courierUuid, World world) {
        Long current = activeChunkTickets.remove(courierUuid);
        if (current != null && world != null) {
            int oldX = (int) (current >> 32);
            int oldZ = (int) (long) current;
            world.removePluginChunkTicket(oldX, oldZ, plugin);
        }
    }

    private void handleTransitToDestination(Parrot parrot, CourierData data, ConfigManager cfg) {
        Location targetLoc = null;
        boolean isPerchTarget = false;

        if (data.getTargetType() == TargetType.PLAYER) {
            Player targetPlayer = Bukkit.getPlayerExact(data.getTargetPlayerName());
            OfflinePlayer offlinePlayer = (targetPlayer != null) ? targetPlayer : Bukkit.getOfflinePlayer(data.getTargetPlayerName());
            UUID recipientUuid = offlinePlayer.getUniqueId();
            Location playerPerch = (recipientUuid != null) ? plugin.getCourierManager().getPlayerPerch(recipientUuid) : null;
            boolean preferPerch = (recipientUuid != null) && plugin.getCourierManager().isPerchPrioritized(recipientUuid);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                if (playerPerch != null && playerPerch.getWorld().equals(parrot.getWorld())) {
                    targetLoc = playerPerch.clone().add(0.5, 1.0, 0.5);
                    isPerchTarget = true;
                } else {
                    parrot.setSitting(true);
                    if (cfg.isDancingWhileWaiting()) {
                        doWaitingAnimation(parrot);
                    }
                    if (cfg.isShowNametags()) {
                        parrot.customName(TextUtil.parse("<gray>✉ Waiting for <yellow>" + data.getTargetPlayerName() + "</yellow> to connect...</gray>"));
                    }
                    return;
                }
            } else {
                if (!targetPlayer.getWorld().equals(parrot.getWorld())) {
                    if (cfg.isInterdimensionalEnabled()) {
                        if (cfg.isRequireChorusFruit() && !data.isDimensionalTravel()) {
                            parrot.setSitting(true);
                            if (cfg.isShowNametags()) {
                                parrot.customName(TextUtil.parse("<gray>✉ Target in another dimension <red>(Needs Chorus Fruit)</red></gray>"));
                            }
                            return;
                        }
                        handleDimensionalAscentAndWarp(parrot, data, targetPlayer.getWorld(), targetPlayer.getLocation(), cfg);
                        return;
                    } else {
                        parrot.setSitting(true);
                        if (cfg.isShowNametags()) {
                            parrot.customName(TextUtil.parse("<gray>✉ Target in another dimension</gray>"));
                        }
                        return;
                    }
                }

                if (preferPerch && playerPerch != null && playerPerch.getWorld().equals(parrot.getWorld())) {
                    targetLoc = playerPerch.clone().add(0.5, 1.0, 0.5);
                    isPerchTarget = true;
                } else {
                    targetLoc = targetPlayer.getLocation().add(0, 1.2, 0);
                }
            }
        } else {
            World targetWorld = (data.getTargetWorldName() != null) ? Bukkit.getWorld(data.getTargetWorldName()) : parrot.getWorld();
            if (targetWorld != null && !targetWorld.equals(parrot.getWorld())) {
                if (cfg.isInterdimensionalEnabled()) {
                    if (cfg.isRequireChorusFruit() && !data.isDimensionalTravel()) {
                        parrot.setSitting(true);
                        if (cfg.isShowNametags()) {
                            parrot.customName(TextUtil.parse("<gray>✉ Target in another dimension <red>(Needs Chorus Fruit)</red></gray>"));
                        }
                        return;
                    }
                    Location coordTarget = new Location(targetWorld, data.getTargetX(), data.getTargetY(), data.getTargetZ());
                    handleDimensionalAscentAndWarp(parrot, data, targetWorld, coordTarget, cfg);
                    return;
                }
            }
            targetLoc = data.getTargetLocation(Bukkit.getServer());
        }

        if (targetLoc == null || !targetLoc.getWorld().equals(parrot.getWorld())) {
            return;
        }

        Location cur = parrot.getLocation();

        // 1. Initialize ETA Timeout on flight start
        initializeFlightTimeout(cur, targetLoc, data, cfg);

        // 2. Stuck Detection & Timeout Teleport Rescue Check
        if (checkStuckAndTimeoutRescue(parrot, cur, targetLoc, data, cfg, true, isPerchTarget)) {
            return;
        }

        // 3. Normal Arrival Check (within 2.2 blocks)
        double distSq = cur.distanceSquared(targetLoc);
        if (distSq <= 2.2 * 2.2) {
            pathCache.remove(data.getCourierUuid());
            releaseChunkTicket(data.getCourierUuid(), parrot.getWorld());
            data.setState(CourierState.WAITING_FOR_RECIPIENT);
            data.setStuckTicks(0);
            data.setRetryCount(0);
            data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            plugin.getCourierManager().saveAll();

            parrot.setVelocity(new Vector(0, 0.05, 0));
            parrot.setSitting(false);
            if (cfg.isShowNametags()) {
                parrot.customName(TextUtil.parse("<green><b>[Delivery Ready]</b></green> <yellow>Sneak to trade</yellow>"));
                parrot.setCustomNameVisible(true);
            }

            if (cfg.isSoundsEnabled()) {
                parrot.getWorld().playSound(parrot.getLocation(), cfg.getChimeSound(), 1.2f, 1.2f);
                parrot.getWorld().playSound(parrot.getLocation(), cfg.getAmbientSound(), 1.0f, 1.0f);
            }
            parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.5, 0), 10, 0.25, 0.25, 0.25, 0.04);

            if (data.getTargetType() == TargetType.PLAYER) {
                Player recipient = Bukkit.getPlayerExact(data.getTargetPlayerName());
                if (recipient != null && recipient.isOnline()) {
                    if (isPerchTarget) {
                        String perchMsg = "<gold><b>[Courier]</b> A delivery from <yellow>" + data.getOwnerName() + "</yellow> has arrived at your <green>Delivery Perch</green>! (<yellow>" + targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ() + "</yellow>)</gold>";
                        TextUtil.sendMessage(recipient, perchMsg);
                        TextUtil.sendActionBar(recipient, "<gold><b>[Courier]</b> Package arrived at your Delivery Perch!</gold>");
                    } else {
                        String msg = cfg.getMessage("actionbar-delivery", "<gold><b>[Courier]</b> Delivery from <yellow>%owner%</yellow> - <green>Sneak to accept</green></gold>")
                                .replace("%owner%", data.getOwnerName());
                        TextUtil.sendActionBar(recipient, msg);
                        TextUtil.sendMessage(recipient, "<green>✔ A courier parrot arrived with a delivery from <yellow>" + data.getOwnerName() + "</yellow>! Sneak near it to open the package.</green>");
                    }
                }
            }
            return;
        }

        // 4. Live ETA Action Bar updates
        if (cfg.isShowEtaActionBar() && tickCounter % 20 == 0) {
            double horizDist = Math.sqrt(Math.pow(targetLoc.getX() - cur.getX(), 2) + Math.pow(targetLoc.getZ() - cur.getZ(), 2));
            double speed = data.isSpeedBoost() ? cfg.getFlightSpeed() * cfg.getSpeedBoostMultiplier() : cfg.getFlightSpeed();
            int etaSeconds = Math.max(1, (int) (horizDist / (speed * 20.0)));

            Player owner = (data.getOwnerUuid() != null) ? Bukkit.getPlayer(data.getOwnerUuid()) : null;
            if (owner != null && owner.isOnline()) {
                String targetDisplay = (data.getTargetType() == TargetType.COORDINATES) ? "Coordinates" : data.getTargetPlayerName();
                TextUtil.sendActionBar(owner, "<gold><b>[Courier]</b> Delivering to <yellow>" + targetDisplay + "</yellow> • ETA: <green>" + etaSeconds + "s</green></gold>");
            }

            if (data.getTargetType() == TargetType.PLAYER) {
                Player recipient = Bukkit.getPlayerExact(data.getTargetPlayerName());
                if (recipient != null && recipient.isOnline() && recipient.getWorld().equals(parrot.getWorld())) {
                    TextUtil.sendActionBar(recipient, "<gold><b>[Courier]</b> Incoming delivery from <yellow>" + data.getOwnerName() + "</yellow> • ETA: <green>" + etaSeconds + "s</green></gold>");
                }
            }
        }

        // 5. Baritone 3D Flight Navigation
        flyTowardsBaritone(parrot, cur, targetLoc, data, cfg);
    }

    private void handleWaitingForRecipient(Parrot parrot, CourierData data, ConfigManager cfg, boolean shouldNotify) {
        hoverInPlace(parrot);

        if (cfg.isDancingWhileWaiting()) {
            doWaitingAnimation(parrot);
        }

        if (cfg.isStateParticles()) {
            parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.4, 0), cfg.getHoverParticleCount(), 0.2, 0.2, 0.2, 0.01);
            if (data.isGlowing()) {
                parrot.getWorld().spawnParticle(Particle.WAX_OFF, parrot.getLocation().add(0, 0.3, 0), 1, 0.1, 0.1, 0.1, 0.01);
            }
        }

        if (shouldNotify) {
            String msg = cfg.getMessage("actionbar-delivery", "<gold><b>[Courier]</b> Delivery from <yellow>%owner%</yellow> - <green>Sneak to accept</green></gold>")
                    .replace("%owner%", data.getOwnerName());

            if (data.getTargetType() == TargetType.PLAYER) {
                Player recipient = Bukkit.getPlayerExact(data.getTargetPlayerName());
                if (recipient != null && recipient.isOnline() && recipient.getWorld().equals(parrot.getWorld())) {
                    if (recipient.getLocation().distanceSquared(parrot.getLocation()) < 25 * 25) {
                        TextUtil.sendActionBar(recipient, msg);
                    }
                }
            } else {
                for (Entity entity : parrot.getNearbyEntities(6, 6, 6)) {
                    if (entity instanceof Player player) {
                        TextUtil.sendActionBar(player, "<gold><b>[Courier]</b> Delivery waiting at coordinates - <green>Sneak to accept</green></gold>");
                    }
                }
            }
        }
    }

    private void handleTransitToOwner(Parrot parrot, CourierData data, ConfigManager cfg) {
        Location targetLoc = null;
        boolean isOwnerPerch = false;
        Player owner = (data.getOwnerUuid() != null) ? Bukkit.getPlayer(data.getOwnerUuid()) : null;
        Location ownerPerch = (data.getOwnerUuid() != null) ? plugin.getCourierManager().getPlayerPerch(data.getOwnerUuid()) : null;
        boolean preferPerch = (data.getOwnerUuid() != null) && plugin.getCourierManager().isPerchPrioritized(data.getOwnerUuid());

        if (owner != null && owner.isOnline()) {
            if (!owner.getWorld().equals(parrot.getWorld())) {
                if (cfg.isInterdimensionalEnabled()) {
                    if (cfg.isRequireChorusFruit() && !data.isDimensionalTravel()) {
                        parrot.setSitting(true);
                        if (cfg.isShowNametags()) {
                            parrot.customName(TextUtil.parse("<gray>✉ Owner in another dimension <red>(Needs Chorus Fruit)</red></gray>"));
                        }
                        return;
                    }
                    handleDimensionalAscentAndWarp(parrot, data, owner.getWorld(), owner.getLocation(), cfg);
                    return;
                }
            }

            if (preferPerch && ownerPerch != null && ownerPerch.getWorld().equals(parrot.getWorld())) {
                targetLoc = ownerPerch.clone().add(0.5, 1.0, 0.5);
                isOwnerPerch = true;
            } else if (owner.getWorld().equals(parrot.getWorld())) {
                targetLoc = owner.getLocation().add(0, 1.2, 0);
            }
        } else {
            if (ownerPerch != null && ownerPerch.getWorld().equals(parrot.getWorld())) {
                targetLoc = ownerPerch.clone().add(0.5, 1.0, 0.5);
                isOwnerPerch = true;
            } else {
                targetLoc = parrot.getWorld().getSpawnLocation();
            }
        }

        if (targetLoc == null || !targetLoc.getWorld().equals(parrot.getWorld())) {
            return;
        }

        Location cur = parrot.getLocation();

        // 1. Initialize ETA Timeout on return start
        initializeFlightTimeout(cur, targetLoc, data, cfg);

        // 2. Stuck Detection & Timeout Teleport Rescue Check
        if (checkStuckAndTimeoutRescue(parrot, cur, targetLoc, data, cfg, false, isOwnerPerch)) {
            return;
        }

        // 3. Normal Return Arrival Check (within 2.2 blocks)
        double distSq = cur.distanceSquared(targetLoc);
        if (distSq <= 2.2 * 2.2) {
            pathCache.remove(data.getCourierUuid());
            releaseChunkTicket(data.getCourierUuid(), parrot.getWorld());
            data.setState(CourierState.WAITING_FOR_OWNER);
            data.setStuckTicks(0);
            data.setRetryCount(0);
            data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            plugin.getCourierManager().saveAll();

            parrot.setVelocity(new Vector(0, 0.05, 0));
            parrot.setSitting(false);
            if (cfg.isShowNametags()) {
                parrot.customName(TextUtil.parse("<green><b>[Courier Returned]</b></green> <yellow>Sneak to claim</yellow>"));
                parrot.setCustomNameVisible(true);
            }

            if (cfg.isSoundsEnabled()) {
                parrot.getWorld().playSound(parrot.getLocation(), cfg.getBellSound(), 1.2f, 1.4f);
            }
            parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.5, 0), 10, 0.25, 0.25, 0.25, 0.04);

            if (owner != null && owner.isOnline()) {
                if (isOwnerPerch) {
                    String perchMsg = "<gold><b>[Courier]</b> Your returned courier has landed at your <green>Delivery Perch</green>! (<yellow>" + targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ() + "</yellow>)</gold>";
                    TextUtil.sendMessage(owner, perchMsg);
                    TextUtil.sendActionBar(owner, "<gold><b>[Courier]</b> Courier returned at your Delivery Perch!</gold>");
                } else {
                    String msg = cfg.getMessage("actionbar-returned", "<gold><b>[Courier]</b> Your courier returned! - <green>Sneak to collect</green></gold>");
                    TextUtil.sendActionBar(owner, msg);
                    TextUtil.sendMessage(owner, "<green>✔ Your courier returned with your items! Sneak near it to claim.</green>");
                }
            }
            return;
        }

        // 4. Live ETA Action Bar updates
        if (cfg.isShowEtaActionBar() && tickCounter % 20 == 0) {
            double horizDist = Math.sqrt(Math.pow(targetLoc.getX() - cur.getX(), 2) + Math.pow(targetLoc.getZ() - cur.getZ(), 2));
            double speed = data.isSpeedBoost() ? cfg.getFlightSpeed() * cfg.getSpeedBoostMultiplier() : cfg.getFlightSpeed();
            int etaSeconds = Math.max(1, (int) (horizDist / (speed * 20.0)));

            if (owner != null && owner.isOnline()) {
                TextUtil.sendActionBar(owner, "<gold><b>[Courier]</b> Returning to you • ETA: <green>" + etaSeconds + "s</green></gold>");
            }
        }

        // 5. Baritone 3D Flight Navigation
        flyTowardsBaritone(parrot, cur, targetLoc, data, cfg);
    }

    private void handleWaitingForOwner(Parrot parrot, CourierData data, ConfigManager cfg, boolean shouldNotify) {
        hoverInPlace(parrot);

        if (cfg.isDancingWhileWaiting()) {
            doWaitingAnimation(parrot);
        }

        if (cfg.isStateParticles()) {
            parrot.getWorld().spawnParticle(cfg.getHoverParticle(), parrot.getLocation().add(0, 0.4, 0), cfg.getHoverParticleCount(), 0.2, 0.2, 0.2, 0.01);
        }

        if (shouldNotify) {
            Player owner = (data.getOwnerUuid() != null) ? Bukkit.getPlayer(data.getOwnerUuid()) : null;
            if (owner != null && owner.isOnline() && owner.getWorld().equals(parrot.getWorld())) {
                if (owner.getLocation().distanceSquared(parrot.getLocation()) < 25 * 25) {
                    String msg = cfg.getMessage("actionbar-returned", "<gold><b>[Courier]</b> Your courier returned! - <green>Sneak to collect</green></gold>");
                    TextUtil.sendActionBar(owner, msg);
                }
            }
        }
    }

    private void initializeFlightTimeout(Location cur, Location targetLoc, CourierData data, ConfigManager cfg) {
        if (data.getTransitStartTime() <= 0 || data.getMaxTransitTimeoutMs() <= 0) {
            double distance = Math.sqrt(cur.distanceSquared(targetLoc));
            long maxTimeoutMs = (cfg.getTimeoutBaseSeconds() + (long) ((distance / 100.0) * cfg.getTimeoutPer100BlocksSeconds())) * 1000L;
            data.setMaxTransitTimeoutMs(maxTimeoutMs);
            data.setTransitStartTime(System.currentTimeMillis());
            data.setLastTrackedPosition(cur.getX(), cur.getY(), cur.getZ());
            data.setStuckTicks(0);
            data.setRetryCount(0);
        }
    }

    private boolean checkStuckAndTimeoutRescue(Parrot parrot, Location cur, Location targetLoc, CourierData data, ConfigManager cfg, boolean isToRecipient, boolean isPerch) {
        double moveDistSq = Math.pow(cur.getX() - data.getLastTrackedX(), 2) + Math.pow(cur.getY() - data.getLastTrackedY(), 2) + Math.pow(cur.getZ() - data.getLastTrackedZ(), 2);
        long elapsedTransit = System.currentTimeMillis() - data.getTransitStartTime();

        if (moveDistSq < 0.45 * 0.45) {
            data.incrementStuckTicks();
            if (data.getStuckTicks() >= cfg.getStuckRetryTicks()) {
                data.setStuckTicks(0);
                data.incrementRetryCount();
                pathCache.remove(data.getCourierUuid());

                Vector escapeVector = findBestEscapeVector(cur);
                parrot.setVelocity(escapeVector.multiply(0.38));
                parrot.getWorld().spawnParticle(Particle.CLOUD, parrot.getLocation().add(0, 0.3, 0), 6, 0.2, 0.2, 0.2, 0.05);
                parrot.getWorld().playSound(parrot.getLocation(), Sound.ENTITY_PARROT_FLY, 1.0f, 1.3f);
            }
        } else {
            data.setLastTrackedPosition(cur.getX(), cur.getY(), cur.getZ());
            data.setStuckTicks(Math.max(0, data.getStuckTicks() - 2));
        }

        boolean timeoutExpired = (data.getMaxTransitTimeoutMs() > 0 && elapsedTransit > data.getMaxTransitTimeoutMs());
        boolean maxRetriesExceeded = (data.getRetryCount() >= cfg.getMaxStuckRetries());

        if (timeoutExpired || maxRetriesExceeded) {
            executeEmergencyTeleportRescue(parrot, targetLoc, data, cfg, isToRecipient, isPerch);
            return true;
        }

        return false;
    }

    private Vector findBestEscapeVector(Location cur) {
        World world = cur.getWorld();
        if (world == null) return new Vector(0, 0.2, 0);

        Vector[] directions = new Vector[]{
                new Vector(0, 1, 0),
                new Vector(1, 0.5, 0),
                new Vector(-1, 0.5, 0),
                new Vector(0, 0.5, 1),
                new Vector(0, 0.5, -1),
                new Vector(1, 0, 1),
                new Vector(-1, 0, -1),
                new Vector(0, -0.5, 0)
        };

        for (Vector dir : directions) {
            Location test = cur.clone().add(dir.clone().multiply(2.0));
            if (!Pathfinder3D.isSolidObstacle(test.getBlock()) &&
                !Pathfinder3D.isSolidObstacle(test.clone().add(0, 1, 0).getBlock())) {
                return dir.normalize();
            }
        }
        return new Vector(0, 0.25, 0);
    }

    private void executeEmergencyTeleportRescue(Parrot parrot, Location targetLoc, CourierData data, ConfigManager cfg, boolean isToRecipient, boolean isPerch) {
        pathCache.remove(data.getCourierUuid());
        data.setStuckTicks(0);
        data.setRetryCount(0);

        Location safeLand = targetLoc.clone();
        if (safeLand.getBlock().getType().isSolid()) {
            safeLand.add(0, 1.0, 0);
        }

        parrot.getWorld().spawnParticle(Particle.PORTAL, parrot.getLocation().add(0, 0.5, 0), 25, 0.4, 0.4, 0.4, 0.1);
        parrot.teleport(safeLand);
        parrot.getWorld().spawnParticle(Particle.PORTAL, safeLand.add(0, 0.5, 0), 25, 0.4, 0.4, 0.4, 0.1);
        parrot.getWorld().playSound(safeLand, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        parrot.setVelocity(new Vector(0, 0.05, 0));
        parrot.setSitting(false);

        if (isToRecipient) {
            data.setState(CourierState.WAITING_FOR_RECIPIENT);
            data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            plugin.getCourierManager().saveAll();

            if (cfg.isShowNametags()) {
                parrot.customName(TextUtil.parse("<green><b>[Delivery Ready]</b></green> <yellow>Sneak to trade</yellow>"));
                parrot.setCustomNameVisible(true);
            }

            if (data.getTargetType() == TargetType.PLAYER) {
                Player recipient = Bukkit.getPlayerExact(data.getTargetPlayerName());
                if (recipient != null && recipient.isOnline()) {
                    TextUtil.sendMessage(recipient, "<gold><b>[Courier]</b> A courier parrot bypassed complex terrain and arrived safely with your package from <yellow>" + data.getOwnerName() + "</yellow>!</gold>");
                }
            }
        } else {
            data.setState(CourierState.WAITING_FOR_OWNER);
            data.saveToPdc(parrot.getPersistentDataContainer(), plugin);
            plugin.getCourierManager().saveAll();

            if (cfg.isShowNametags()) {
                parrot.customName(TextUtil.parse("<green><b>[Courier Returned]</b></green> <yellow>Sneak to claim</yellow>"));
                parrot.setCustomNameVisible(true);
            }

            Player owner = (data.getOwnerUuid() != null) ? Bukkit.getPlayer(data.getOwnerUuid()) : null;
            if (owner != null && owner.isOnline()) {
                String landType = isPerch ? "Delivery Perch" : "location";
                TextUtil.sendMessage(owner, "<green>✔ Your courier bypassed complex terrain and arrived at your " + landType + "! Sneak near it to claim your items.</green>");
            }
        }
    }

    private void flyTowardsBaritone(Parrot parrot, Location cur, Location targetLoc, CourierData data, ConfigManager cfg) {
        parrot.setSitting(false);
        parrot.setTarget(null);
        parrot.getPathfinder().stopPathfinding();

        World world = cur.getWorld();
        boolean isNether = (world != null && world.getEnvironment() == World.Environment.NETHER);
        boolean isEnd = (world != null && world.getEnvironment() == World.Environment.THE_END);

        double baseSpeed = cfg.getFlightSpeed();
        double speed = data.isSpeedBoost() ? baseSpeed * cfg.getSpeedBoostMultiplier() : baseSpeed;
        double horizontalDist = Math.sqrt(Math.pow(targetLoc.getX() - cur.getX(), 2) + Math.pow(targetLoc.getZ() - cur.getZ(), 2));

        // Void floor safety
        if (world != null && cur.getY() < world.getMinHeight() + 15) {
            parrot.setVelocity(new Vector(0, 0.40, 0));
            return;
        }

        // 1. Long-Range Hierarchical Waypoint Management (10,000 node search depth)
        Vector targetWaypoint = targetLoc.toVector();
        boolean isDirectClear = Pathfinder3D.isLineOfSightClear(world, cur, targetLoc);

        if (!isDirectClear) {
            CachedPath cached = pathCache.get(data.getCourierUuid());
            if (cached == null || !cached.isValid(targetLoc)) {
                List<Vector> waypoints = Pathfinder3D.findLongRangePath(world, cur, targetLoc);
                cached = new CachedPath(waypoints, targetLoc);
                pathCache.put(data.getCourierUuid(), cached);
            }
            Vector nextWp = cached.getNextWaypoint(cur);
            if (nextWp != null) {
                targetWaypoint = nextWp;
            }
        } else {
            pathCache.remove(data.getCourierUuid());
        }

        // 2. Compute Target Attraction Vector
        Vector toWaypoint = targetWaypoint.clone().subtract(cur.toVector());
        Vector desiredVelocity;

        if (isNether) {
            double localFloorY = getNetherFloorY(cur);
            double localCeilingY = getNetherCeilingY(cur);
            double safeMinY = Math.max(32.0, localFloorY + 2.0);
            double safeMaxY = Math.min(120.0, localCeilingY - 2.0);

            double targetY = Math.max(safeMinY, Math.min(safeMaxY, targetWaypoint.getY()));
            double yDelta = targetY - cur.getY();
            double yVel = Math.max(-0.16, Math.min(0.18, yDelta * 0.08));

            Vector horiz = new Vector(toWaypoint.getX(), 0, toWaypoint.getZ()).normalize().multiply(speed);
            desiredVelocity = new Vector(horiz.getX(), yVel, horiz.getZ());
        } else {
            double localGround = isEnd ? Math.max(50.0, getLocalGroundY(cur)) : getLocalGroundY(cur);
            double clearance = cfg.getTerrainClearance();
            double cruiseAlt = cfg.getCruiseAltitude();
            double targetY = Math.max(localGround + clearance, Math.max(cruiseAlt, targetWaypoint.getY() + clearance));

            if (horizontalDist > 14.0) {
                double yDelta = targetY - cur.getY();
                double yVel = Math.max(-0.20, Math.min(0.24, yDelta * 0.08));
                Vector horiz = new Vector(toWaypoint.getX(), 0, toWaypoint.getZ()).normalize().multiply(speed);
                desiredVelocity = new Vector(horiz.getX(), yVel, horiz.getZ());
            } else {
                double approachSpeed = speed * cfg.getApproachSpeedFactor();
                double glideProgress = Math.min(1.0, horizontalDist / 14.0);
                double currentSpeed = Math.max(0.12, approachSpeed * glideProgress);
                desiredVelocity = toWaypoint.clone().normalize().multiply(currentSpeed);
            }
        }

        // 3. Real-Time 3D Sensory Obstacle Repulsion (Hemisphere probe field)
        Vector repulsionForce = computeSensoryRepulsion(world, cur, desiredVelocity);
        Vector blendedVelocity = desiredVelocity.clone().add(repulsionForce);
        if (blendedVelocity.lengthSquared() > 0.001) {
            blendedVelocity = blendedVelocity.normalize().multiply(speed);
        } else {
            blendedVelocity = desiredVelocity;
        }

        // 4. Collision Slide Guard
        Vector finalVelocity = applyCollisionSlideGuard(world, cur, blendedVelocity);

        parrot.setVelocity(finalVelocity);

        // 5. Heading & Pitch Alignment
        if (finalVelocity.lengthSquared() > 0.005) {
            float yaw = (float) Math.toDegrees(Math.atan2(-finalVelocity.getX(), finalVelocity.getZ()));
            float pitch = (float) Math.toDegrees(-Math.atan2(finalVelocity.getY(), Math.sqrt(finalVelocity.getX() * finalVelocity.getX() + finalVelocity.getZ() * finalVelocity.getZ())));
            parrot.setRotation(yaw, pitch);
        }

        // 6. Visual Particles
        if (cfg.getTrailParticleCount() > 0) {
            Particle trail = data.isGlowing() ? Particle.WAX_OFF : cfg.getTrailParticle();
            parrot.getWorld().spawnParticle(trail, parrot.getLocation().add(0, 0.2, 0), cfg.getTrailParticleCount(), 0.05, 0.05, 0.05, 0.01);
            if (data.isSpeedBoost() && tickCounter % 4 == 0) {
                parrot.getWorld().spawnParticle(Particle.CLOUD, parrot.getLocation().add(0, 0.1, 0), 1, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    private Vector computeSensoryRepulsion(World world, Location cur, Vector heading) {
        if (world == null || heading.lengthSquared() < 0.001) return new Vector(0, 0, 0);

        Vector forward = heading.clone().normalize();
        Vector up = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(up).normalize();
        if (right.lengthSquared() < 0.001) {
            right = new Vector(1, 0, 0);
        }
        Vector realUp = right.clone().crossProduct(forward).normalize();

        Vector totalRepulsion = new Vector(0, 0, 0);

        Vector[] probeRays = new Vector[]{
                forward.clone(),
                forward.clone().add(right.clone().multiply(0.5)).normalize(),
                forward.clone().subtract(right.clone().multiply(0.5)).normalize(),
                forward.clone().add(realUp.clone().multiply(0.5)).normalize(),
                forward.clone().subtract(realUp.clone().multiply(0.5)).normalize(),
                forward.clone().add(right.clone().multiply(1.0)).normalize(),
                forward.clone().subtract(right.clone().multiply(1.0)).normalize(),
                forward.clone().add(realUp.clone().multiply(1.0)).normalize(),
                forward.clone().subtract(realUp.clone().multiply(1.0)).normalize(),
                right.clone(),
                right.clone().multiply(-1),
                realUp.clone(),
                realUp.clone().multiply(-1),
                forward.clone().add(right.clone().multiply(0.5)).add(realUp.clone().multiply(0.5)).normalize()
        };

        for (Vector ray : probeRays) {
            for (double d = 0.8; d <= 4.0; d += 0.8) {
                Location probeLoc = cur.clone().add(ray.clone().multiply(d));
                Block b = probeLoc.getBlock();
                if (Pathfinder3D.isSolidObstacle(b)) {
                    double strength = (4.0 - d) / (d * d * 1.5);
                    totalRepulsion.add(ray.clone().multiply(-strength));
                    break;
                }
            }
        }

        return totalRepulsion;
    }

    private Vector applyCollisionSlideGuard(World world, Location cur, Vector vel) {
        if (world == null || vel.lengthSquared() < 0.001) return vel;

        Location next = cur.clone().add(vel);
        if (!Pathfinder3D.isSolidObstacle(next.getBlock()) &&
            !Pathfinder3D.isSolidObstacle(next.clone().add(0, 0.5, 0).getBlock())) {
            return vel;
        }

        Vector testX = new Vector(vel.getX(), 0, 0);
        Vector testY = new Vector(0, vel.getY(), 0);
        Vector testZ = new Vector(0, 0, vel.getZ());

        boolean xClear = !Pathfinder3D.isSolidObstacle(cur.clone().add(testX.clone().multiply(1.5)).getBlock());
        boolean yClear = !Pathfinder3D.isSolidObstacle(cur.clone().add(testY.clone().multiply(1.5)).getBlock());
        boolean zClear = !Pathfinder3D.isSolidObstacle(cur.clone().add(testZ.clone().multiply(1.5)).getBlock());

        Vector slide = new Vector(
                xClear ? vel.getX() : 0,
                yClear ? vel.getY() : 0,
                zClear ? vel.getZ() : 0
        );

        if (slide.lengthSquared() < 0.005) {
            return new Vector(0, 0.18, 0);
        }

        return slide.normalize().multiply(vel.length());
    }

    private double getLocalGroundY(Location cur) {
        World world = cur.getWorld();
        if (world == null) return cur.getY();
        return world.getHighestBlockYAt(cur.getBlockX(), cur.getBlockZ());
    }

    private double getNetherFloorY(Location cur) {
        World world = cur.getWorld();
        if (world == null) return 32.0;
        int x = cur.getBlockX();
        int z = cur.getBlockZ();
        for (int y = Math.min(115, cur.getBlockY()); y >= 20; y--) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid() || b.isLiquid()) {
                return y + 1.0;
            }
        }
        return 32.0;
    }

    private double getNetherCeilingY(Location cur) {
        World world = cur.getWorld();
        if (world == null) return 120.0;
        int x = cur.getBlockX();
        int z = cur.getBlockZ();
        for (int y = Math.max(35, cur.getBlockY()); y <= 125; y++) {
            Block b = world.getBlockAt(x, y, z);
            if (b.getType().isSolid()) {
                return y - 1.0;
            }
        }
        return 120.0;
    }

    private void handleDimensionalAscentAndWarp(Parrot parrot, CourierData data, World destWorld, Location destTarget, ConfigManager cfg) {
        if (data.getTransitStartTime() == 0) {
            data.setTransitStartTime(System.currentTimeMillis());
        }

        long elapsed = System.currentTimeMillis() - data.getTransitStartTime();
        if (elapsed < 2200) {
            parrot.setVelocity(new Vector(0.10, 0.26, 0.10));
            if (cfg.isShowNametags()) {
                parrot.customName(TextUtil.parse("<gray>Ascending for inter-dimensional warp...</gray>"));
            }
            if (tickCounter % 3 == 0) {
                parrot.getWorld().spawnParticle(Particle.PORTAL, parrot.getLocation().add(0, 0.3, 0), 4, 0.15, 0.15, 0.15, 0.05);
            }
        } else {
            warpToDimension(parrot, destWorld, destTarget);
            data.setTransitStartTime(System.currentTimeMillis());
            data.setMaxTransitTimeoutMs(0);
        }
    }

    private void warpToDimension(Parrot parrot, World destWorld, Location destTarget) {
        parrot.getWorld().spawnParticle(Particle.PORTAL, parrot.getLocation().add(0, 0.5, 0), 25, 0.5, 0.5, 0.5, 0.1);
        parrot.getWorld().playSound(parrot.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);

        Location warpLoc;
        if (destWorld.getEnvironment() == World.Environment.NETHER) {
            double safeY = destTarget.getY() + 3.0;
            for (int y = destTarget.getBlockY() + 1; y <= Math.min(120, destTarget.getBlockY() + 6); y++) {
                if (!destWorld.getBlockAt(destTarget.getBlockX(), y, destTarget.getBlockZ()).getType().isSolid() &&
                    !destWorld.getBlockAt(destTarget.getBlockX(), y + 1, destTarget.getBlockZ()).getType().isSolid()) {
                    safeY = y;
                    break;
                }
            }
            warpLoc = new Location(destWorld, destTarget.getX(), safeY, destTarget.getZ());
        } else {
            double targetY = Math.min(destWorld.getMaxHeight() - 10, Math.max(85.0, destTarget.getY() + 15.0));
            warpLoc = new Location(destWorld, destTarget.getX(), targetY, destTarget.getZ());
        }

        parrot.teleport(warpLoc);

        Vector toDest = destTarget.toVector().subtract(warpLoc.toVector());
        if (toDest.lengthSquared() > 0.005) {
            float yaw = (float) Math.toDegrees(Math.atan2(-toDest.getX(), toDest.getZ()));
            float pitch = (float) Math.toDegrees(-Math.atan2(toDest.getY(), Math.sqrt(toDest.getX() * toDest.getX() + toDest.getZ() * toDest.getZ())));
            parrot.setRotation(yaw, pitch);
            parrot.setVelocity(toDest.clone().normalize().multiply(0.24));
        }

        destWorld.spawnParticle(Particle.REVERSE_PORTAL, warpLoc.add(0, 0.5, 0), 25, 0.5, 0.5, 0.5, 0.1);
        destWorld.playSound(warpLoc, Sound.BLOCK_PORTAL_TRAVEL, 0.8f, 1.5f);
    }

    private void hoverInPlace(Parrot parrot) {
        parrot.setSitting(false);
        parrot.setTarget(null);
        double hoverY = Math.sin(tickCounter * 0.10) * 0.015;
        Vector vel = parrot.getVelocity();
        parrot.setVelocity(new Vector(vel.getX() * 0.3, hoverY, vel.getZ() * 0.3));
    }

    private void doWaitingAnimation(Parrot parrot) {
        float bobYaw = (float) (parrot.getYaw() + Math.sin(tickCounter * 0.25) * 12.0);
        float bobPitch = (float) (Math.cos(tickCounter * 0.20) * 8.0);
        parrot.setRotation(bobYaw, bobPitch);
        if (tickCounter % 20 == 0) {
            parrot.getWorld().spawnParticle(Particle.NOTE, parrot.getLocation().add(0, 0.6, 0), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
