package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.managers.CorruptionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OverviewCommand implements CommandExecutor {

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final PoliticalPowerManager ppManager;
    private final CorruptionManager corruptionManager;
    private final TownyAPI townyAPI;

    public OverviewCommand(TownyPolitics plugin, GovernmentManager govManager, PoliticalPowerManager ppManager, CorruptionManager corruptionManager) {
        this.plugin = plugin;
        this.govManager = govManager;
        this.ppManager = ppManager;
        this.corruptionManager = corruptionManager;
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

        Nation nation;
        if (args.length > 0) {
            // Look up nation by name
            nation = townyAPI.getNation(args[0]);
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "Nation not found: " + args[0]);
                return true;
            }
        } else {
            // Use player's nation
            nation = resident.getNationOrNull();
            if (nation == null) {
                player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                return true;
            }
        }

        showNationOverview(player, nation);
        return true;
    }

    private void showNationOverview(Player player, Nation nation) {
        // Get government type
        GovernmentType govType = govManager.getGovernmentType(nation);

        // Get political power
        double pp = ppManager.getPoliticalPower(nation);
        double dailyGain = ppManager.calculateDailyPPGain(nation);

        // Get corruption
        double corruption = corruptionManager.getCorruption(nation);
        double corruptionGain = corruptionManager.calculateDailyCorruptionGain(nation);
        boolean isCritical = corruptionManager.isCorruptionCritical(nation);

        // Display overview with custom header
        player.sendMessage(ChatColor.GOLD + ".oOo.________.[" + ChatColor.YELLOW + " " + nation.getName() + "'s Political Overview " + ChatColor.GOLD + "].________.oOo.");

        // Government section
        player.sendMessage(ChatColor.DARK_GREEN + "Government: " + ChatColor.GREEN + govType.getDisplayName());

        // Format the description to show effects title in dark green and effects in green
        String[] descLines = govType.getDescription().split("\n");
        for (String line : descLines) {
            if (line.contains("Effects:")) {
                player.sendMessage(ChatColor.DARK_GREEN + line);
            } else {
                player.sendMessage(ChatColor.GREEN + "  " + line);
            }
        }

        // Political Power section
        player.sendMessage(ChatColor.DARK_GREEN + "Political Power: " + ChatColor.GREEN + String.format("%.2f", pp));
        player.sendMessage(ChatColor.DARK_GREEN + "Daily Political Power Gain: " + ChatColor.GREEN + String.format("+%.2f", dailyGain));

        // Corruption section
        ChatColor corruptColor;
        if (corruption >= 75) corruptColor = ChatColor.DARK_RED;
        else if (corruption >= 50) corruptColor = ChatColor.RED;
        else if (corruption <= 25) corruptColor = ChatColor.YELLOW;
        else corruptColor = ChatColor.GREEN;

        player.sendMessage(ChatColor.DARK_RED + "Corruption Level: " + corruptColor + String.format("%.1f%%", corruption));
        player.sendMessage(ChatColor.DARK_RED + "Daily Corruption Gain: " + ChatColor.RED + String.format("+%.2f%%", corruptionGain));
    }

    private String formatModifier(double modifier) {
        String formattedValue = String.format("%+.1f%%", (modifier - 1.0) * 100);

        if (modifier > 1.0) {
            return ChatColor.RED + formattedValue;
        } else if (modifier < 1.0) {
            return ChatColor.RED + formattedValue;
        } else {
            return ChatColor.GREEN + "0%";
        }
    }
}