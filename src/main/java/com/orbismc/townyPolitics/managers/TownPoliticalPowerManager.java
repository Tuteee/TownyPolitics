package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.ITownPoliticalPowerStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownPoliticalPowerManager {

    private final TownyPolitics plugin;
    private final ITownPoliticalPowerStorage storage;
    private final Map<UUID, Double> townPP; // Cache of town UUIDs to their political power
    private final DelegateLogger logger;

    // Maximum political power limit
    private final double MAX_PP = 1000.0;

    public TownPoliticalPowerManager(TownyPolitics plugin, ITownPoliticalPowerStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.townPP = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "TownPPManager");

        // Load data from storage
        loadData();
    }

    public void loadData() {
        townPP.clear();
        townPP.putAll(storage.loadAllPP());
        logger.info("Loaded political power data for " + townPP.size() + " towns");
    }

    public double getPoliticalPower(Town town) {
        double pp = townPP.getOrDefault(town.getUUID(), 0.0);
        logger.fine("Town " + town.getName() + " has " + pp + " political power");
        return pp;
    }

    public void setPoliticalPower(Town town, double amount) {
        // Ensure PP can't go below 0 or above MAX_PP
        double newAmount = Math.min(MAX_PP, Math.max(0, amount));
        double oldAmount = townPP.getOrDefault(town.getUUID(), 0.0);

        townPP.put(town.getUUID(), newAmount);
        storage.savePP(town.getUUID(), newAmount);

        logger.info("Town " + town.getName() + " political power set to " +
                newAmount + " (was " + oldAmount + ")");
    }

    public void addPoliticalPower(Town town, double amount) {
        double current = getPoliticalPower(town);
        double newAmount = current + amount;

        logger.info("Adding " + amount + " political power to town " +
                town.getName() + " (new total: " + newAmount + ")");

        setPoliticalPower(town, newAmount);
    }

    public boolean removePoliticalPower(Town town, double amount) {
        double current = getPoliticalPower(town);
        if (current >= amount) {
            double newAmount = current - amount;

            logger.info("Removing " + amount + " political power from town " +
                    town.getName() + " (new total: " + newAmount + ")");

            setPoliticalPower(town, newAmount);
            return true;
        }

        logger.warning("Cannot remove " + amount + " political power from town " +
                town.getName() + " (only has " + current + ")");
        return false;
    }

    public double getMaxPoliticalPower() {
        return MAX_PP;
    }

    public double calculateDailyPPGain(Town town) {
        int residents = town.getResidents().size();
        double baseGain = plugin.getConfig().getDouble("town_political_power.base_gain", 0.8);
        double maxGain = plugin.getConfig().getDouble("town_political_power.max_daily_gain", 4.0);
        double minGain = plugin.getConfig().getDouble("town_political_power.min_daily_gain", 0.8);

        double ppGain;
        if (residents <= 0) {
            ppGain = 0; // No residents, no PP
        } else if (residents == 1) {
            ppGain = baseGain; // 1 resident = base_gain PP
        } else if (residents <= 5) {
            ppGain = baseGain + (residents - 1) * 0.1 * baseGain;
        } else if (residents <= 10) {
            ppGain = 1.4 * baseGain + (residents - 5) * 0.08 * baseGain;
        } else {
            ppGain = Math.min(maxGain, 1.8 * baseGain + Math.log10(residents / 10.0) * 1.5 * baseGain);
        }

        // Apply any modifiers from government type or other systems
        // This would be implemented in the future

        double finalGain = Math.min(maxGain, Math.max(minGain, ppGain));

        logger.fine("Town " + town.getName() + " daily PP gain calculation: " +
                "residents=" + residents + ", baseGain=" + baseGain +
                ", calculatedGain=" + ppGain + ", finalGain=" + finalGain);

        return finalGain;
    }

    public void processNewDay() {
        logger.info("Processing daily political power gains for all towns");

        plugin.getTownyAPI().getTowns().forEach(town -> {
            double gain = calculateDailyPPGain(town);
            addPoliticalPower(town, gain);
            logger.info("Town " + town.getName() + " gained " +
                    String.format("%.2f", gain) + " political power");
        });
    }
}