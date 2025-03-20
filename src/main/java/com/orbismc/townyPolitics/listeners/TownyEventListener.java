package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;

public class TownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;

    // Constants for metadata keys - try to identify what's being used currently
    private static final String PP_META_KEY = "townypolitics_pp_display";
    private static final String[] POSSIBLE_KEYS = {
            "political_power", "pp", "townypolitics_pp", "politicalpower"
    };

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
        try {
            // For each possible key, try to blank out the displayed value
            for (String key : POSSIBLE_KEYS) {
                if (nation.hasMeta(key)) {
                    // Found an existing key, try to set its value to be minimal
                    if (nation.getMetadata(key) instanceof StringDataField) {
                        StringDataField field = (StringDataField) nation.getMetadata(key);
                        // Set to empty to minimize display
                        field.setValue("");
                    }
                }
            }

            // Now set our display key with the format we want
            if (nation.hasMeta(PP_META_KEY)) {
                if (nation.getMetadata(PP_META_KEY) instanceof StringDataField) {
                    StringDataField field = (StringDataField) nation.getMetadata(PP_META_KEY);
                    field.setValue("[Political Power]");
                } else {
                    nation.removeMetaData(nation.getMetadata(PP_META_KEY));
                    nation.addMetaData(new StringDataField(PP_META_KEY, "[Political Power]", ""));
                }
            } else {
                nation.addMetaData(new StringDataField(PP_META_KEY, "[Political Power]", ""));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating nation metadata: " + e.getMessage());
        }
    }

    /**
     * Add the political power line with hover effect to the status screen
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            // Update metadata to try to minimize any existing political power display
            Nation nation = event.getNation();
            updateNationPoliticalPowerMetadata(nation);

            // If the sender is a player, we'll send a component with hover effect
            if (event.getCommandSender() instanceof Player) {
                final Player player = (Player) event.getCommandSender();
                final double currentPP = ppManager.getPoliticalPower(nation);
                final double dailyGain = ppManager.calculateDailyPPGain(nation);

                // Schedule a task to add the hover component after the status screen
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Create hover text with detailed information
                            ComponentBuilder hoverBuilder = new ComponentBuilder("Current: ")
                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                    .append(String.format("%.2f", currentPP))
                                    .color(net.md_5.bungee.api.ChatColor.WHITE)
                                    .append("\nDaily Gain: ")
                                    .color(net.md_5.bungee.api.ChatColor.GOLD)
                                    .append("+" + String.format("%.2f", dailyGain) + "/day")
                                    .color(net.md_5.bungee.api.ChatColor.GREEN);

                            // Create the main component with hover effect
                            TextComponent ppComponent = new TextComponent("[Political Power] - Hover for details");
                            ppComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
                            ppComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));

                            // Send the component to the player
                            player.spigot().sendMessage(ppComponent);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error sending hover component: " + e.getMessage());
                        }
                    }
                }, 2L);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling nation status event: " + e.getMessage());
        }
    }
}