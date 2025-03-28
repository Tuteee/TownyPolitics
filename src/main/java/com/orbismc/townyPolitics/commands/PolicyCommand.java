package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PolicyManager;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.utils.PolicyEffectsDisplay;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PolicyCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final PolicyManager policyManager;
    private final TownyAPI townyAPI;
    private final String commandSource;

    public PolicyCommand(TownyPolitics plugin, PolicyManager policyManager, String commandSource) {
        this.plugin = plugin;
        this.policyManager = policyManager;
        this.townyAPI = TownyAPI.getInstance();
        this.commandSource = commandSource; // "town" or "nation"
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
            sender.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }

        if (args.length == 0) {
            showPolicyHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleListCommand(player, resident, args);
            case "info":
                return handleInfoCommand(player, resident, args);
            case "enact":
                return handleEnactCommand(player, resident, args);
            case "revoke":
                return handleRevokeCommand(player, resident, args);
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                showPolicyHelp(player);
                return true;
        }
    }

    private boolean handleListCommand(Player player, Resident resident, String[] args) {
        if (commandSource.equals("town")) {
            // Town policies
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a town.");
                return true;
            }

            Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(town);
            if (activePolicies.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Your town has no active policies.");
                return true;
            }

            player.sendMessage(ChatColor.GOLD + "=== " + town.getName() + "'s Active Policies ===");
            for (ActivePolicy activePolicy : activePolicies) {
                Policy policy = policyManager.getPolicy(activePolicy.getPolicyId());
                if (policy == null) continue;

                player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + policy.getName() +
                        ChatColor.GRAY + " (" + activePolicy.formatRemainingTime() + ")" +
                        ChatColor.DARK_GRAY + " ID: " + activePolicy.getId());
            }
        } else {
            // Nation policies
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }

            Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(nation);
            if (activePolicies.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Your nation has no active policies.");
                return true;
            }

            player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Active Policies ===");
            for (ActivePolicy activePolicy : activePolicies) {
                Policy policy = policyManager.getPolicy(activePolicy.getPolicyId());
                if (policy == null) continue;

                player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + policy.getName() +
                        ChatColor.GRAY + " (" + activePolicy.formatRemainingTime() + ")" +
                        ChatColor.DARK_GRAY + " ID: " + activePolicy.getId());
            }
        }

        return true;
    }

    private boolean handleInfoCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " policy info <policy_id>");
            return true;
        }

        String policyId = args[1];
        Policy policy = policyManager.getPolicy(policyId);

        if (policy == null) {
            player.sendMessage(ChatColor.RED + "Policy not found: " + policyId);
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== Policy: " + policy.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + policy.getDescription());
        player.sendMessage(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + policy.getCost() + " Political Power");
        player.sendMessage(ChatColor.YELLOW + "Duration: " + ChatColor.WHITE +
                (policy.getDuration() < 0 ? "Permanent" : policy.getDuration() + " days"));
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + policy.getType().name());

        // Show requirements
        player.sendMessage(ChatColor.YELLOW + "Requirements:");
        if (policy.getMinPoliticalPower() > 0) {
            player.sendMessage(ChatColor.GRAY + "• Min Political Power: " +
                    ChatColor.WHITE + policy.getMinPoliticalPower());
        }
        if (policy.getMaxCorruption() < 100) {
            player.sendMessage(ChatColor.GRAY + "• Max Corruption: " +
                    ChatColor.WHITE + policy.getMaxCorruption() + "%");
        }

        // Show allowed governments
        if (!policy.getAllowedGovernments().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Allowed Governments:");
            policy.getAllowedGovernments().forEach(govt ->
                    player.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + govt.getDisplayName())
            );
        }

        // Show effects
        player.sendMessage(ChatColor.YELLOW + "Effects:");
        PolicyEffectsDisplay.displayEffects(player, policy.getEffects());

        return true;
    }

    private boolean handleEnactCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " policy enact <policy_id>");
            return true;
        }

        String policyId = args[1];
        Policy policy = policyManager.getPolicy(policyId);

        if (policy == null) {
            player.sendMessage(ChatColor.RED + "Policy not found: " + policyId);
            return true;
        }

        if (commandSource.equals("town")) {
            // Town enacting policy
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a town.");
                return true;
            }

            // Check if player is mayor
            if (!town.isMayor(resident)) {
                player.sendMessage(ChatColor.RED + "Only the mayor can enact town policies.");
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(town.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(town.getUUID());
                String timeStr = policyManager.formatCooldownTime(town.getUUID());
                player.sendMessage(ChatColor.RED + "Your town must wait " + timeStr + " before changing policies again.");
                return true;
            }

            // Attempt to enact the policy
            boolean success = policyManager.enactPolicy(town, policyId);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully enacted policy: " + policy.getName());
            } else {
                player.sendMessage(ChatColor.RED + "Failed to enact policy. Check requirements and try again.");
            }

        } else {
            // Nation enacting policy
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }

            // Check if player is king
            if (!nation.isKing(resident)) {
                player.sendMessage(ChatColor.RED + "Only the nation leader can enact nation policies.");
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(nation.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(nation.getUUID());
                String timeStr = policyManager.formatCooldownTime(nation.getUUID());
                player.sendMessage(ChatColor.RED + "Your nation must wait " + timeStr + " before changing policies again.");
                return true;
            }

            // Attempt to enact the policy
            boolean success = policyManager.enactPolicy(nation, policyId);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully enacted policy: " + policy.getName());
            } else {
                player.sendMessage(ChatColor.RED + "Failed to enact policy. Check requirements and try again.");
            }
        }

        return true;
    }

    private boolean handleRevokeCommand(Player player, Resident resident, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " policy revoke <policy_uuid>");
            return true;
        }

        String policyUuidStr = args[1];
        UUID policyUuid;

        try {
            policyUuid = UUID.fromString(policyUuidStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Invalid policy UUID format. Use /" + commandSource + " policy list to see active policies.");
            return true;
        }

        if (commandSource.equals("town")) {
            // Town revoking policy
            Town town = resident.getTownOrNull();
            if (town == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a town.");
                return true;
            }

            // Check if player is mayor
            if (!town.isMayor(resident)) {
                player.sendMessage(ChatColor.RED + "Only the mayor can revoke town policies.");
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(town.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(town.getUUID());
                String timeStr = policyManager.formatCooldownTime(town.getUUID());
                player.sendMessage(ChatColor.RED + "Your town must wait " + timeStr + " before changing policies again.");
                return true;
            }

            // Attempt to revoke the policy
            boolean success = policyManager.revokePolicy(town.getUUID(), policyUuid, false);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully revoked policy.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to revoke policy. It may not exist or belong to your town.");
            }

        } else {
            // Nation revoking policy
            Nation nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }

            // Check if player is king
            if (!nation.isKing(resident)) {
                player.sendMessage(ChatColor.RED + "Only the nation leader can revoke nation policies.");
                return true;
            }

            // Check cooldown
            if (policyManager.isOnCooldown(nation.getUUID())) {
                long remaining = policyManager.getCooldownTimeRemaining(nation.getUUID());
                String timeStr = policyManager.formatCooldownTime(nation.getUUID());
                player.sendMessage(ChatColor.RED + "Your nation must wait " + timeStr + " before changing policies again.");
                return true;
            }

            // Attempt to revoke the policy
            boolean success = policyManager.revokePolicy(nation.getUUID(), policyUuid, true);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Successfully revoked policy.");
            } else {
                player.sendMessage(ChatColor.RED + "Failed to revoke policy. It may not exist or belong to your nation.");
            }
        }

        return true;
    }

    private void showPolicyHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + commandSource.toUpperCase() + " Policy Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " policy list" +
                ChatColor.WHITE + " - List all active policies");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " policy info <policy_id>" +
                ChatColor.WHITE + " - View detailed information about a policy");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " policy enact <policy_id>" +
                ChatColor.WHITE + " - Enact a new policy");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " policy revoke <policy_uuid>" +
                ChatColor.WHITE + " - Revoke an active policy");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("list", "info", "enact", "revoke");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("enact")) {
                // Return available policy IDs
                return filterCompletions(
                        policyManager.getAvailablePolicies().stream()
                                .map(Policy::getId)
                                .collect(Collectors.toList()),
                        args[1]
                );
            } else if (args[0].equalsIgnoreCase("revoke") && sender instanceof Player) {
                // Return active policy UUIDs
                Player player = (Player) sender;
                Resident resident = townyAPI.getResident(player.getUniqueId());

                if (resident != null) {
                    Set<ActivePolicy> activePolicies;

                    if (commandSource.equals("town")) {
                        Town town = resident.getTownOrNull();
                        if (town != null) {
                            activePolicies = policyManager.getActivePolicies(town);
                        } else {
                            return completions;
                        }
                    } else {
                        Nation nation = resident.getNationOrNull();
                        if (nation != null) {
                            activePolicies = policyManager.getActivePolicies(nation);
                        } else {
                            return completions;
                        }
                    }

                    return filterCompletions(
                            activePolicies.stream()
                                    .map(policy -> policy.getId().toString())
                                    .collect(Collectors.toList()),
                            args[1]
                    );
                }
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}