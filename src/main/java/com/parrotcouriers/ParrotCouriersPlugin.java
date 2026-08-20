package com.parrotcouriers;

import com.parrotcouriers.command.CourierCommand;
import com.parrotcouriers.config.ConfigManager;
import com.parrotcouriers.flight.FlightEngine;
import com.parrotcouriers.gui.GuiManager;
import com.parrotcouriers.listener.CourierListener;
import com.parrotcouriers.manager.CourierManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ParrotCouriers.
 * Target: PaperMC Minecraft version 26.2.
 */
public class ParrotCouriersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private CourierManager courierManager;
    private FlightEngine flightEngine;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // Initialize Managers
        this.courierManager = new CourierManager(this);
        this.courierManager.loadAll();

        this.guiManager = new GuiManager(this);
        this.flightEngine = new FlightEngine(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new CourierListener(this), this);
        getServer().getPluginManager().registerEvents(this.guiManager, this);

        // Register Commands
        PluginCommand command = getCommand("courier");
        if (command != null) {
            CourierCommand courierCommand = new CourierCommand(this);
            command.setExecutor(courierCommand);
            command.setTabCompleter(courierCommand);
        }

        // Start Flight Engine task (runs every tick)
        this.flightEngine.runTaskTimer(this, 1L, 1L);

        getLogger().info("ParrotCouriers v" + getPluginMeta().getVersion() + " enabled on Paper 26.2!");
    }

    public void reloadPlugin() {
        reloadConfig();
        if (this.configManager != null) {
            this.configManager.load();
        }
        if (this.courierManager != null) {
            this.courierManager.loadAll();
        }
    }

    @Override
    public void onDisable() {
        if (this.flightEngine != null) {
            this.flightEngine.cancel();
        }
        if (this.courierManager != null) {
            this.courierManager.saveAll();
        }
        getLogger().info("ParrotCouriers disabled and data saved safely.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CourierManager getCourierManager() {
        return courierManager;
    }

    public FlightEngine getFlightEngine() {
        return flightEngine;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }
}
