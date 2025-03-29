package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.TownPoliticalPowerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TownPoliticalPowerCommand implements CommandExecutor {

    private final TownyPolitics plugin;
    private final TownPoliticalPowerManager ppManager;
    private final TownyAPI townyAPI;

    public TownPoliticalPowerCommand(TownyPolitics plugin, TownPoliticalPowerManager ppManager) {
        this.plugin = plugin;
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

        // Check if player is in a town
        Resident resident;
        try {
            // Use direct TownyAPI method instead of going through DataSource
            resident = townyAPI.getResident(player.getUniqueId());

            if (resident == null) {
                player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
                return true;
            }

            // If no args, show the player's town PP
            if (args.length == 0) {
                if (!resident.hasTown()) {
                    player.sendMessage(ChatColor.RED + "You are not part of a town.");
                    return true;
                }

                Town playerTown = resident.getTown();
                double pp = ppManager.getPoliticalPower(playerTown);
                double dailyGain = ppManager.calculateDailyPPGain(playerTown);

                player.sendMessage(ChatColor.GOLD + "=== " + playerTown.getName() + "'s Political Power ===");
                player.sendMessage(ChatColor.YELLOW + "Current PP: " + ChatColor.WHITE + String.format("%.2f", pp));
                player.sendMessage(ChatColor.YELLOW + "Daily Gain: " + ChatColor.WHITE + String.format("%.2f", dailyGain));
                player.sendMessage(ChatColor.YELLOW + "Residents: " + ChatColor.WHITE + playerTown.getResidents().size());

                return true;
            }

            // If args, show the specified town's PP
            String townName = args[0];
            try {
                // Use direct TownyAPI method to get town by name
                Town town = townyAPI.getTown(townName);

                if (town == null) {
                    player.sendMessage(ChatColor.RED + "Town not found: " + townName);
                    return true;
                }

                double pp = ppManager.getPoliticalPower(town);
                double dailyGain = ppManager.calculateDailyPPGain(town);

                player.sendMessage(ChatColor.GOLD + "=== " + town.getName() + "'s Political Power ===");
                player.sendMessage(ChatColor.YELLOW + "Current PP: " + ChatColor.WHITE + String.format("%.2f", pp));
                player.sendMessage(ChatColor.YELLOW + "Daily Gain: " + ChatColor.WHITE + String.format("%.2f", dailyGain));
                player.sendMessage(ChatColor.YELLOW + "Residents: " + ChatColor.WHITE + town.getResidents().size());

                return true;
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Error finding town: " + townName);
                return true;
            }

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Error retrieving your Towny data: " + e.getMessage());
            return true;
        }
    }
}