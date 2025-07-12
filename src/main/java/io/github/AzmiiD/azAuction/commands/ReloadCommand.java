package io.github.AzmiiD.azAuction.commands;

import io.github.AzmiiD.azAuction.AzAuction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final AzAuction plugin;

    public ReloadCommand(AzAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auctionhouse.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§eReloading SimpleAuctionHouse...");
        plugin.reload();
        sender.sendMessage("§aSimpleAuctionHouse has been reloaded!");

        return true;
    }
}
