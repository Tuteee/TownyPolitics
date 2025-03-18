package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.orbismc.townypolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PoliticalPowerCommand implements CommandExecutor, TabCompleter {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;
    private final TownyAPI townyAPI;

    public PoliticalPowerCommand(TownyPolitics plugin, PoliticalPowerManager ppManager) {
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

        // Check if player is in a nation
        Resident resident;
        try {
            resident = townyAPI.getDataSource().getResident(player.getName());

            // If no args, show the player's nation PP
            if (args.length == 0) {
                if (!resident.hasNation()) {
                    player.sendMessage(ChatColor.RED + "You are not part of a nation.");
                    return true;
                }

                Nation playerNation = resident.getNation();
                double pp = ppManager.getPoliticalPower(playerNation);
                double dailyGain = ppManager.calculateDailyPPGain(playerNation);

                player.sendMessage(ChatColor.GOLD + "=== " + playerNation.getName() + "'s Political Power ===");
                player.sendMessage(ChatColor.YELLOW + "Current PP: " + ChatColor.WHITE + String.format("%.2f", pp));
                player.sendMessage(ChatColor.YELLOW + "Daily Gain: " + ChatColor.WHITE + String.format("%.2f", dailyGain));
                player.sendMessage(ChatColor.YELLOW + "Residents: " + ChatColor.WHITE + playerNation.getNumResidents());

                return true;
            }

            // If args, show the specified nation's PP
            String nationName = args[0];
            try {
                Nation nation = townyAPI.getDataSource().getNation(nationName);
                double pp = ppManager.getPoliticalPower(nation);
                double dailyGain = ppManager.calculateDailyPPGain(nation);

                player.sendMessage(ChatColor.GOLD + "=== " + nation.getName() + "'s Political Power ===");
                player.sendMessage(ChatColor.YELLOW + "Current PP: " + ChatColor.WHITE + String.format("%.2f", pp));
                player.sendMessage(ChatColor.YELLOW + "Daily Gain: " + ChatColor.WHITE + String.format("%.2f", dailyGain));
                player.sendMessage(ChatColor.YELLOW + "Residents: " + ChatColor.WHITE + nation.getNumResidents());

                return true;
            } catch (NotRegisteredException e) {
                player.sendMessage(ChatColor.RED + "Nation not found: " + nationName);
                return true;
            }

        } catch (NotRegisteredException e) {
            player.sendMessage(ChatColor.RED + "You are not registered in Towny.");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab complete for nation names
            String partialName = args[0].toLowerCase();

            // Get all nation names
            List<String> nationNames = townyAPI.getNations().stream()
                    .map(Nation::getName)
                    .collect(Collectors.toList());

            // Filter by partial input
            for (String name : nationNames) {
                if (name.toLowerCase().startsWith(partialName)) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }
}