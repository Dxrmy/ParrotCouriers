package com.parrotcouriers.config;

import com.parrotcouriers.ParrotCouriersPlugin;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages plugin configuration values, particles, sounds, and flight settings.
 */
public class ConfigManager {

    private final ParrotCouriersPlugin plugin;

    private String namePrefix;
    private boolean requirePrefix;
    private boolean freezeDuringSetup;

    private double flightSpeed;
    private double terrainClearance;
    private double cruiseAltitude;
    private double approachSpeedFactor;
    private double speedBoostMultiplier;
    private int stuckRetryTicks;
    private int maxStuckRetries;
    private int timeoutBaseSeconds;
    private int timeoutPer100BlocksSeconds;

    private Particle trailParticle;
    private int trailParticleCount;
    private Particle hoverParticle;
    private int hoverParticleCount;
    private Particle lockParticle;
    private int lockParticleCount;

    private double interactionRadius;
    private int actionBarIntervalTicks;
    private boolean allowFreeDeliveries;
    private boolean showEtaActionBar;
    private boolean prioritizePerchesDefault;
    private boolean interdimensionalEnabled;
    private boolean requireChorusFruit;

    private boolean soundsEnabled;
    private Sound chimeSound;
    private Sound bellSound;
    private Sound completeSound;
    private Sound ambientSound;
    private Sound lockSound;

    private boolean showPrefix;
    private String chatPrefix;

    private boolean dancingWhileWaiting;
    private boolean stateParticles;
    private boolean showNametags;

    public ConfigManager(ParrotCouriersPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Targeting
        this.namePrefix = cfg.getString("targeting.prefix", "@");
        this.requirePrefix = cfg.getBoolean("targeting.require-prefix", true);
        this.freezeDuringSetup = cfg.getBoolean("targeting.freeze-during-setup", true);

        // Flight (Default slower, relaxed speed: 0.24 blocks/tick ~ 4.8 blocks/sec)
        this.flightSpeed = cfg.getDouble("flight.speed", 0.24);
        this.terrainClearance = cfg.getDouble("flight.terrain-clearance", 6.0);
        this.cruiseAltitude = cfg.getDouble("flight.cruise-altitude", 85.0);
        this.approachSpeedFactor = cfg.getDouble("flight.approach-speed-factor", 0.55);
        this.speedBoostMultiplier = cfg.getDouble("flight.speed-boost-multiplier", 1.85);

        // Stuck Detection & Emergency Teleport Timeout
        this.stuckRetryTicks = cfg.getInt("flight.stuck-retry-ticks", 50);
        this.maxStuckRetries = cfg.getInt("flight.max-stuck-retries", 3);
        this.timeoutBaseSeconds = cfg.getInt("flight.timeout-base-seconds", 40);
        this.timeoutPer100BlocksSeconds = cfg.getInt("flight.timeout-per-100-blocks-seconds", 12);

        // Particles
        this.trailParticle = parseParticle(cfg.getString("flight.trail-particle", "WAX_OFF"), Particle.WAX_OFF);
        this.trailParticleCount = cfg.getInt("flight.trail-particle-count", 1);
        this.hoverParticle = parseParticle(cfg.getString("flight.hover-particle", "HAPPY_VILLAGER"), Particle.HAPPY_VILLAGER);
        this.hoverParticleCount = cfg.getInt("flight.hover-particle-count", 1);
        this.lockParticle = parseParticle(cfg.getString("flight.lock-particle", "HEART"), Particle.HEART);
        this.lockParticleCount = cfg.getInt("flight.lock-particle-count", 10);

        // Delivery & Perches
        this.interactionRadius = cfg.getDouble("delivery.interaction-radius", 3.5);
        this.actionBarIntervalTicks = cfg.getInt("delivery.actionbar-interval-ticks", 35);
        this.allowFreeDeliveries = cfg.getBoolean("delivery.allow-free-deliveries", true);
        this.showEtaActionBar = cfg.getBoolean("delivery.show-eta-actionbar", true);
        this.prioritizePerchesDefault = cfg.getBoolean("perches.prioritize-perches", true);
        this.interdimensionalEnabled = cfg.getBoolean("interdimensional.enabled", true);
        this.requireChorusFruit = cfg.getBoolean("interdimensional.require-chorus-fruit", true);

        // Sounds
        this.soundsEnabled = cfg.getBoolean("sounds.enabled", true);
        this.chimeSound = parseSound(cfg.getString("sounds.chime-sound", "BLOCK_NOTE_BLOCK_CHIME"), Sound.BLOCK_NOTE_BLOCK_CHIME);
        this.bellSound = parseSound(cfg.getString("sounds.bell-sound", "BLOCK_NOTE_BLOCK_BELL"), Sound.BLOCK_NOTE_BLOCK_BELL);
        this.completeSound = parseSound(cfg.getString("sounds.complete-sound", "ENTITY_PLAYER_LEVELUP"), Sound.ENTITY_PLAYER_LEVELUP);
        this.ambientSound = parseSound(cfg.getString("sounds.ambient-sound", "ENTITY_PARROT_AMBIENT"), Sound.ENTITY_PARROT_AMBIENT);
        this.lockSound = parseSound(cfg.getString("sounds.lock-sound", "ENTITY_PARROT_EAT"), Sound.ENTITY_PARROT_EAT);

        // Messages
        this.showPrefix = cfg.getBoolean("messages.show-prefix", true);
        this.chatPrefix = cfg.getString("messages.prefix", "<gold>[ParrotCouriers]</gold> ");

        // Visual Cues
        this.dancingWhileWaiting = cfg.getBoolean("visual-cues.dancing-while-waiting", false);
        this.stateParticles = cfg.getBoolean("visual-cues.state-particles", true);
        this.showNametags = cfg.getBoolean("visual-cues.show-nametags", true);
    }

