package io.github.AzmiiD.azAuction.gui;

import io.github.AzmiiD.azAuction.AzAuction;
import io.github.AzmiiD.azAuction.models.AuctionItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI implements InventoryHolder, Listener {

    private final AzAuction plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<AuctionItem> auctions;
    private int page = 0;
    private final int itemsPerPage;

    public AuctionHouseGUI(AzAuction plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        this.auctions = plugin.getAuctionManager().getActiveAuctions();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getGuiTitle()));

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        updateInventory();
    }

    public void open() {
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, auctions.size());

        for (int i = startIndex; i < endIndex; i++) {
            AuctionItem auction = auctions.get(i);
            ItemStack displayItem = auction.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();

            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add("§7Seller: §e" + auction.getSellerName());
                lore.add("§7Price: §a$" + auction.getPrice());
                lore.add("§7ID: §e" + auction.getId());
                lore.add("");

                if (auction.getSellerId().equals(player.getUniqueId())) {
                    lore.add("§eClick to remove your listing!");
                } else {
                    lore.add("§eClick to buy this item!");
                }

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(i - startIndex, displayItem);
        }

        // Navigation items
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§ePrevious Page");
            prevPage.setItemMeta(prevMeta);
            inventory.setItem(45, prevPage);
        }

        if (endIndex < auctions.size()) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§eNext Page");
            nextPage.setItemMeta(nextMeta);
            inventory.setItem(53, nextPage);
        }

        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lAuction House Info");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Total Items: §e" + auctions.size());
        infoLore.add("§7Current Page: §e" + (page + 1));
        infoLore.add("§7Max Pages: §e" + ((auctions.size() - 1) / itemsPerPage + 1));
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inventory.setItem(49, info);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if the player is the one who opened this GUI
        if (!(event.getWhoClicked() instanceof Player) || !event.getWhoClicked().equals(player)) {
            return;
        }

        // Check if any of the inventories involved is our auction house inventory
        if (event.getInventory().getHolder() == this ||
                (event.getClickedInventory() != null && event.getClickedInventory().getHolder() == this)) {

            // Always cancel the event to prevent any item movement
            event.setCancelled(true);

            // Only process clicks if it's specifically in our auction house inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() == this) {
                handleAuctionClick(event);
            }
        }
    }

    private void handleAuctionClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // Navigation
        if (slot == 45 && page > 0) {
            page--;
            updateInventory();
            return;
        }

        if (slot == 53 && (page + 1) * itemsPerPage < auctions.size()) {
            page++;
            updateInventory();
            return;
        }

        // Info item
        if (slot == 49) {
            return;
        }

        // Auction item
        if (slot < itemsPerPage) {
            int auctionIndex = page * itemsPerPage + slot;
            if (auctionIndex < auctions.size()) {
                AuctionItem auction = auctions.get(auctionIndex);

                if (auction.getSellerId().equals(player.getUniqueId())) {
                    // Remove own auction
                    if (plugin.getAuctionManager().removePlayerAuction(player, auction.getId())) {
                        auctions.remove(auctionIndex);
                        updateInventory();
                    }
                } else {
                    // Buy auction
                    if (plugin.getAuctionManager().buyItem(player, auction.getId())) {
                        auctions.remove(auctionIndex);
                        updateInventory();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        // Check if the player is the one who opened this GUI
        if (!(event.getWhoClicked() instanceof Player) || !event.getWhoClicked().equals(player)) {
            return;
        }

        // Check if any of the slots being dragged to are in our auction house inventory
        if (event.getInventory().getHolder() == this) {
            for (int slot : event.getInventorySlots()) {
                if (slot < inventory.getSize()) {
                    // Cancel the event to prevent any item dragging
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this && event.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
