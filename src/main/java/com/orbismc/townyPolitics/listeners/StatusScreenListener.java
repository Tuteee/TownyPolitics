package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.components.CorruptionComponent;
import com.orbismc.townyPolitics.components.GovernmentComponent;
import com.orbismc.townyPolitics.components.PoliticalPowerComponent;
import com.orbismc.townyPolitics.components.PolicyComponent;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles all status screen components for towns and nations
 */
public class StatusScreenListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Components
    private final PoliticalPowerComponent ppComponent;
    private final GovernmentComponent govComponent;
    private final CorruptionComponent corruptionComponent;
    private final PolicyComponent policyComponent;

    public StatusScreenListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "StatusScreen");

        // Initialize components
        this.ppComponent = new PoliticalPowerComponent(plugin);
        this.govComponent = new GovernmentComponent(plugin);
        this.corruptionComponent = new CorruptionComponent(plugin);
        this.policyComponent = new PolicyComponent(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNationStatusScreenEarly(NationStatusScreenEvent event) {
        // Clear any default components that we want to replace
        ppComponent.updateNationPoliticalPowerMetadata(event.getNation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();

            // Add each component
            ppComponent.addToNationScreen(event, nation);
            govComponent.addToNationScreen(event, nation);
            corruptionComponent.addToNationScreen(event, nation);
            policyComponent.addToNationScreen(event, nation);

        } catch (Exception e) {
            logger.severe("Error adding components to nation status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();

            // Add each component
            govComponent.addToTownScreen(event, town);
            corruptionComponent.addToTownScreen(event, town);
            policyComponent.addToTownScreen(event, town);

            // Only add town pp component if it's enabled
            if (plugin.getTownPPManager() != null) {
                ppComponent.addToTownScreen(event, town);
            }

        } catch (Exception e) {
            logger.severe("Error adding components to town status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}