    private Particle parseParticle(String name, Particle fallback) {
        if (name == null) return fallback;
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown particle in config: '" + name + "'. Using fallback " + fallback.name());
            return fallback;
        }
    }

    private Sound parseSound(String name, Sound fallback) {
        if (name == null) return fallback;
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown sound in config: '" + name + "'. Using fallback " + fallback.name());
            return fallback;
        }
    }

    public String getMessage(String path, String def) {
        return plugin.getConfig().getString("messages." + path, def);
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public boolean isRequirePrefix() {
        return requirePrefix;
    }

    public boolean isFreezeDuringSetup() {
        return freezeDuringSetup;
    }

    public double getFlightSpeed() {
        return flightSpeed;
    }

    public double getTerrainClearance() {
        return terrainClearance;
    }

    public double getCruiseAltitude() {
        return cruiseAltitude;
    }

    public double getApproachSpeedFactor() {
        return approachSpeedFactor;
    }

    public double getSpeedBoostMultiplier() {
        return speedBoostMultiplier;
    }

    public int getStuckRetryTicks() {
        return stuckRetryTicks;
    }

    public int getMaxStuckRetries() {
        return maxStuckRetries;
    }

    public int getTimeoutBaseSeconds() {
        return timeoutBaseSeconds;
    }

    public int getTimeoutPer100BlocksSeconds() {
        return timeoutPer100BlocksSeconds;
    }

    public Particle getTrailParticle() {
        return trailParticle;
    }

    public int getTrailParticleCount() {
        return trailParticleCount;
    }

    public Particle getHoverParticle() {
        return hoverParticle;
    }

    public int getHoverParticleCount() {
        return hoverParticleCount;
    }

    public Particle getLockParticle() {
        return lockParticle;
    }

    public int getLockParticleCount() {
        return lockParticleCount;
    }

    public double getInteractionRadius() {
        return interactionRadius;
    }

    public int getActionBarIntervalTicks() {
        return actionBarIntervalTicks;
    }

    public boolean isAllowFreeDeliveries() {
        return allowFreeDeliveries;
    }

    public boolean isShowEtaActionBar() {
        return showEtaActionBar;
    }

    public boolean isPrioritizePerchesDefault() {
        return prioritizePerchesDefault;
    }

    public boolean isInterdimensionalEnabled() {
        return interdimensionalEnabled;
    }

    public boolean isRequireChorusFruit() {
        return requireChorusFruit;
    }

    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }

    public Sound getChimeSound() {
        return chimeSound;
    }

    public Sound getBellSound() {
        return bellSound;
    }

    public Sound getCompleteSound() {
        return completeSound;
    }

    public Sound getAmbientSound() {
        return ambientSound;
    }

    public Sound getLockSound() {
        return lockSound;
    }

    public boolean isShowPrefix() {
        return showPrefix;
    }

    public String getChatPrefix() {
        return chatPrefix;
    }

    public boolean isDancingWhileWaiting() {
        return dancingWhileWaiting;
    }

    public boolean isStateParticles() {
        return stateParticles;
    }

    public boolean isShowNametags() {
        return showNametags;
    }
}
