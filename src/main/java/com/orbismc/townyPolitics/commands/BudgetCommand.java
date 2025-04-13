package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.BudgetCategory;
import com.orbismc.townyPolitics.budget.BudgetAllocation;
import com.orbismc.townyPolitics.managers.BudgetManager;
import com.orbismc.townyPolitics.commands.base.BaseCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BudgetCommand extends BaseCommand {

    private final BudgetManager budgetManager;
    private final String commandSource;

    public BudgetCommand(TownyPolitics plugin, BudgetManager budgetManager, String commandSource) {
        super(plugin, "BudgetCommand");
        this.budgetManager = budgetManager;
        this.commandSource = commandSource; // "town" or "nation"
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) {
            return true;
        }

        Player player = (Player) sender;
        Resident resident = getResident(player);
        if (resident == null) {
            return true;
        }

        if (args.length == 0) {
            showBudgetInfo(player, resident);
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "info":
                showBudgetInfo(player, resident);
                break;

            case "set":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " budget set <category> <percentage>");
                    return true;
                }
                handleSetBudget(player, resident, args[1], args[2]);
                break;

            case "cycle":
                showNextBudgetCycle(player, resident);
                break;

            case "help":
                showHelp(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                showHelp(player);
                break;
        }

        return true;
    }

    private void showBudgetInfo(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;

            // Check if player is mayor or assistant
            if (!town.isMayor(resident)) {
                player.sendMessage(ChatColor.RED + "Only the mayor can view town budget details.");
                return;
            }

            Map<BudgetCategory, BudgetAllocation> allocations = budgetManager.getTownBudgetAllocations(town);

            // Display budget allocations
            player.sendMessage(ChatColor.GOLD + "=== " + town.getName() + "'s Budget Allocations ===");

            for (BudgetCategory category : BudgetCategory.values()) {
                BudgetAllocation allocation = allocations.get(category);
                player.sendMessage(ChatColor.YELLOW + category.name() + ": " +
                        ChatColor.WHITE + String.format("%.1f%%", allocation.getPercentage()));
            }

            // Show information about next budget cycle
            int daysUntil = budgetManager.getDaysUntilNextCycle(town.getUUID(), false);
            String timeUntil = budgetManager.getFormattedTimeUntilNextCycle(town.getUUID(), false);
            player.sendMessage(ChatColor.YELLOW + "Next budget cycle: " + ChatColor.WHITE + timeUntil);

        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;

            // Check if player is nation leader or assistant
            if (!nation.isKing(resident) && !nation.hasAssistant(resident)) {
                player.sendMessage(ChatColor.RED + "Only the nation leader or assistants can view nation budget details.");
                return;
            }

            Map<BudgetCategory, BudgetAllocation> allocations = budgetManager.getNationBudgetAllocations(nation);

            // Display budget allocations
            player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Budget Allocations ===");

            for (BudgetCategory category : BudgetCategory.values()) {
                BudgetAllocation allocation = allocations.get(category);
                player.sendMessage(ChatColor.YELLOW + category.name() + ": " +
                        ChatColor.WHITE + String.format("%.1f%%", allocation.getPercentage()));
            }

            // Show information about next budget cycle
            int daysUntil = budgetManager.getDaysUntilNextCycle(nation.getUUID(), true);
            String timeUntil = budgetManager.getFormattedTimeUntilNextCycle(nation.getUUID(), true);
            player.sendMessage(ChatColor.YELLOW + "Next budget cycle: " + ChatColor.WHITE + timeUntil);
        }
    }

    /**
     * Show information about the next budget cycle
     */
    private void showNextBudgetCycle(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;

            // Check if player is mayor or assistant
            if (!town.isMayor(resident)) {
                player.sendMessage(ChatColor.RED + "Only the mayor or assistants can view town budget cycle information.");
                return;
            }

            // Get days until next cycle
            int daysUntil = budgetManager.getDaysUntilNextCycle(town.getUUID(), false);
            String timeUntil = budgetManager.getFormattedTimeUntilNextCycle(town.getUUID(), false);

            player.sendMessage(ChatColor.GOLD + "=== " + town.getName() + "'s Budget Cycle ===");
            player.sendMessage(ChatColor.YELLOW + "Next budget cycle: " + ChatColor.WHITE + timeUntil);

            // If cycle is due today
            if (daysUntil == 0) {
                player.sendMessage(ChatColor.GREEN + "Your town's budget cycle will process soon.");
            }

        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;

            // Check if player is king or assistant
            if (!nation.isKing(resident) && !nation.hasAssistant(resident)) {
                player.sendMessage(ChatColor.RED + "Only the nation leader or assistants can view nation budget cycle information.");
                return;
            }

            // Get days until next cycle
            int daysUntil = budgetManager.getDaysUntilNextCycle(nation.getUUID(), true);
            String timeUntil = budgetManager.getFormattedTimeUntilNextCycle(nation.getUUID(), true);

            player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Budget Cycle ===");
            player.sendMessage(ChatColor.YELLOW + "Next budget cycle: " + ChatColor.WHITE + timeUntil);

            // If cycle is due today
            if (daysUntil == 0) {
                player.sendMessage(ChatColor.GREEN + "Your nation's budget cycle will process soon.");
            }
        }
    }

    private void handleSetBudget(Player player, Resident resident, String categoryStr, String percentageStr) {
        // Parse category
        BudgetCategory category;
        try {
            category = BudgetCategory.fromString(categoryStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid category: " + categoryStr);
            player.sendMessage(ChatColor.RED + "Valid categories: " +
                    Arrays.stream(BudgetCategory.values())
                            .map(BudgetCategory::name)
                            .collect(Collectors.joining(", ")));
            return;
        }

        // Parse percentage
        double percentage;
        try {
            percentage = Double.parseDouble(percentageStr);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid percentage: " + percentageStr);
            return;
        }

        // Handle town or nation
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;

            // Check if player is mayor
            if (!town.isMayor(resident)) {
                player.sendMessage(ChatColor.RED + "Only the mayor can change town budget allocations.");
                return;
            }

            // Set budget allocation
            boolean success = budgetManager.setTownBudgetAllocation(town, category, percentage);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully set " + town.getName() +
                        "'s " + category.name() + " budget allocation to " + percentage + "%");
            } else {
                // Get min/max bounds
                double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
                double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

                player.sendMessage(ChatColor.RED + "Failed to set budget allocation. Valid range: " +
                        minPercent + "% - " + maxPercent + "%");
            }

        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;

            // Check if player is nation leader
            if (!nation.isKing(resident)) {
                player.sendMessage(ChatColor.RED + "Only the nation leader can change nation budget allocations.");
                return;
            }

            // Set budget allocation
            boolean success = budgetManager.setNationBudgetAllocation(nation, category, percentage);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully set " + nation.getName() +
                        "'s " + category.name() + " budget allocation to " + percentage + "%");
            } else {
                // Get min/max bounds
                double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
                double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

                player.sendMessage(ChatColor.RED + "Failed to set budget allocation. Valid range: " +
                        minPercent + "% - " + maxPercent + "%");
            }
        }
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + commandSource.toUpperCase() + " Budget Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " budget" +
                ChatColor.WHITE + " - Show budget allocations");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " budget info" +
                ChatColor.WHITE + " - Show budget allocations");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " budget set <category> <percentage>" +
                ChatColor.WHITE + " - Set budget allocation for a category");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " budget cycle" +
                ChatColor.WHITE + " - Show information about the next budget cycle");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " budget help" +
                ChatColor.WHITE + " - Show this help message");

        player.sendMessage(ChatColor.YELLOW + "Available categories: " +
                Arrays.stream(BudgetCategory.values())
                        .map(BudgetCategory::name)
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete subcommands
            List<String> subCommands = Arrays.asList("info", "set", "cycle", "help");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            // Complete categories
            List<String> categories = Arrays.stream(BudgetCategory.values())
                    .map(category -> category.name().toLowerCase())
                    .collect(Collectors.toList());
            return filterCompletions(categories, args[1]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Suggest some reasonable percentage values
            completions.add("25");
            completions.add("50");
            completions.add("75");
            completions.add("100");
            return filterCompletions(completions, args[2]);
        }

        return completions;
    }
}