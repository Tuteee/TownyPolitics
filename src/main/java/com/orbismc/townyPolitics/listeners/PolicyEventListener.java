package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PolicyManager;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.utils.EventHelper;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Set;

public class PolicyEventListener implements Listener {
    private final TownyPolitics plugin;
    private final PolicyManager policyManager;
    private final DelegateLogger logger;

    public PolicyEventListener(TownyPolitics plugin, PolicyManager policyManager) {
        this.plugin = plugin;
        this.policyManager = policyManager;
        this.logger = new DelegateLogger(plugin, "PolicyEventListener");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();
            addPoliciesComponent(event, nation);
        } catch (Exception e) {
            logger.severe("Error handling NationStatusScreenEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();
            addTownPoliciesComponent(event, town);
        } catch (Exception e) {
            logger.severe("Error handling TownStatusScreenEvent: " + e.getMessage());
        }
    }

    private void addPoliciesComponent(NationStatusScreenEvent event, Nation nation) {
        Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(nation);
        if (activePolicies.isEmpty()) {
            return; // No policies to display
        }

        // Start building the hover text
        Component.Builder hoverBuilder = Component.text()
                .append(Component.text("Active Policies").color(NamedTextColor.DARK_GREEN))
                .append(Component.newline())
                .append(Component.newline());

        // Add each policy to the hover text
        for (ActivePolicy policy : activePolicies) {
            Policy policyDef = policyManager.getPolicy(policy.getPolicyId());
            if (policyDef == null) continue;

            hoverBuilder.append(Component.text("• " + policyDef.getName()).color(NamedTextColor.GREEN))
                    .append(Component.text(" (" + policy.formatRemainingTime() + ")").color(NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        Component hoverText = hoverBuilder.build();
        Component policiesComponent = EventHelper.createHoverComponent(
                "Policies (" + activePolicies.size() + ")",
                hoverText,
                NamedTextColor.BLUE
        );

        EventHelper.addComponentToScreen(event, "policies_display", policiesComponent);
    }

    private void addTownPoliciesComponent(TownStatusScreenEvent event, Town town) {
        Set<ActivePolicy> activePolicies = policyManager.getActivePolicies(town);
        if (activePolicies.isEmpty()) {
            return; // No policies to display
        }

        // Start building the hover text
        Component.Builder hoverBuilder = Component.text()
                .append(Component.text("Active Policies").color(NamedTextColor.DARK_GREEN))
                .append(Component.newline())
                .append(Component.newline());

        // Add each policy to the hover text
        for (ActivePolicy policy : activePolicies) {
            Policy policyDef = policyManager.getPolicy(policy.getPolicyId());
            if (policyDef == null) continue;

            hoverBuilder.append(Component.text("• " + policyDef.getName()).color(NamedTextColor.GREEN))
                    .append(Component.text(" (" + policy.formatRemainingTime() + ")").color(NamedTextColor.GRAY))
                    .append(Component.newline());
        }

        Component hoverText = hoverBuilder.build();
        Component policiesComponent = EventHelper.createHoverComponent(
                "Policies (" + activePolicies.size() + ")",
                hoverText,
                NamedTextColor.BLUE
        );

        EventHelper.addComponentToScreen(event, "town_policies_display", policiesComponent);
    }
}