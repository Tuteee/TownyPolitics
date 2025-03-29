package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.commands.base.BaseCommand;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GovernmentCommand extends BaseCommand {

    private final GovernmentManager govManager;
    private final String commandSource;

    public GovernmentCommand(TownyPolitics plugin, GovernmentManager govManager, String commandSource) {
        super(plugin, "GovernmentCommand");
        this.govManager = govManager;
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

        // Both town and nation have the same simplified command structure now
        if (args.length == 0) {
            sendUsageMessage(player);
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
        if (commandSource.equals("town") && govType.isNationOnly()) {
            player.sendMessage(ChatColor.RED + "The government type " + govType.getDisplayName() +
                    " is only available for nations, not towns.");
            return true;
        }

        // Set government based on command source
        if (commandSource.equals("town")) {
            return setTownGovernment(player, resident, govType);
        } else if (commandSource.equals("nation")) {
            return setNationGovernment(player, resident, govType);
        }

        return true;
    }

    private void sendUsageMessage(Player player) {
        player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " government <type>");

        // Show appropriate government types based on command source
        if (commandSource.equals("town")) {
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

        if (commandSource.equals("nation")) {
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/nation overview" +
                    ChatColor.YELLOW + " to see your current government type");
        }
    }

    private boolean setTownGovernment(Player player, Resident resident, GovernmentType govType) {
        Town town = getTown(resident, player);
        if (town == null) {
            return true;
        }

        // Check if player is the mayor
        if (!isTownMayor(resident, town, player)) {
            return true;
        }

        // Check government change requirements
        GovernmentChangeResult result = checkGovernmentChangeRequirements(town, govType, false);
        if (!result.isSuccess()) {
            player.sendMessage(ChatColor.RED + result.getMessage());
            return true;
        }

        // Try to set government type
        boolean success = govManager.setGovernmentType(town, govType);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + town.getName() +
                    "'s government to " + govType.getDisplayName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to change government type due to a cooldown.");
        }

        return true;
    }

    private boolean setNationGovernment(Player player, Resident resident, GovernmentType govType) {
        Nation nation = getNation(resident, player);
        if (nation == null) {
            return true;
        }

        // Check if player is the king
        if (!isNationLeader(resident, nation, player)) {
            return true;
        }

        // Check government change requirements
        GovernmentChangeResult result = checkGovernmentChangeRequirements(nation, govType, true);
        if (!result.isSuccess()) {
            player.sendMessage(ChatColor.RED + result.getMessage());
            return true;
        }

        // Try to set government type
        boolean success = govManager.setGovernmentType(nation, govType);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Successfully changed " + nation.getName() +
                    "'s government to " + govType.getDisplayName() + ".");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to change government type due to a cooldown.");
        }

        return true;
    }

    private GovernmentChangeResult checkGovernmentChangeRequirements(Object entity, GovernmentType govType, boolean isNation) {
        // Check cooldown
        if (isNation) {
            Nation nation = (Nation) entity;
            if (govManager.isOnCooldown(nation)) {
                long remaining = govManager.getCooldownTimeRemaining(nation);
                String timeStr = govManager.formatCooldownTime(remaining);
                return new GovernmentChangeResult(false,
                        "Your nation must wait " + timeStr + " before changing government again.");
            }

            // Check switch time
            long lastChange = govManager.getLastChangeTime(nation);
            boolean hasChangedBefore = lastChange > 0;
            if (hasChangedBefore) {
                long switchTimeDays = plugin.getConfig().getLong("government.switch_time", 7);
                long switchTimeMillis = switchTimeDays * 24 * 60 * 60 * 1000;

                if (System.currentTimeMillis() - lastChange < switchTimeMillis) {
                    long remaining = switchTimeMillis - (System.currentTimeMillis() - lastChange);
                    String timeStr = govManager.formatCooldownTime(remaining);
                    return new GovernmentChangeResult(false,
                            "Your nation is still completing the transition to " +
                                    govManager.getGovernmentType(nation).getDisplayName() +
                                    ". Please wait " + timeStr + " before changing government again.");
                }
            }
        } else {
            Town town = (Town) entity;
            if (govManager.isOnCooldown(town)) {
                long remaining = govManager.getCooldownTimeRemaining(town);
                String timeStr = govManager.formatCooldownTime(remaining);
                return new GovernmentChangeResult(false,
                        "Your town must wait " + timeStr + " before changing government again.");
            }

            // Check switch time
            long lastChange = govManager.getLastChangeTime(town);
            boolean hasChangedBefore = lastChange > 0;
            if (hasChangedBefore) {
                long switchTimeDays = plugin.getConfig().getLong("government.switch_time", 7);
                long switchTimeMillis = switchTimeDays * 24 * 60 * 60 * 1000;

                if (System.currentTimeMillis() - lastChange < switchTimeMillis) {
                    long remaining = switchTimeMillis - (System.currentTimeMillis() - lastChange);
                    String timeStr = govManager.formatCooldownTime(remaining);
                    return new GovernmentChangeResult(false,
                            "Your town is still completing the transition to " +
                                    govManager.getGovernmentType(town).getDisplayName() +
                                    ". Please wait " + timeStr + " before changing government again.");
                }
            }
        }

        return new GovernmentChangeResult(true, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // For both town and nation, only provide government type completions at the first level
        if (args.length == 1) {
            List<String> govTypes;
            if (commandSource.equals("town")) {
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

    // Helper class for result handling
    private static class GovernmentChangeResult {
        private final boolean success;
        private final String message;

        public GovernmentChangeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}