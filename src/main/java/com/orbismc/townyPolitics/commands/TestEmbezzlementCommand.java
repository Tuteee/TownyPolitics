package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestEmbezzlementCommand implements CommandExecutor {

    private final TownyPolitics plugin;
    private final TownyAPI townyAPI;

    public TestEmbezzlementCommand(TownyPolitics plugin) {
        this.plugin = plugin;
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!sender.hasPermission("townypolitics.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /taxtest <town> <amount>");
            return true;
        }

        // Get the town
        String townName = args[0];
        Town town = townyAPI.getTown(townName);
        if (town == null) {
            player.sendMessage(ChatColor.RED + "Town not found: " + townName);
            return true;
        }

        // Check if town has a nation
        if (!town.hasNation()) {
            player.sendMessage(ChatColor.RED + "Town is not part of a nation.");
            return true;
        }

        // Get the amount
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }

        // Get the nation
        Nation nation;
        try {
            nation = town.getNation();
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error getting nation: " + e.getMessage());
            return true;
        }

        // Log the test
        plugin.getLogger().info("TAXTEST: Simulating tax payment of " + amount +
                " from " + town.getName() + " to " + nation.getName());

        // Withdraw from town account
        if (!town.getAccount().withdraw(amount, "Test tax payment to " + nation.getName())) {
            player.sendMessage(ChatColor.RED + "Town doesn't have enough money.");
            return true;
        }

        // Deposit to nation account
        nation.getAccount().deposit(amount, "Test tax payment from " + town.getName());

        // Display corruption info
        double corruption = plugin.getCorruptionManager().getCorruption(nation);
        player.sendMessage(ChatColor.GREEN + "Test tax payment of " + amount +
                " sent from " + town.getName() + " to " + nation.getName());
        player.sendMessage(ChatColor.YELLOW + "Nation corruption: " +
                String.format("%.1f%%", corruption));
        player.sendMessage(ChatColor.YELLOW + "Check console for embezzlement results.");

        return true;
    }
}