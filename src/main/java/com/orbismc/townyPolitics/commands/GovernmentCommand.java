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

        // Check if player is registered in Towny
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }

        // Both town and nation have the same simplified command structure now
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /" + COMMAND_SOURCE + " government <type>");

            // Show appropriate government types based on command source
            if (COMMAND_SOURCE.equals("town")) {
                player.sendMessage(ChatColor.YELLOW + "Available government types: " +
                        Arrays.stream(GovernmentType.getTownGovernmentTypes())
                                .map(GovernmentType::name)
                                .collect(Collectors.joining(", ")));
            } else {
                player.sendMessage(ChatColor.YELLOW + "Available government types: " +
                        Arrays.stream(GovernmentType.values())
                                .map(GovernmentType::name)
                                .collect(Collectors.joining(", ")));
            }

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

        // Check if trying to set a nation-only government type for a town
        if (COMMAND_SOURCE.equals("town") && govType.isNationOnly()) {
            player.sendMessage(ChatColor.RED + "The government type " + govType.getDisplayName() +
                    " is only available for nations, not towns.");
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

        // Check cooldown
        if (govManager.isOnCooldown(town)) {
            long remaining = govManager.getCooldownTimeRemaining(town);
            String timeStr = govManager.formatCooldownTime(remaining);
            player.sendMessage(ChatColor.RED + "Your town must wait " + timeStr + " before changing government again.");
            return true;
        }

        // Try to set government type
        boolean success = govManager.setGovernmentType(town, govType);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + town.getName() + "'s government to " + govType.getDisplayName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to change government type due to a cooldown.");
        }

        return true;
    }

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

        // Check cooldown
        if (govManager.isOnCooldown(nation)) {
            long remaining = govManager.getCooldownTimeRemaining(nation);
            String timeStr = govManager.formatCooldownTime(remaining);
            player.sendMessage(ChatColor.RED + "Your nation must wait " + timeStr + " before changing government again.");
            return true;
        }

        // Try to set government type
        boolean success = govManager.setGovernmentType(nation, govType);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + nation.getName() + "'s government to " + govType.getDisplayName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to change government type due to a cooldown.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // For both town and nation, only provide government type completions at the first level
        if (args.length == 1) {
            List<String> govTypes;
            if (COMMAND_SOURCE.equals("town")) {
                govTypes = Arrays.stream(GovernmentType.getTownGovernmentTypes())
                        .map(GovernmentType::name)
                        .collect(Collectors.toList());
            } else {
                govTypes = Arrays.stream(GovernmentType.values())
                        .map(GovernmentType::name)
                        .collect(Collectors.toList());
            }
            return filterCompletions(govTypes, args[0]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}