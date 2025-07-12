package io.github.AzmiiD.azAuction.listeners;

import io.github.AzmiiD.azAuction.AzAuction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final AzAuction plugin;

    public PlayerListener(AzAuction plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Could add welcome message or notifications here
        // For now, we'll keep it simple
    }
}
