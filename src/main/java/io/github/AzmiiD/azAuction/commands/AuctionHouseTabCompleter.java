package io.github.AzmiiD.azAuction.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AuctionHouseTabCompleter implements TabCompleter {

    private final List<String> subCommands = Arrays.asList("sell", "buy", "list", "remove", "web");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("buy")) {
            // You could suggest some auction IDs here if needed
            return Collections.singletonList("<id>");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            // You could suggest player’s own auction IDs here if needed
            return Collections.singletonList("<id>");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return Collections.singletonList("<price>");
        }

        return Collections.emptyList();
    }
}