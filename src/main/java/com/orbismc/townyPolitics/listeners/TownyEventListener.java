package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;

    private static final String STORAGE_KEY = "townypolitics_storage";
    private static final String POLITICAL_POWER_KEY = "political_power";

    public TownyEventListener(TownyPolitics plugin, PoliticalPowerManager ppManager) {
        this.plugin = plugin;
        this.ppManager = ppManager;
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        plugin.getLogger().info("Processing new day for political power distribution...");
        ppManager.processNewDay();
        plugin.getLogger().info("Political power distribution complete.");
    }

    public void updateNationPoliticalPowerMetadata(Nation nation) {
        try {
            // Remove Towny's default political power display
            if (nation.hasMeta(POLITICAL_POWER_KEY)) {
                nation.removeMetaData(nation.getMetadata(POLITICAL_POWER_KEY));
            }

            double currentPP = ppManager.getPoliticalPower(nation);
            double dailyGain = ppManager.calculateDailyPPGain(nation);
            String data = String.format("%.2f|%.2f", currentPP, dailyGain);

            if (nation.hasMeta(STORAGE_KEY)) {
                nation.removeMetaData(nation.getMetadata(STORAGE_KEY));
            }
            nation.addMetaData(new StringDataField(STORAGE_KEY, data));
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating nation metadata: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST) // Run first, before Towny adds components
    public void onNationStatusScreenEarly(NationStatusScreenEvent event) {
        // Just make sure we clear any default Political Power display
        updateNationPoliticalPowerMetadata(event.getNation());
    }

    @EventHandler(priority = EventPriority.MONITOR) // Run at the very end
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();
            double currentPP = ppManager.getPoliticalPower(nation);
            double dailyGain = ppManager.calculateDailyPPGain(nation);
            int residents = nation.getNumResidents();

            // Create hover text components
            Component hoverText = Component.text("Political Power Details")
                    .color(NamedTextColor.GREEN)
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Current Power: " + String.format("%.2f", currentPP))
                            .color(NamedTextColor.DARK_GREEN))
                    .append(Component.newline())
                    .append(Component.text("Daily Gain: +" + String.format("%.2f", dailyGain) + "/day")
                            .color(NamedTextColor.DARK_GREEN))
                    .append(Component.newline())
                    .append(Component.text("Residents: " + residents)
                            .color(NamedTextColor.DARK_GREEN));

            // Create the Political Power component
            Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
            Component powerText = Component.text("Political Power").color(NamedTextColor.GREEN);
            Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

            Component ppComponent = Component.empty()
                    .append(openBracket)
                    .append(powerText)
                    .append(closeBracket)
                    .hoverEvent(HoverEvent.showText(hoverText));

            // Try to find and fix the Religion component if needed
            try {
                if (event.getStatusScreen().hasComponent("religion")) {
                    // Get the religion component
                    Component religion = event.getStatusScreen().getComponentOrNull("religion");

                    // Create a new religion component without the Political Power part
                    Component newReligion = religion;

                    // Replace the religion component
                    event.getStatusScreen().replaceComponent("religion", newReligion);
                }
            } catch (Exception e) {
                plugin.getLogger().info("Could not modify religion component: " + e.getMessage());
            }

            // Add our political power component with a unique key
            event.getStatusScreen().addComponentOf("political_power_display", ppComponent);

        } catch (Exception e) {
            plugin.getLogger().warning("Error adding hoverable component: " + e.getMessage());
            e.printStackTrace();
        }
    }
}