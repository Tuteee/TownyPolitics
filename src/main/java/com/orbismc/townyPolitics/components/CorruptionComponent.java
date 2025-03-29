package com.orbismc.townyPolitics.components;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;

/**
 * Component that displays Corruption information on status screens
 */
public class CorruptionComponent extends StatusComponent {

    public CorruptionComponent(TownyPolitics plugin) {
        super(plugin, "Corruption");
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        // Get corruption data
        double corruption = plugin.getCorruptionManager().getCorruption(nation);
        double dailyCorruptionGain = plugin.getCorruptionManager().calculateDailyCorruptionGain(nation);

        // Get modifiers
        double taxMod = plugin.getCorruptionManager().getTaxationModifier(nation);
        double ppMod = plugin.getCorruptionManager().getPoliticalPowerModifier(nation);
        double resourceMod = plugin.getCorruptionManager().getResourceModifier(nation);
        double spendingMod = plugin.getCorruptionManager().getSpendingModifier(nation);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String ppModStr = String.format("%+.1f%%", (ppMod - 1.0) * 100);
        String resourceModStr = String.format("%+.1f%%", (resourceMod - 1.0) * 100);
        String spendingModStr = String.format("%+.1f%%", (spendingMod - 1.0) * 100);

        // Get corruption threshold level and determine color
        int thresholdLevel = plugin.getCorruptionManager().getCorruptionThresholdLevel(nation);
        NamedTextColor corruptColor = getCorruptionColorForThreshold(thresholdLevel);

        // Get threshold name
        String thresholdName = plugin.getCorruptionManager().getCorruptionThresholdName(thresholdLevel);

        // Create hover text component
        Component hoverText = Component.text("Corruption Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Status: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(thresholdName)
                        .color(corruptColor))
                .append(Component.newline())
                .append(Component.text("Current Level: " + String.format("%.1f%%", corruption))
                        .color(corruptColor))
                .append(Component.newline())
                .append(Component.text("Daily Change: +" + String.format("%.2f%%", dailyCorruptionGain))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Effects:")
                        .color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("• Tax Collection: " + String.format("%+.1f%%", -corruption * 5))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("• Political Power Gain: " + ppModStr)
                        .color(getTextColorForValue(ppMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Resource Output: " + resourceModStr)
                        .color(getTextColorForValue(resourceMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Spending Costs: " + spendingModStr)
                        .color(getTextColorForValue(spendingMod - 1.0, true)));

        // Create the Corruption component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component corruptText = Component.text("Corruption").color(corruptColor);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component corruptComponent = Component.empty()
                .append(openBracket)
                .append(corruptText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "corruption_display", corruptComponent);
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        // Get corruption data
        double corruption = plugin.getTownCorruptionManager().getCorruption(town);
        double dailyCorruptionGain = plugin.getTownCorruptionManager().calculateDailyCorruptionGain(town);

        // Get modifiers
        double taxMod = plugin.getTownCorruptionManager().getTaxationModifier(town);
        double tradeMod = plugin.getTownCorruptionManager().getTradeModifier(town);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String tradeModStr = String.format("%+.1f%%", (tradeMod - 1.0) * 100);

        // Get corruption threshold level and determine color
        int thresholdLevel = plugin.getTownCorruptionManager().getCorruptionThresholdLevel(town);
        NamedTextColor corruptColor = getCorruptionColorForThreshold(thresholdLevel);

        // Get threshold name
        String thresholdName = plugin.getTownCorruptionManager().getCorruptionThresholdName(thresholdLevel);

        // Create hover text component
        Component hoverText = Component.text("Town Corruption Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Status: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text(thresholdName)
                        .color(corruptColor))
                .append(Component.newline())
                .append(Component.text("Current Level: " + String.format("%.1f%%", corruption))
                        .color(corruptColor))
                .append(Component.newline())
                .append(Component.text("Daily Change: +" + String.format("%.2f%%", dailyCorruptionGain))
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Effects:")
                        .color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("• Tax Income: " + taxModStr)
                        .color(getTextColorForValue(taxMod - 1.0, false)))
                .append(Component.newline())
                .append(Component.text("• Trade Income: " + tradeModStr)
                        .color(getTextColorForValue(tradeMod - 1.0, false)));

        // Create the Corruption component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component corruptText = Component.text("Corruption").color(corruptColor);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component corruptComponent = Component.empty()
                .append(openBracket)
                .append(corruptText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        addComponentToScreen(event, "town_corruption_display", corruptComponent);
    }

    /**
     * Get the appropriate color for a corruption threshold level
     */
    private NamedTextColor getCorruptionColorForThreshold(int thresholdLevel) {
        return switch (thresholdLevel) {
            case 0 -> NamedTextColor.GREEN;       // Minimal
            case 1 -> NamedTextColor.YELLOW;      // Low
            case 2 -> NamedTextColor.GOLD;        // Medium
            case 3 -> NamedTextColor.RED;         // High
            case 4 -> NamedTextColor.DARK_RED;    // Critical
            default -> NamedTextColor.GREEN;
        };
    }
}