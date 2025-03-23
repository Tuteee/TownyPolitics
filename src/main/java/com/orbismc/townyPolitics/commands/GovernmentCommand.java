package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;

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

public class GovernmentCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final TownyAPI townyAPI;
    private final String COMMAND_SOURCE;

    public GovernmentCommand(TownyPolitics plugin, GovernmentManager govManager, String commandSource) {
        this.plugin = plugin;
        this.govManager = govManager;
        this.townyAPI = TownyAPI.getInstance();
        this.COMMAND_SOURCE = commandSource; // "town" or "nation"
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Debug info
        System.out.println("Command label: " + label);
        System.out.println("Command name: " + command.getName());
        System.out.println("Command source: " + COMMAND_SOURCE);

        // Check if player is registered in Towny
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }

        // Handle subcommands
        if (args.length == 0) {
            if (!player.hasPermission("townypolitics.government.info")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to view government info.");
                return true;
            }
            return showInfo(player, resident);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                return showHelp(player);
            case "info":
                if (!player.hasPermission("townypolitics.government.info")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to view government info.");
                    return true;
                }
                return showInfo(player, resident);
            case "set":
                String permNode = "townypolitics.government.set." + COMMAND_SOURCE;
                if (!player.hasPermission(permNode)) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to change government types.");
                    return true;
                }
                return setGovernment(player, resident, args);
            case "list":
                if (!player.hasPermission("townypolitics.government.list")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to list government types.");
                    return true;
                }
                return listGovernments(player);
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                return showHelp(player);
        }
    }

    /**
     * Show command help to a player
     *
     * @param player The player
     * @return true
     */
    private boolean showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Government Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + COMMAND_SOURCE + " government " + ChatColor.WHITE + "- View government type");
        player.sendMessage(ChatColor.YELLOW + "/" + COMMAND_SOURCE + " government set <type> " + ChatColor.WHITE + "- Set government type (mayor/king only)");
        player.sendMessage(ChatColor.YELLOW + "/" + COMMAND_SOURCE + " government list " + ChatColor.WHITE + "- List all government types");
        player.sendMessage(ChatColor.YELLOW + "/" + COMMAND_SOURCE + " government help " + ChatColor.WHITE + "- Show this help message");
        return true;
    }

    /**
     * Show government info to a player
     *
     * @param player The player
     * @param resident The resident
     * @return true
     */
    private boolean showInfo(Player player, Resident resident) {
        if (COMMAND_SOURCE.equals("town")) {
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a town.");
                return true;
            }

            GovernmentType govType = govManager.getGovernmentType(town);

            player.sendMessage(ChatColor.GOLD + "=== " + town.getName() + "'s Government ===");
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + govType.getDisplayName());
            player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + govType.getDescription());

            return true;
        } else if (COMMAND_SOURCE.equals("nation")) {
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }

            GovernmentType govType = govManager.getGovernmentType(nation);

            player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Government ===");
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + govType.getDisplayName());
            player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + govType.getDescription());

            return true;
        }

        return showHelp(player);
    }

    /**
     * Set government type
     *
     * @param player The player
     * @param resident The resident
     * @param args Command arguments
     * @return true
     */
    private boolean setGovernment(Player player, Resident resident, String[] args) {
        // Check if args are valid
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + COMMAND_SOURCE + " government set <type>");
            return true;
        }

        String govTypeName = args[1].toUpperCase();

        // Try to get the government type
        GovernmentType govType = GovernmentType.getByName(govTypeName);
        if (govType == null) {
            player.sendMessage(ChatColor.RED + "Invalid government type: " + args[1]);
            player.sendMessage(ChatColor.RED + "Available types: " +
                    Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.joining(", ")));
            return true;
        }

        // Handle town government change
        if (COMMAND_SOURCE.equals("town")) {
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a town.");
                return true;
            }

            // Check if player is the mayor
            if (!town.getMayor().equals(resident)) {
                player.sendMessage(ChatColor.RED + "You must be the mayor to change the town's government.");
                return true;
            }

            // Set government type
            govManager.setGovernmentType(town, govType);
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + town.getName() + "'s government to " + govType.getDisplayName() + ".");

            return true;
        }
        // Handle nation government change
        else if (COMMAND_SOURCE.equals("nation")) {
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }

            // Check if player is the king
            if (!nation.isKing(resident)) {
                player.sendMessage(ChatColor.RED + "You must be the nation leader to change the nation's government.");
                return true;
            }

            // Set government type
            govManager.setGovernmentType(nation, govType);
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + nation.getName() + "'s government to " + govType.getDisplayName() + ".");

            return true;
        }

        return showHelp(player);
    }

    /**
     * List all government types
     *
     * @param player The player
     * @return true
     */
    private boolean listGovernments(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Available Government Types ===");

        for (GovernmentType type : GovernmentType.values()) {
            player.sendMessage(ChatColor.YELLOW + type.getDisplayName() + ChatColor.WHITE + " - " + type.getDescription());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - subcommands
            List<String> subCommands = Arrays.asList("help", "info", "set", "list");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            // Second argument for "set" - government types
            List<String> govTypes = Arrays.stream(GovernmentType.values())
                    .map(GovernmentType::name)
                    .collect(Collectors.toList());
            return filterCompletions(govTypes, args[1]);
        }

        return completions;
    }

    /**
     * Filter tab completions by prefix
     *
     * @param options Available options
     * @param prefix Current user input prefix
     * @return Filtered completions
     */
    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}