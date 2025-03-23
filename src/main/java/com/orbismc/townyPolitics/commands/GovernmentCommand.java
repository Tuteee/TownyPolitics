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
        System.out.println("Args length: " + args.length);
        if (args.length > 0) {
            System.out.println("First arg: " + args[0]);
        }

        // Check if player is registered in Towny
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }

        // Both town and nation have the same simplified command structure now
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /" + COMMAND_SOURCE + " government <type>");
            player.sendMessage(ChatColor.YELLOW + "Available government types: " +
                    Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.joining(", ")));

            if (COMMAND_SOURCE.equals("nation")) {
                player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/nation overview" +
                        ChatColor.YELLOW + " to see your current government type");
            }
            return true;
        }

        // First argument is the government type
        String govTypeName = args[0].toUpperCase();

        // Try to get the government type
        GovernmentType govType = GovernmentType.getByName(govTypeName);
        if (govType == null) {
            player.sendMessage(ChatColor.RED + "Invalid government type: " + args[0]);
            player.sendMessage(ChatColor.RED + "Available types: " +
                    Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.joining(", ")));
            return true;
        }

        // Check permission
        String permissionNode = "townypolitics.government.set." + COMMAND_SOURCE;
        if (!player.hasPermission(permissionNode)) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change government types.");
            return true;
        }

        // Set government based on command source
        if (COMMAND_SOURCE.equals("town")) {
            return setTownGovernment(player, resident, govType);
        } else if (COMMAND_SOURCE.equals("nation")) {
            return setNationGovernment(player, resident, govType);
        }

        return true;
    }

    /**
     * Set town government type
     *
     * @param player The player
     * @param resident The resident
     * @param govType The government type to set
     * @return true
     */
    private boolean setTownGovernment(Player player, Resident resident, GovernmentType govType) {
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

    /**
     * Set nation government type
     *
     * @param player The player
     * @param resident The resident
     * @param govType The government type to set
     * @return true
     */
    private boolean setNationGovernment(Player player, Resident resident, GovernmentType govType) {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // For both town and nation, only provide government type completions at the first level
        if (args.length == 1) {
            List<String> govTypes = Arrays.stream(GovernmentType.values())
                    .map(GovernmentType::name)
                    .collect(Collectors.toList());
            return filterCompletions(govTypes, args[0]);
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