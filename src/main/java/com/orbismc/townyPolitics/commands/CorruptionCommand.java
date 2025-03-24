package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CorruptionCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final PoliticalPowerManager ppManager;
    private final TownyAPI townyAPI;

    public CorruptionCommand(TownyPolitics plugin, CorruptionManager corruptionManager, PoliticalPowerManager ppManager) {
        this.plugin = plugin;
        this.corruptionManager = corruptionManager;
        this.ppManager = ppManager;
        this.townyAPI = TownyAPI.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is registered in Towny
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }

        // Check if player is in a nation
        Nation nation = resident.getNationOrNull();
        if (nation == null) {
            player.sendMessage(ChatColor.RED + "You are not part of a nation.");
            return true;
        }

        // If no args, show the player's nation corruption info
        if (args.length == 0) {
            return showCorruptionInfo(player, nation);
        }

        // Just show the corruption info - no reduce functionality yet
        switch (args[0].toLowerCase()) {
            case "info", "status" -> showCorruptionInfo(player, nation);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                showHelp(player);
            }
        }

        return true;
    }

    private boolean showCorruptionInfo(Player player, Nation nation) {
        double corruption = corruptionManager.getCorruption(nation);
        double dailyGain = corruptionManager.calculateDailyCorruptionGain(nation);
        boolean isCritical = corruptionManager.isCorruptionCritical(nation);

        // Get modifiers
        double taxMod = corruptionManager.getTaxationModifier(nation);
        double ppMod = corruptionManager.getPoliticalPowerModifier(nation);
        double resourceMod = corruptionManager.getResourceModifier(nation);
        double spendingMod = corruptionManager.getSpendingModifier(nation);

        // Format modifiers as percentages relative to 100%
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String ppModStr = String.format("%+.1f%%", (ppMod - 1.0) * 100);
        String resourceModStr = String.format("%+.1f%%", (resourceMod - 1.0) * 100);
        String spendingModStr = String.format("%+.1f%%", (spendingMod - 1.0) * 100);

        // Show information
        player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Corruption Level ===");

        // Color-code the corruption level based on severity
        ChatColor corruptColor;
        if (corruption >= 75) corruptColor = ChatColor.DARK_RED;
        else if (corruption >= 50) corruptColor = ChatColor.RED;
        else if (corruption >= 25) corruptColor = ChatColor.GOLD;
        else corruptColor = ChatColor.GREEN;

        player.sendMessage(ChatColor.YELLOW + "Current Corruption: " + corruptColor +
                String.format("%.1f%%", corruption));

        player.sendMessage(ChatColor.YELLOW + "Daily Change: " + ChatColor.RED +
                String.format("+%.2f%%", dailyGain));

        // Show current effects
        player.sendMessage(ChatColor.GOLD + "Current Effects:");
        player.sendMessage(ChatColor.YELLOW + "• Max Taxation: " +
                getColorForModifier(taxMod) + taxModStr);
        player.sendMessage(ChatColor.YELLOW + "• Political Power Gain: " +
                getColorForModifier(ppMod) + ppModStr);
        player.sendMessage(ChatColor.YELLOW + "• Resource Output: " +
                getColorForModifier(resourceMod) + resourceModStr);
        player.sendMessage(ChatColor.YELLOW + "• Spending Costs: " +
                getColorForModifier(spendingMod) + spendingModStr);

        // Show critical warning if applicable
        if (isCritical) {
            player.sendMessage(ChatColor.DARK_RED + "WARNING: Corruption has reached critical levels!");
            player.sendMessage(ChatColor.RED + "Your nation will lose 5% of its political power each day!");
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Corruption Command Help ===");
        player.sendMessage(ChatColor.YELLOW + "/nation corruption" + ChatColor.WHITE + " - Show corruption info");
        player.sendMessage(ChatColor.YELLOW + "/nation corruption info" + ChatColor.WHITE + " - Show corruption info");
    }

    private ChatColor getColorForModifier(double modifier) {
        if (modifier > 1.0) return ChatColor.RED;
        if (modifier < 1.0) return ChatColor.RED;
        return ChatColor.GREEN;
    }

    private boolean handleReduceCommand(Player player, Resident resident, Nation nation, String[] args) {
        // Check if player is leader or has permission
        if (!nation.isKing(resident)) {
            player.sendMessage(ChatColor.RED + "Only the nation leader can reduce corruption.");
            return true;
        }

        // Check for amount argument
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Please specify an amount to reduce.");
            player.sendMessage(ChatColor.YELLOW + "Usage: /nation corruption reduce <amount>");
            return true;
        }

        // Parse the amount
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }

        // Check minimum reduction amount
        double minReduction = plugin.getConfig().getDouble("corruption.min_reduction", 5.0);
        if (amount < minReduction) {
            player.sendMessage(ChatColor.RED + "Minimum reduction amount is " +
                    String.format("%.1f", minReduction) + "%.");
            return true;
        }

        // Check if there's enough corruption to reduce
        double currentCorruption = corruptionManager.getCorruption(nation);
        if (amount > currentCorruption) {
            player.sendMessage(ChatColor.RED + "You cannot reduce more corruption than you have.");
            player.sendMessage(ChatColor.YELLOW + "Current corruption: " +
                    String.format("%.1f", currentCorruption) + "%");
            return true;
        }

        // Calculate political power cost
        double ppCost = corruptionManager.calculatePPCostForCorruptionReduction(amount);
        double currentPP = ppManager.getPoliticalPower(nation);

        // Check if nation has enough political power
        if (currentPP < ppCost) {
            player.sendMessage(ChatColor.RED + "Not enough political power to reduce corruption.");
            player.sendMessage(ChatColor.YELLOW + "Required: " + String.format("%.1f", ppCost));
            player.sendMessage(ChatColor.YELLOW + "Available: " + String.format("%.1f", currentPP));
            return true;
        }

        // All checks passed, reduce corruption and deduct political power
        corruptionManager.reduceCorruption(nation, amount);
        ppManager.removePoliticalPower(nation, ppCost);

        player.sendMessage(ChatColor.GREEN + "Successfully reduced corruption by " +
                String.format("%.1f", amount) + "% at a cost of " +
                String.format("%.1f", ppCost) + " political power.");

        // Show new corruption level
        double newCorruption = corruptionManager.getCorruption(nation);
        player.sendMessage(ChatColor.YELLOW + "New corruption level: " +
                ChatColor.GREEN + String.format("%.1f", newCorruption) + "%");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("info", "status");
            return filterCompletions(subCommands, args[0]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}