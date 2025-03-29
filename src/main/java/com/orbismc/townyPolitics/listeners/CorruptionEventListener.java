package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.managers.TownCorruptionManager;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.orbismc.townyPolitics.utils.EventHelper;
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CorruptionEventListener implements Listener {
    private final TownyPolitics plugin;
    private final CorruptionManager corruptionManager;
    private final TownCorruptionManager townCorruptionManager;
    private final PoliticalPowerManager ppManager;
    private final DelegateLogger logger;

    public CorruptionEventListener(TownyPolitics plugin, CorruptionManager corruptionManager,
                                   TownCorruptionManager townCorruptionManager, PoliticalPowerManager ppManager) {
        this.plugin = plugin;
        this.corruptionManager = corruptionManager;
        this.townCorruptionManager = townCorruptionManager;
        this.ppManager = ppManager;
        this.logger = new DelegateLogger(plugin, "CorruptEventListener");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNationStatusScreen(NationStatusScreenEvent event) {
        try {
            Nation nation = event.getNation();
            addCorruptionComponent(event, nation);
        } catch (Exception e) {
            logger.severe("Error handling NationStatusScreenEvent: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTownStatusScreen(TownStatusScreenEvent event) {
        try {
            Town town = event.getTown();
            addTownCorruptionComponent(event, town);
        } catch (Exception e) {
            logger.severe("Error handling TownStatusScreenEvent: " + e.getMessage());
        }
    }

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

        // Get threshold level and color
        int thresholdLevel = corruptionManager.getCorruptionThresholdLevel(nation);
        String thresholdName = corruptionManager.getCorruptionThresholdName(thresholdLevel);
        NamedTextColor corruptColor = getColorForThreshold(thresholdLevel);

        Component hoverText = EventHelper.buildHoverText("Corruption Information",
                "Status: " + thresholdName,
                "Current Level: " + String.format("%.1f%%", corruption),
                "Daily Change: +" + String.format("%.2f%%", dailyCorruptionGain),
                "",
                "Effects:",
                "• Tax Collection: " + String.format("%+.1f%%", -corruption * 5),
                "• Political Power Gain: " + ppModStr,
                "• Resource Output: " + resourceModStr,
                "• Spending Costs: " + spendingModStr);

        Component corruptComponent = EventHelper.createHoverComponent("Corruption", hoverText, corruptColor);
        EventHelper.addComponentToScreen(event, "corruption_display", corruptComponent);
    }

    private void addTownCorruptionComponent(TownStatusScreenEvent event, Town town) {
        double corruption = townCorruptionManager.getCorruption(town);
        double dailyCorruptionGain = townCorruptionManager.calculateDailyCorruptionGain(town);

        // Get modifiers
        double taxMod = townCorruptionManager.getTaxationModifier(town);
        double tradeMod = townCorruptionManager.getTradeModifier(town);

        // Format modifiers as percentages
        String taxModStr = String.format("%+.1f%%", (taxMod - 1.0) * 100);
        String tradeModStr = String.format("%+.1f%%", (tradeMod - 1.0) * 100);

        // Get threshold level and color
        int thresholdLevel = townCorruptionManager.getCorruptionThresholdLevel(town);
        String thresholdName = townCorruptionManager.getCorruptionThresholdName(thresholdLevel);
        NamedTextColor corruptColor = getColorForThreshold(thresholdLevel);

        Component hoverText = EventHelper.buildHoverText("Town Corruption Information",
                "Status: " + thresholdName,
                "Current Level: " + String.format("%.1f%%", corruption),
                "Daily Change: +" + String.format("%.2f%%", dailyCorruptionGain),
                "",
                "Effects:",
                "• Tax Income: " + taxModStr,
                "• Trade Income: " + tradeModStr);

        Component corruptComponent = EventHelper.createHoverComponent("Corruption", hoverText, corruptColor);
        EventHelper.addComponentToScreen(event, "town_corruption_display", corruptComponent);
    }

    private NamedTextColor getColorForThreshold(int thresholdLevel) {
        return switch (thresholdLevel) {
            case 0 -> NamedTextColor.GREEN;     // Minimal
            case 1 -> NamedTextColor.YELLOW;    // Low
            case 2 -> NamedTextColor.GOLD;      // Medium
            case 3 -> NamedTextColor.RED;       // High
            case 4 -> NamedTextColor.DARK_RED;  // Critical
            default -> NamedTextColor.GREEN;
        };
    }
}