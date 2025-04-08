package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.EducationEffects;
import com.orbismc.townyPolitics.policy.PolicyEffects;
import com.orbismc.townyPolitics.storage.IPoliticalPowerStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoliticalPowerManager implements Manager {

    private final TownyPolitics plugin;
    private final IPoliticalPowerStorage storage;
    private final Map<UUID, Double> nationPP; // Cache of nation UUIDs to their political power
    private final DelegateLogger logger;

    // Maximum political power limit
    private final double MAX_PP = 1000.0;

    public PoliticalPowerManager(TownyPolitics plugin, IPoliticalPowerStorage storage, GovernmentManager govManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.nationPP = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "PPManager");

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        nationPP.clear();
        nationPP.putAll(storage.loadAllPP());
        logger.info("Loaded political power data for " + nationPP.size() + " nations");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved political power data to storage");
    }

    public double getPoliticalPower(Nation nation) {
        double pp = nationPP.getOrDefault(nation.getUUID(), 0.0);
        logger.fine("Nation " + nation.getName() + " has " + pp + " political power");
        return pp;
    }

    public void setPoliticalPower(Nation nation, double amount) {
        // Ensure PP can't go below 0 or above MAX_PP
        double newAmount = Math.min(MAX_PP, Math.max(0, amount));
        double oldAmount = nationPP.getOrDefault(nation.getUUID(), 0.0);

        nationPP.put(nation.getUUID(), newAmount);
        storage.savePP(nation.getUUID(), newAmount);

        logger.info("Nation " + nation.getName() + " political power set to " +
                newAmount + " (was " + oldAmount + ")");
    }

    public void addPoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        double newAmount = current + amount;

        logger.info("Adding " + amount + " political power to nation " +
                nation.getName() + " (new total: " + newAmount + ")");

        setPoliticalPower(nation, newAmount);
    }

    public boolean removePoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        if (current >= amount) {
            double newAmount = current - amount;

            logger.info("Removing " + amount + " political power from nation " +
                    nation.getName() + " (new total: " + newAmount + ")");

            setPoliticalPower(nation, newAmount);
            return true;
        }

        logger.warning("Cannot remove " + amount + " political power from nation " +
                nation.getName() + " (only has " + current + ")");
        return false;
    }

    public double getMaxPoliticalPower() {
        return MAX_PP;
    }

    public double calculateDailyPPGain(Nation nation) {
        int residents = nation.getNumResidents();
        double baseGain = plugin.getConfig().getDouble("political_power.base_gain", 1.0);
        double maxGain = plugin.getConfig().getDouble("political_power.max_daily_gain", 5.0);
        double minGain = plugin.getConfig().getDouble("political_power.min_daily_gain", 1.0);

        double ppGain;
        if (residents <= 0) {
            ppGain = 0; // No residents, no PP
        } else if (residents == 1) {
            ppGain = baseGain; // 1 resident = base_gain PP
        } else if (residents <= 5) {
            ppGain = baseGain + (residents - 1) * 0.125 * baseGain;
        } else if (residents <= 10) {
            ppGain = 1.5 * baseGain + (residents - 5) * 0.1 * baseGain;
        } else {
            ppGain = Math.min(maxGain, 2.0 * baseGain + Math.log10(residents / 10.0) * 2 * baseGain);
        }

        // Apply corruption modifiers
        CorruptionManager corruptionManager = plugin.getCorruptionManager();
        double ppModifier = corruptionManager.getPoliticalPowerModifier(nation);
        ppGain *= ppModifier;

        // Apply education effects if available
        if (plugin.getEffectsManager() != null) {
            EducationEffects educationEffects = plugin.getEffectsManager().getNationEducationEffects(nation);
            double educationModifier = educationEffects.getPpGainModifier();
            ppGain *= educationModifier;

            logger.fine("Nation " + nation.getName() + " education modifier applied to PP gain: " +
                    educationModifier + " (new gain: " + ppGain + ")");
        }

        // Apply policy modifiers
        if (plugin.getPolicyManager() != null) {
            PolicyEffects effects = plugin.getPolicyManager().getCombinedPolicyEffects(nation);
            ppGain *= effects.getPoliticalPowerGainModifier();
        }

        double finalGain = Math.min(maxGain, Math.max(minGain, ppGain));

        logger.fine("Nation " + nation.getName() + " daily PP gain calculation: " +
                "residents=" + residents + ", baseGain=" + baseGain +
                ", calculatedGain=" + ppGain + ", finalGain=" + finalGain);

        return finalGain;
    }

    public void processNewDay() {
        logger.info("Processing daily political power gains for all nations");

        plugin.getTownyAPI().getNations().forEach(nation -> {
            double gain = calculateDailyPPGain(nation);
            addPoliticalPower(nation, gain);
            logger.info("Nation " + nation.getName() + " gained " +
                    String.format("%.2f", gain) + " political power");
        });
    }
}