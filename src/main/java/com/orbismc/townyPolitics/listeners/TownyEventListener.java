package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.metadata.StringDataField;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.event.HoverEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class TownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;
    private final GovernmentManager govManager;
    private final CorruptionManager corruptionManager;

    private static final String STORAGE_KEY = "townypolitics_storage";
    private static final String POLITICAL_POWER_KEY = "political_power";
    private static final String GOVERNMENT_KEY = "government_type";
    private static final String CORRUPTION_KEY = "corruption";

    public TownyEventListener(TownyPolitics plugin, PoliticalPowerManager ppManager, CorruptionManager corruptionManager) {
        this.plugin = plugin;
        this.ppManager = ppManager;
        this.govManager = plugin.getGovManager();
        this.corruptionManager = corruptionManager;
    }

    @EventHandler
    public void onNewDay(NewDayEvent event) {
        plugin.getLogger().info("Processing new day for political power and corruption...");
        ppManager.processNewDay();
        corruptionManager.processNewDay();
        plugin.getLogger().info("Political power and corruption processing complete.");
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onNationStatusScreenEarly(NationStatusScreenEvent event) {
        // Just make sure we clear any default Political Power display
        updateNationPoliticalPowerMetadata(event.getNation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();

            // Add Political Power component
            addPoliticalPowerComponent(event, nation);

            // Add Government Type component
            addGovernmentComponent(event, nation);

            // Add Corruption component
            addCorruptionComponent(event, nation);

        } catch (Exception e) {
            plugin.getLogger().warning("Error adding components to nation status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();

            // Add Government Type component for towns
            addTownGovernmentComponent(event, town);

        } catch (Exception e) {
            plugin.getLogger().warning("Error adding components to town status screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add political power component to nation status screen
     *
     * @param event The event
     * @param nation The nation
     */
    private void addPoliticalPowerComponent(NationStatusScreenEvent event, Nation nation) {
        double currentPP = ppManager.getPoliticalPower(nation);
        double dailyGain = ppManager.calculateDailyPPGain(nation);
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
        event.getStatusScreen().addComponentOf("political_power_display", ppComponent);
    }

    /**
     * Add government type component to nation status screen
     *
     * @param event The event
     * @param nation The nation
     */
    private void addGovernmentComponent(NationStatusScreenEvent event, Nation nation) {
        GovernmentType govType = govManager.getGovernmentType(nation);

        // Create hover text component
        Component hoverText = Component.text("Government Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Type: " + govType.getDisplayName())
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text(govType.getDescription())
                        .color(NamedTextColor.GREEN));

        // Create the Government component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component govText = Component.text("Government").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component govComponent = Component.empty()
                .append(openBracket)
                .append(govText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        event.getStatusScreen().addComponentOf("government_display", govComponent);
    }

    /**
     * Add corruption component to nation status screen
     *
     * @param event The event
     * @param nation The nation
     */
    private void addCorruptionComponent(NationStatusScreenEvent event, Nation nation) {
        double corruption = corruptionManager.getCorruption(nation);
        double dailyCorruptionGain = corruptionManager.calculateDailyCorruptionGain(nation);

        // Get modifiers
        double taxMod = corruptionManager.getTaxationModifier(nation);
        double ppMod = corruptionManager.getPoliticalPowerModifier(nation);
        double resourceMod = corruptionManager.getResourceModifier(nation);
        double spendingMod = corruptionManager.getSpendingModifier(nation);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String ppModStr = String.format("%+.1f%%", (ppMod - 1.0) * 100);
        String resourceModStr = String.format("%+.1f%%", (resourceMod - 1.0) * 100);
        String spendingModStr = String.format("%+.1f%%", (spendingMod - 1.0) * 100);

        // Get corruption threshold level and determine color
        int thresholdLevel = corruptionManager.getCorruptionThresholdLevel(nation);
        NamedTextColor corruptColor = switch (thresholdLevel) {
            case 0 -> NamedTextColor.GREEN;       // Minimal
            case 1 -> NamedTextColor.YELLOW;      // Low
            case 2 -> NamedTextColor.GOLD;        // Medium
            case 3 -> NamedTextColor.RED;         // High
            case 4 -> NamedTextColor.DARK_RED;    // Critical
            default -> NamedTextColor.GREEN;
        };

        // Get threshold name
        String thresholdName = corruptionManager.getCorruptionThresholdName(thresholdLevel);

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
                .append(Component.text("• Max Taxation: " + taxModStr)
                        .color(getTextColorForValue(taxMod - 1.0)))
                .append(Component.newline())
                .append(Component.text("• Political Power Gain: " + ppModStr)
                        .color(getTextColorForValue(ppMod - 1.0)))
                .append(Component.newline())
                .append(Component.text("• Resource Output: " + resourceModStr)
                        .color(getTextColorForValue(resourceMod - 1.0)))
                .append(Component.newline())
                .append(Component.text("• Spending Costs: " + spendingModStr)
                        .color(getTextColorForValue(spendingMod - 1.0)));

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
        event.getStatusScreen().addComponentOf("corruption_display", corruptComponent);
    }

    /**
     * Add corruption component to town status screen
     *
     * @param event The event
     * @param town The town
     */
    private void addTownCorruptionComponent(TownStatusScreenEvent event, Town town) {
        double corruption = 0.0;

        // Determine text color based on corruption level
        NamedTextColor corruptColor;
        if (corruption >= 75) corruptColor = NamedTextColor.DARK_RED;
        else if (corruption >= 50) corruptColor = NamedTextColor.RED;
        else if (corruption >= 25) corruptColor = NamedTextColor.YELLOW;
        else if (corruption == 0) corruptColor  = NamedTextColor.GREEN;
        else corruptColor = NamedTextColor.GREEN;

        // Create hover text component
        Component hoverText = Component.text("Corruption Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Current Corruption Level: " + String.format("%.1f%%", corruption))
                        .color(corruptColor));

        // Create the Corruption component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component corruptText = Component.text("Corruption").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component corruptComponent = Component.empty()
                .append(openBracket)
                .append(corruptText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        event.getStatusScreen().addComponentOf("corruption_display", corruptComponent);
    }

    /**
     * Add government type component to town status screen
     *
     * @param event The event
     * @param town The town
     */
    private void addTownGovernmentComponent(TownStatusScreenEvent event, Town town) {
        GovernmentType govType = govManager.getGovernmentType(town);

        // Create hover text component
        Component hoverText = Component.text("Government Information")
                .color(NamedTextColor.DARK_GREEN)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Type: " + govType.getDisplayName())
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text(govType.getDescription())
                        .color(NamedTextColor.GREEN));

        // Create the Government component
        Component openBracket = Component.text("[").color(NamedTextColor.GRAY);
        Component govText = Component.text("Government").color(NamedTextColor.GREEN);
        Component closeBracket = Component.text("]").color(NamedTextColor.GRAY);

        Component govComponent = Component.empty()
                .append(openBracket)
                .append(govText)
                .append(closeBracket)
                .hoverEvent(HoverEvent.showText(hoverText));

        // Add to status screen
        event.getStatusScreen().addComponentOf("government_display", govComponent);
    }

    private ChatColor getColorForModifier(double modifier) {
        if (modifier > 1.0) return ChatColor.RED;
        if (modifier < 1.0) return ChatColor.RED;
        return ChatColor.GREEN;
    }

    /**
     * Get appropriate text color for a value
     * @param value The value to check
     * @return Appropriate color (RED for negative, GREEN for positive or zero)
     */
    private NamedTextColor getTextColorForValue(double value) {
        if (value < 0) return NamedTextColor.RED;
        return NamedTextColor.GREEN;
    }
}