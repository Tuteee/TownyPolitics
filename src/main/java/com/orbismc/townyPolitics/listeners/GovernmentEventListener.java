package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.TownGovernmentManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.utils.EventHelper;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class GovernmentEventListener implements Listener {
    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final TownGovernmentManager townGovManager;
    private final DelegateLogger logger;

    public GovernmentEventListener(TownyPolitics plugin, GovernmentManager govManager, TownGovernmentManager townGovManager) {
        this.plugin = plugin;
        this.govManager = govManager;
        this.townGovManager = townGovManager;
        this.logger = new DelegateLogger(plugin, "GovEventListener");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();
            addGovernmentComponent(event, nation);
        } catch (Exception e) {
            logger.severe("Error handling NationStatusScreenEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();
            addTownGovernmentComponent(event, town);
        } catch (Exception e) {
            logger.severe("Error handling TownStatusScreenEvent: " + e.getMessage());
        }
    }

    private void addGovernmentComponent(NationStatusScreenEvent event, Nation nation) {
        GovernmentType govType = govManager.getGovernmentType(nation);

        Component hoverText = EventHelper.buildHoverText("Government Information",
                "Type: " + govType.getDisplayName(),
                govType.getDescription());

        Component govComponent = EventHelper.createHoverComponent("Government", hoverText, NamedTextColor.GREEN);
        EventHelper.addComponentToScreen(event, "government_display", govComponent);
    }

    private void addTownGovernmentComponent(TownStatusScreenEvent event, Town town) {
        GovernmentType govType = townGovManager.getGovernmentType(town);

        Component hoverText = EventHelper.buildHoverText("Government Information",
                "Type: " + govType.getDisplayName(),
                govType.getDescription());

        Component govComponent = EventHelper.createHoverComponent("Government", hoverText, NamedTextColor.GREEN);
        EventHelper.addComponentToScreen(event, "government_display", govComponent);
    }
}