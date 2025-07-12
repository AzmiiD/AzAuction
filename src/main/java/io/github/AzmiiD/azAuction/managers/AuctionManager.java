package io.github.AzmiiD.azAuction.managers;

import io.github.AzmiiD.azAuction.AzAuction;
import io.github.AzmiiD.azAuction.models.AuctionItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {

    private final AzAuction plugin;
    private final Map<Integer, AuctionItem> activeAuctions;
    private final Map<UUID, List<Integer>> playerAuctions;

    public AuctionManager(AzAuction plugin) {
        this.plugin = plugin;
        this.activeAuctions = new ConcurrentHashMap<>();
        this.playerAuctions = new ConcurrentHashMap<>();

        // Start expiration check task
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpiredAuctions, 20L * 60L, 20L * 60L); // Check every minute
    }

    public void loadAuctions() {
        activeAuctions.clear();
        playerAuctions.clear();

        List<AuctionItem> auctions = plugin.getDatabaseManager().getAllAuctions();
        for (AuctionItem auction : auctions) {
            if (!auction.isExpired()) {
                activeAuctions.put(auction.getId(), auction);
                playerAuctions.computeIfAbsent(auction.getSellerId(), k -> new ArrayList<>()).add(auction.getId());
            } else {
                // Remove expired auctions
                plugin.getDatabaseManager().removeAuction(auction.getId());
                returnItemToPlayer(auction);
            }
        }

        plugin.getLogger().info("Loaded " + activeAuctions.size() + " active auctions.");
    }

    public boolean listItem(Player player, ItemStack item, double price) {
        ConfigManager config = plugin.getConfigManager();

        // Check if player has reached max listings
        int playerListings = getPlayerListingCount(player.getUniqueId());
        if (playerListings >= config.getMaxListingsPerPlayer()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("max", String.valueOf(config.getMaxListingsPerPlayer()));
            plugin.getMessageManager().sendMessage(player, "max-listings-reached", placeholders);
            return false;
        }

        // Check price limits
        if (price < config.getMinPrice() || price > config.getMaxPrice()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min", plugin.getEconomyManager().format(config.getMinPrice()));
            placeholders.put("max", plugin.getEconomyManager().format(config.getMaxPrice()));
            plugin.getMessageManager().sendMessage(player, "invalid-price", placeholders);
            return false;
        }

        // Create auction item
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime + (config.getListingDuration() * 60 * 60 * 1000L); // Convert hours to milliseconds

        AuctionItem auction = new AuctionItem(
                player.getUniqueId(),
                player.getName(),
                item.clone(),
                price,
                currentTime,
                expireTime
        );

        // Save to database
        if (plugin.getDatabaseManager().insertAuction(auction)) {
            activeAuctions.put(auction.getId(), auction);
            playerAuctions.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(auction.getId());

            plugin.getMessageManager().sendMessage(player, "item-listed", "price", plugin.getEconomyManager().format(price));
            return true;
        } else {
            plugin.getMessageManager().sendMessage(player, "database-error");
            return false;
        }
    }

    public boolean buyItem(Player buyer, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction == null) {
            return false;
        }

        // Check if buyer has enough money
        if (!plugin.getEconomyManager().hasEnough(buyer, auction.getPrice())) {
            plugin.getMessageManager().sendMessage(buyer, "not-enough-money");
            return false;
        }

        // Process transaction
        if (!plugin.getEconomyManager().withdraw(buyer, auction.getPrice())) {
            plugin.getMessageManager().sendMessage(buyer, "not-enough-money");
            return false;
        }

        // Calculate seller amount (after tax)
        double taxRate = plugin.getConfigManager().getTaxRate();
        double sellerAmount = auction.getPrice() * (1.0 - taxRate);

        // Give money to seller
        Player seller = Bukkit.getPlayer(auction.getSellerId());
        if (seller != null) {
            plugin.getEconomyManager().deposit(seller, sellerAmount);

            // Notify seller
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", getItemDisplayName(auction.getItem()));
            placeholders.put("price", plugin.getEconomyManager().format(auction.getPrice()));
            plugin.getMessageManager().sendMessage(seller, "item-sold", placeholders);
        } else {
            // TODO: Handle offline player payment (could use a pending payments system)
        }

        // Give item to buyer
        HashMap<Integer, ItemStack> remaining = buyer.getInventory().addItem(auction.getItem());
        if (!remaining.isEmpty()) {
            // Drop items if inventory is full
            for (ItemStack item : remaining.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), item);
            }
        }

        // Remove auction
        removeAuction(auctionId);

        // Notify buyer
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", getItemDisplayName(auction.getItem()));
        placeholders.put("price", plugin.getEconomyManager().format(auction.getPrice()));
        plugin.getMessageManager().sendMessage(buyer, "item-bought", placeholders);

        return true;
    }

    public boolean removeAuction(int auctionId) {
        AuctionItem auction = activeAuctions.remove(auctionId);
        if (auction != null) {
            // Remove from player's listings
            List<Integer> playerListings = playerAuctions.get(auction.getSellerId());
            if (playerListings != null) {
                playerListings.remove(Integer.valueOf(auctionId));
                if (playerListings.isEmpty()) {
                    playerAuctions.remove(auction.getSellerId());
                }
            }

            // Remove from database
            plugin.getDatabaseManager().removeAuction(auctionId);
            return true;
        }
        return false;
    }

    public boolean removePlayerAuction(Player player, int auctionId) {
        AuctionItem auction = activeAuctions.get(auctionId);
        if (auction != null && auction.getSellerId().equals(player.getUniqueId())) {
            // Return item to player
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(auction.getItem());
            if (!remaining.isEmpty()) {
                // Drop items if inventory is full
                for (ItemStack item : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }

            removeAuction(auctionId);
            plugin.getMessageManager().sendMessage(player, "item-removed");
            return true;
        }
        return false;
    }

    public List<AuctionItem> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public List<AuctionItem> getPlayerAuctions(UUID playerId) {
        List<Integer> playerListingIds = playerAuctions.get(playerId);
        if (playerListingIds == null) {
            return new ArrayList<>();
        }

        List<AuctionItem> playerListings = new ArrayList<>();
        for (Integer id : playerListingIds) {
            AuctionItem auction = activeAuctions.get(id);
            if (auction != null) {
                playerListings.add(auction);
            }
        }

        return playerListings;
    }

    public int getPlayerListingCount(UUID playerId) {
        List<Integer> playerListingIds = playerAuctions.get(playerId);
        return playerListingIds != null ? playerListingIds.size() : 0;
    }

    public AuctionItem getAuction(int id) {
        return activeAuctions.get(id);
    }

    private void checkExpiredAuctions() {
        List<Integer> expiredIds = new ArrayList<>();

        for (AuctionItem auction : activeAuctions.values()) {
            if (auction.isExpired()) {
                expiredIds.add(auction.getId());
            }
        }

        for (Integer id : expiredIds) {
            AuctionItem auction = activeAuctions.get(id);
            if (auction != null) {
                returnItemToPlayer(auction);
                removeAuction(id);
            }
        }
    }

    private void returnItemToPlayer(AuctionItem auction) {
        Player player = Bukkit.getPlayer(auction.getSellerId());
        if (player != null) {
            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(auction.getItem());
            if (!remaining.isEmpty()) {
                // Drop items if inventory is full
                for (ItemStack item : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("item", getItemDisplayName(auction.getItem()));
            plugin.getMessageManager().sendMessage(player, "listing-expired", placeholders);
        }
        // TODO: Handle offline player item return (could use a pending items system)
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace('_', ' ');
    }
}
