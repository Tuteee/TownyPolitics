package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.CorruptionStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorruptionManager {

    private final TownyPolitics plugin;
    private final CorruptionStorage storage;
    private final GovernmentManager govManager;

    // Cache of nation/town UUIDs to their corruption levels
    private final Map<UUID, Double> nationCorruption;
    private final Map<UUID, Double> townCorruption;

    // Maximum corruption level (100% is completely corrupt)
    private final double MAX_CORRUPTION = 100.0;

    // Critical corruption threshold where negative effects become severe
    private final double CRITICAL_THRESHOLD;

    public CorruptionManager(TownyPolitics plugin, CorruptionStorage storage, GovernmentManager govManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.govManager = govManager;
        this.nationCorruption = new HashMap<>();
        this.townCorruption = new HashMap<>();

        // Load configuration
        this.CRITICAL_THRESHOLD = plugin.getConfig().getDouble("corruption.critical_threshold", 75.0);

        // Load data from storage
        loadData();
    }

    public void loadData() {
        nationCorruption.clear();
        townCorruption.clear();
        nationCorruption.putAll(storage.loadAllCorruption(true));
        townCorruption.putAll(storage.loadAllCorruption(false));
    }

    public double getCorruption(Nation nation) {
        return nationCorruption.getOrDefault(nation.getUUID(), 0.0);
    }

    public double getCorruption(Town town) {
        return townCorruption.getOrDefault(town.getUUID(), 0.0);
    }

    public void setCorruption(Nation nation, double amount) {
        // Ensure corruption can't go below 0 or above MAX_CORRUPTION
        double newAmount = Math.min(MAX_CORRUPTION, Math.max(0, amount));
        nationCorruption.put(nation.getUUID(), newAmount);
        storage.saveCorruption(nation.getUUID(), newAmount, true);
    }

    public void setCorruption(Town town, double amount) {
        // Ensure corruption can't go below 0 or above MAX_CORRUPTION
        double newAmount = Math.min(MAX_CORRUPTION, Math.max(0, amount));
        townCorruption.put(town.getUUID(), newAmount);
        storage.saveCorruption(town.getUUID(), newAmount, false);
    }

    public void addCorruption(Nation nation, double amount) {
        double current = getCorruption(nation);
        setCorruption(nation, current + amount);
    }

    public void addCorruption(Town town, double amount) {
        double current = getCorruption(town);
        setCorruption(town, current + amount);
    }

    public void reduceCorruption(Nation nation, double amount) {
        double current = getCorruption(nation);
        setCorruption(nation, current - amount);
    }

    public void reduceCorruption(Town town, double amount) {
        double current = getCorruption(town);
        setCorruption(town, current - amount);
    }

    public boolean isCorruptionCritical(Nation nation) {
        return getCorruption(nation) >= CRITICAL_THRESHOLD;
    }

    public boolean isCorruptionCritical(Town town) {
        return getCorruption(town) >= CRITICAL_THRESHOLD;
    }

    /**
     * Get the modifier for taxation based on corruption level
     * Higher corruption = higher potential maximum taxation
     *
     * @param nation The nation
     * @return The taxation modifier (0.0 to 1.0 representing 0-100%)
     */
    public double getTaxationModifier(Nation nation) {
        double corruption = getCorruption(nation);
        // At 0% corruption, no effect
        // At 100% corruption, +20% max taxation
        return 1.0 + (corruption / 100.0 * 0.2);
    }

    /**
     * Get the modifier for political power gain based on corruption level
     * Higher corruption = lower PP gain
     *
     * @param nation The nation
     * @return The PP gain modifier (0.0 to 1.0)
     */
    public double getPoliticalPowerModifier(Nation nation) {
        double corruption = getCorruption(nation);
        // At 0% corruption, no effect
        // At 100% corruption, -35% political power gain
        return 1.0 - (corruption / 100.0 * 0.35);
    }

    /**
     * Get the modifier for resource output based on corruption level
     * Higher corruption = lower resource output
     *
     * @param nation The nation
     * @return The resource modifier (0.0 to 1.0)
     */
    public double getResourceModifier(Nation nation) {
        double corruption = getCorruption(nation);
        // At 0% corruption, no effect
        // At 100% corruption, -25% resource output
        return 1.0 - (corruption / 100.0 * 0.25);
    }

    /**
     * Get the modifier for spending based on corruption level
     * Higher corruption = higher spending
     *
     * @param nation The nation
     * @return The spending modifier (1.0 or higher)
     */
    public double getSpendingModifier(Nation nation) {
        double corruption = getCorruption(nation);
        // At 0% corruption, no effect
        // At 100% corruption, +40% spending
        return 1.0 + (corruption / 100.0 * 0.4);
    }

    /**
     * Calculate daily corruption gain for a nation based on its government type,
     * security spending, and other factors.
     *
     * @param nation The nation
     * @return The daily corruption gain
     */
    public double calculateDailyCorruptionGain(Nation nation) {
        // Base daily corruption gain from config
        double baseGain = plugin.getConfig().getDouble("corruption.base_daily_gain", 0.5);

        // Government type modifier
        GovernmentType govType = govManager.getGovernmentType(nation);
        double govModifier = getGovernmentTypeModifier(govType);

        // For a full implementation, you would factor in security spending here
        // For now, we'll use a placeholder
        double securitySpending = 0.0; // Will be implemented in future
        double securityModifier = 1.0 - (securitySpending * 0.1); // 10% reduction per level

        // Calculate final gain (minimum 0)
        return Math.max(0, baseGain * govModifier * securityModifier);
    }

    /**
     * Get corruption modifier based on government type
     *
     * @param govType The government type
     * @return The corruption gain multiplier
     */
    private double getGovernmentTypeModifier(GovernmentType govType) {
        return switch (govType) {
            case AUTOCRACY -> 1.5;    // +50% corruption gain
            case OLIGARCHY -> 1.2;    // +20% corruption gain
            case REPUBLIC -> 0.9;     // -10% corruption gain
            case CONSTITUTIONAL_MONARCHY -> 0.8; // -20% corruption gain
            case DIRECT_DEMOCRACY -> 0.6; // -40% corruption gain
            default -> 1.0;
        };
    }

    /**
     * Process daily corruption changes for all nations
     */
    public void processNewDay() {
        plugin.getTownyAPI().getNations().forEach(nation -> {
            double gain = calculateDailyCorruptionGain(nation);
            addCorruption(nation, gain);
            plugin.getLogger().info("Nation " + nation.getName() + " gained " +
                    String.format("%.2f", gain) + " corruption, now at " +
                    String.format("%.2f", getCorruption(nation)) + "%");

            // Apply additional effects if corruption is at critical level
            if (isCorruptionCritical(nation)) {
                applyCorruptionEffects(nation);
            }
        });
    }

    /**
     * Apply negative effects when corruption reaches critical levels
     *
     * @param nation The nation
     */
    private void applyCorruptionEffects(Nation nation) {
        // Implement critical corruption effects here
        // Examples:
        // - Reduce political power by 5%
        // - Notify nation that corruption is critical
        // - Apply debuffs to nation members

        // For now, just reduce political power
        PoliticalPowerManager ppManager = plugin.getPPManager();
        double currentPP = ppManager.getPoliticalPower(nation);
        double reduction = currentPP * 0.05; // 5% reduction

        if (reduction > 0) {
            ppManager.removePoliticalPower(nation, reduction);
            plugin.getLogger().info("Nation " + nation.getName() +
                    " lost " + String.format("%.2f", reduction) +
                    " political power due to critical corruption levels");
        }
    }

    /**
     * Calculate the political power cost to reduce corruption
     *
     * @param amount The amount of corruption to reduce
     * @return The political power cost
     */
    public double calculatePPCostForCorruptionReduction(double amount) {
        // Base cost from config
        double baseRate = plugin.getConfig().getDouble("corruption.pp_cost_rate", 2.0);

        // Each point of corruption reduction costs baseRate PP
        return amount * baseRate;
    }
}