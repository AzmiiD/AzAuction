package io.github.AzmiiD.azAuction.commands;

import io.github.AzmiiD.azAuction.AzAuction;
import io.github.AzmiiD.azAuction.gui.AuctionHouseGUI;
import io.github.AzmiiD.azAuction.models.AuctionItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionHouseCommand implements CommandExecutor {

    private final AzAuction plugin;

    public AuctionHouseCommand(AzAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("auctionhouse.use")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            // Open auction house GUI
            new AuctionHouseGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "sell":
                handleSellCommand(player, args);
                break;
            case "buy":
                handleBuyCommand(player, args);
                break;
            case "list":
                handleListCommand(player);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            case "web":
                handleWebCommand(player);
                break;
            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void handleSellCommand(Player player, String[] args) {
        if (!player.hasPermission("auctionhouse.sell")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /ah sell <price>");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            plugin.getMessageManager().sendMessage(player, "no-item-in-hand");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid price! Please enter a valid number.");
            return;
        }

        if (price <= 0) {
            player.sendMessage("§cPrice must be greater than 0!");
            return;
        }

        if (plugin.getAuctionManager().listItem(player, item, price)) {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void handleBuyCommand(Player player, String[] args) {
        if (!player.hasPermission("auctionhouse.buy")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /ah buy <id>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid ID! Please enter a valid number.");
            return;
        }

        plugin.getAuctionManager().buyItem(player, id);
    }

    private void handleListCommand(Player player) {
        List<AuctionItem> auctions = plugin.getAuctionManager().getPlayerAuctions(player.getUniqueId());

        if (auctions.isEmpty()) {
            player.sendMessage("§eYou have no active auctions.");
            return;
        }

        player.sendMessage("§6§lYour Active Auctions:");
        for (AuctionItem auction : auctions) {
            String itemName = auction.getItem().hasItemMeta() && auction.getItem().getItemMeta().hasDisplayName()
                    ? auction.getItem().getItemMeta().getDisplayName()
                    : auction.getItem().getType().name().toLowerCase().replace('_', ' ');

            player.sendMessage("§7ID: §e" + auction.getId() + " §7| §f" + itemName + " §7| §a$" + auction.getPrice());
        }
    }

    private void handleRemoveCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /ah remove <id>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid ID! Please enter a valid number.");
            return;
        }

        if (!plugin.getAuctionManager().removePlayerAuction(player, id)) {
            player.sendMessage("§cAuction not found or you don't own this auction!");
        }
    }

    private void handleWebCommand(Player player) {
        if (!player.hasPermission("auctionhouse.web")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return;
        }

        if (plugin.getWebServer() != null && plugin.getWebServer().isRunning()) {
            int port = plugin.getConfigManager().getWebPort();
            plugin.getMessageManager().sendMessage(player, "web-url", "port", String.valueOf(port));
        } else {
            player.sendMessage("§cWeb interface is not enabled!");
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("§6§lAuction House Commands:");
        player.sendMessage("§e/ah §7- Open auction house GUI");
        player.sendMessage("§e/ah sell <price> §7- Sell item in hand");
        player.sendMessage("§e/ah buy <id> §7- Buy an item");
        player.sendMessage("§e/ah list §7- List your auctions");
        player.sendMessage("§e/ah remove <id> §7- Remove your auction");
        player.sendMessage("§e/ah web §7- Get web interface URL");
    }
}
