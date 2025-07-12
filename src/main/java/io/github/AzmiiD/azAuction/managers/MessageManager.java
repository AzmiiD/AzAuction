package io.github.AzmiiD.azAuction.managers;

import io.github.AzmiiD.azAuction.AzAuction;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final AzAuction plugin;
    private FileConfiguration config;

    public MessageManager(AzAuction plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        this.config = plugin.getConfig();
    }

    public String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', config.getString("messages." + key, "&cMessage not found: " + key));
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(key));
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(key, placeholders));
    }

    public void sendMessage(Player player, String key, String placeholder, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(placeholder, value);
        sendMessage(player, key, placeholders);
    }

    public String getPrefix() {
        return getMessage("prefix");
    }
}
