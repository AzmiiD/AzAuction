package io.github.AzmiiD.azAuction.managers;

import io.github.AzmiiD.azAuction.AzAuction;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final AzAuction plugin;
    private FileConfiguration config;

    public ConfigManager(AzAuction plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getMaxListingsPerPlayer() {
        return config.getInt("auction.max-listings-per-player", 10);
    }

    public double getMaxPrice() {
        return config.getDouble("auction.max-price", 1000000.0);
    }

    public double getMinPrice() {
        return config.getDouble("auction.min-price", 0.01);
    }

    public double getTaxRate() {
        return config.getDouble("auction.tax-rate", 0.05);
    }

    public int getListingDuration() {
        return config.getInt("auction.listing-duration", 168);
    }

    public int getItemsPerPage() {
        return config.getInt("gui.items-per-page", 45);
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "&6&lAuction House");
    }

    public int getUpdateInterval() {
        return config.getInt("gui.update-interval", 5);
    }

    public boolean isWebEnabled() {
        return config.getBoolean("auction.web-enabled", true);
    }

    public int getWebPort() {
        return config.getInt("auction.web-port", 8080);
    }

    public String getWebHost() {
        return config.getString("auction.web-host", "0.0.0.0");
    }
}
