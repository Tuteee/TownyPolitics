package com.orbismc.townyPolitics.listeners;

import com.orbismc.townyPolitics.components.*; // Import ElectionStatusComponent
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
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
    private final ElectionStatusComponent electionComponent; // Added

    public StatusScreenListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "StatusScreen");

        // Initialize components - Ensure ElectionManager is available
        this.ppComponent = new PoliticalPowerComponent(plugin);
        this.govComponent = new GovernmentComponent(plugin);
        this.corruptionComponent = new CorruptionComponent(plugin);
        this.policyComponent = new PolicyComponent(plugin);
        if (plugin.getElectionManager() != null) { // Check if ElectionManager initialized
            this.electionComponent = new ElectionStatusComponent(plugin, plugin.getElectionManager()); // Added Initialization
        } else {
            this.electionComponent = null;
            logger.warning("ElectionManager is null, ElectionStatusComponent could not be initialized!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNationStatusScreenEarly(NationStatusScreenEvent event) {
        // Clear any default components that we want to replace
        if (ppComponent != null) {
            ppComponent.updateNationPoliticalPowerMetadata(event.getNation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();

            // Add each component - Use null checks for safety
            if (ppComponent != null) ppComponent.addToNationScreen(event, nation);
            if (govComponent != null) govComponent.addToNationScreen(event, nation);
            if (corruptionComponent != null) corruptionComponent.addToNationScreen(event, nation);
            if (policyComponent != null) policyComponent.addToNationScreen(event, nation);
            if (electionComponent != null) electionComponent.addToNationScreen(event, nation); // Added call

        } catch (Exception e) {
            logger.severe("Error adding components to nation status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();

            // Add each component - Use null checks for safety
            if (govComponent != null) govComponent.addToTownScreen(event, town);
            if (corruptionComponent != null) corruptionComponent.addToTownScreen(event, town);
            if (policyComponent != null) policyComponent.addToTownScreen(event, town);
            if (electionComponent != null) electionComponent.addToTownScreen(event, town); // Added call

            // Only add town pp component if it's enabled
            if (plugin.getTownPPManager() != null && ppComponent != null) {
                ppComponent.addToTownScreen(event, town);
            }

        } catch (Exception e) {
            logger.severe("Error adding components to town status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}