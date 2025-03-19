package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;

    public TownyEventListener(TownyPolitics plugin, PoliticalPowerManager ppManager) {
        this.plugin = plugin;
        this.ppManager = ppManager;
    }

    /**
     * Handle Towny's NewDayEvent to distribute political power
     *
     * @param event The NewDayEvent
     */
    @EventHandler
    public void onNewDay(NewDayEvent event) {
        plugin.getLogger().info("Processing new day for political power distribution...");
        ppManager.processNewDay();
        plugin.getLogger().info("Political power distribution complete.");
    }

    /**
     * Updates political power information for a specific nation
     * This method is called from other classes when nation PP changes
     *
     * @param nation The nation to update
     */
    public void updateNationPoliticalPowerMetadata(Nation nation) {
        // This method exists for compatibility with other classes
        // but doesn't need to do anything as we're using the hover approach
    }

    /**
     * Intercept the /n command and add our information with hover effect
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        // Check if this is a nation status command
        if (command.startsWith("/n ") || command.equals("/n") ||
                command.startsWith("/nation ") || command.equals("/nation")) {

            // Get arguments (nation name if any)
            String[] args = command.split(" ");

            // If this is a subcommand like "/n new" or "/n invite", ignore it
            if (args.length > 1) {
                String subCommand = args[1].toLowerCase();
                if (subCommand.equals("new") || subCommand.equals("create") ||
                        subCommand.equals("invite") || subCommand.equals("add") ||
                        subCommand.equals("kick") || subCommand.equals("deposit") ||
                        subCommand.equals("withdraw") || subCommand.equals("leave") ||
                        subCommand.equals("ally") || subCommand.equals("enemy") ||
                        subCommand.equals("neutral") || subCommand.equals("set") ||
                        subCommand.equals("toggle") || subCommand.equals("rank") ||
                        subCommand.equals("king") || subCommand.equals("mayor") ||
                        subCommand.equals("delete") || subCommand.equals("merge") ||
                        subCommand.equals("politicalpower") || subCommand.equals("pp")) {
                    return;
                }
            }

            final Player player = event.getPlayer();

            // Schedule a delayed task to find the nation and send PP info after the status screen
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        Nation nation = null;

                        // Try to get nation from arguments first
                        if (args.length > 1) {
                            String nationName = args[1];
                            nation = TownyAPI.getInstance().getNation(nationName);
                        }

                        // If no nation from args, try to get player's nation
                        if (nation == null) {
                            try {
                                nation = TownyAPI.getInstance().getResident(player.getUniqueId()).getNation();
                            } catch (Exception e) {
                                // Player might not be in a nation
                                return;
                            }
                        }

                        // If we found a nation, show PP info
                        if (nation != null && player.isOnline()) {
                            double currentPP = ppManager.getPoliticalPower(nation);
                            double dailyGain = ppManager.calculateDailyPPGain(nation);

                            // Create the hover text with detailed information
                            String hoverText = ChatColor.GOLD + "Current: " + ChatColor.WHITE +
                                    String.format("%.2f", currentPP) + "\n" +
                                    ChatColor.GOLD + "Daily Gain: " + ChatColor.GREEN +
                                    "+" + String.format("%.2f", dailyGain) + "/day";

                            // Create the hover components from the text
                            BaseComponent[] hoverComponent = TextComponent.fromLegacyText(hoverText);

                            // Create the main component with hover effect
                            TextComponent ppComponent = new TextComponent(ChatColor.GOLD + "[Political Power]");
                            ppComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent));

                            // Send the component to the player
                            player.spigot().sendMessage(ppComponent);
                        }
                    } catch (Exception e) {
                        // Log any errors
                        plugin.getLogger().warning("Error showing political power: " + e.getMessage());
                    }
                }
            }, 2L);
        }
    }
}