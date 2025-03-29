package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;

/**
 * Component that displays Political Power information on status screens
 */
public class PoliticalPowerComponent extends StatusComponent {

    private static final String STORAGE_KEY = "townypolitics_storage";
    private static final String POLITICAL_POWER_KEY = "political_power";

    public PoliticalPowerComponent(TownyPolitics plugin) {
        super(plugin, "PP");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        double currentPP = plugin.getPPManager().getPoliticalPower(nation);
        double dailyGain = plugin.getPPManager().calculateDailyPPGain(nation);
        int residents = nation.getNumResidents();

        // Create hover text component
        Component hoverText = Component.text("Political Power Details")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Current Power: " + String.format("%.2f", currentPP))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Daily Gain: +" + String.format("%.2f", dailyGain) + "/day")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Residents: " + residents)
                        .color(NamedTextColor.GREEN));

        // Create the Political Power component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component powerText = Component.text("Political Power").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component ppComponent = Component.empty()
                .append(openBracket)
                .append(powerText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "political_power_display", ppComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        double currentPP = plugin.getTownPPManager().getPoliticalPower(town);
        double dailyGain = plugin.getTownPPManager().calculateDailyPPGain(town);
        int residents = town.getResidents().size();

        // Create hover text component
        Component hoverText = Component.text("Town Political Power Details")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Current Power: " + String.format("%.2f", currentPP))
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Daily Gain: +" + String.format("%.2f", dailyGain) + "/day")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("Residents: " + residents)
                        .color(NamedTextColor.GREEN));

        // Create the Political Power component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component powerText = Component.text("Political Power").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component ppComponent = Component.empty()
                .append(openBracket)
                .append(powerText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "town_political_power_display", ppComponent);
    }

    /**
     * Update nation's political power metadata to remove default display
     */
    public void updateNationPoliticalPowerMetadata(Nation nation) {
        try {
            // Remove Towny's default political power display
            if (nation.hasMeta(POLITICAL_POWER_KEY)) {
                nation.removeMetaData(nation.getMetadata(POLITICAL_POWER_KEY));
            }

            double currentPP = plugin.getPPManager().getPoliticalPower(nation);
            double dailyGain = plugin.getPPManager().calculateDailyPPGain(nation);
            String data = String.format("%.2f|%.2f", currentPP, dailyGain);

            if (nation.hasMeta(STORAGE_KEY)) {
                nation.removeMetaData(nation.getMetadata(STORAGE_KEY));
            }
            nation.addMetaData(new StringDataField(STORAGE_KEY, data));
        } catch (Exception e) {
            logger.warning("Error updating nation metadata: " + e.getMessage());
        }
    }
}