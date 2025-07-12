package io.github.AzmiiD.azAuction;

import io.github.AzmiiD.azAuction.commands.AuctionHouseCommand;
import io.github.AzmiiD.azAuction.commands.AuctionHouseTabCompleter;
import io.github.AzmiiD.azAuction.commands.ReloadCommand;
import io.github.AzmiiD.azAuction.database.DatabaseManager;
import io.github.AzmiiD.azAuction.economy.EconomyManager;
import io.github.AzmiiD.azAuction.listeners.PlayerListener;
import io.github.AzmiiD.azAuction.managers.AuctionManager;
import io.github.AzmiiD.azAuction.managers.ConfigManager;
import io.github.AzmiiD.azAuction.managers.MessageManager;
import io.github.AzmiiD.azAuction.web.WebServer;
import org.bukkit.plugin.java.JavaPlugin;

public final class AzAuction extends JavaPlugin {

    private static AzAuction instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private AuctionManager auctionManager;
    private WebServer webServer;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.economyManager = new EconomyManager(this);
        this.auctionManager = new AuctionManager(this);

        // Setup economy
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Vault not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize database
        if (!databaseManager.initialize()) {
            getLogger().severe("Failed to initialize database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        getCommand("auctionhouse").setExecutor(new AuctionHouseCommand(this));
        getCommand("auctionhouse").setTabCompleter(new AuctionHouseTabCompleter());
        getCommand("ahreload").setExecutor(new ReloadCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Start web server if enabled
        if (configManager.getConfig().getBoolean("auction.web-enabled", true)) {
            this.webServer = new WebServer(this);
            webServer.start();
        }

        // Load auctions
        auctionManager.loadAuctions();

        getLogger().info("SimpleAuctionHouse has been enabled!");

        // Display web URL if enabled
        if (webServer != null && webServer.isRunning()) {
            int port = configManager.getConfig().getInt("auction.web-port", 8080);
            getLogger().info("Web interface available at: http://localhost:" + port);
        }
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("SimpleAuctionHouse has been disabled!");
    }

    public void reload() {
        configManager.reload();
        messageManager.reload();

        // Restart web server if needed
        if (webServer != null) {
            webServer.stop();
        }

        if (configManager.getConfig().getBoolean("auction.web-enabled", true)) {
            this.webServer = new WebServer(this);
            webServer.start();
        }

        auctionManager.loadAuctions();
    }

    public static AzAuction getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}