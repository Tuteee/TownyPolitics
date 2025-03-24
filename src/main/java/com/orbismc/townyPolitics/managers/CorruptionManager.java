package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.ICorruptionStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CorruptionManager {

    private final TownyPolitics plugin;
    private final ICorruptionStorage storage;
    private final GovernmentManager govManager;

    // Cache of nation UUIDs to their corruption levels
    private final Map<UUID, Double> nationCorruption;

    // Maximum corruption level (100% is completely corrupt)
    private final double MAX_CORRUPTION = 100.0;

    // Corruption thresholds
    private final double LOW_THRESHOLD;
    private final double MEDIUM_THRESHOLD;
    private final double HIGH_THRESHOLD;
    private final double CRITICAL_THRESHOLD;

    public CorruptionManager(TownyPolitics plugin, ICorruptionStorage storage, GovernmentManager govManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.govManager = govManager;
        this.nationCorruption = new HashMap<>();

        // Load configuration - threshold values
        this.LOW_THRESHOLD = plugin.getConfig().getDouble("corruption.thresholds.low", 25.0);
        this.MEDIUM_THRESHOLD = plugin.getConfig().getDouble("corruption.thresholds.medium", 50.0);
        this.HIGH_THRESHOLD = plugin.getConfig().getDouble("corruption.thresholds.high", 75.0);
        this.CRITICAL_THRESHOLD = plugin.getConfig().getDouble("corruption.thresholds.critical", 90.0);

        // Load data from storage
        loadData();
    }

    public void loadData() {
        nationCorruption.clear();
        nationCorruption.putAll(storage.loadAllCorruption(true));
    }

    public double getCorruption(Nation nation) {
        return nationCorruption.getOrDefault(nation.getUUID(), 0.0);
    }

    public void setCorruption(Nation nation, double amount) {
        // Ensure corruption can't go below 0 or above MAX_CORRUPTION
        double newAmount = Math.min(MAX_CORRUPTION, Math.max(0, amount));
        nationCorruption.put(nation.getUUID(), newAmount);
        storage.saveCorruption(nation.getUUID(), newAmount, true);
    }

    public void addCorruption(Nation nation, double amount) {
        double current = getCorruption(nation);
        setCorruption(nation, current + amount);
    }

    public void reduceCorruption(Nation nation, double amount) {
        double current = getCorruption(nation);
        setCorruption(nation, current - amount);
    }

    /**
     * Get the corruption threshold level for a nation
     *
     * @param nation The nation
     * @return The threshold level (0-4, where 4 is critical)
     */
    public int getCorruptionThresholdLevel(Nation nation) {
        double corruption = getCorruption(nation);

        if (corruption >= CRITICAL_THRESHOLD) return 4; // Critical
        if (corruption >= HIGH_THRESHOLD) return 3;     // High
        if (corruption >= MEDIUM_THRESHOLD) return 2;   // Medium
        if (corruption >= LOW_THRESHOLD) return 1;      // Low
        return 0;                                       // Minimal
    }

    /**
     * Check if nation's corruption is at or above a specific threshold
     *
     * @param nation The nation
     * @param thresholdLevel The threshold level to check (0-4)
     * @return True if corruption is at or above the threshold
     */
    public boolean isCorruptionAtOrAboveThreshold(Nation nation, int thresholdLevel) {
        return getCorruptionThresholdLevel(nation) >= thresholdLevel;
    }

    /**
     * Check if a nation's corruption is at critical level
     *
     * @param nation The nation
     * @return True if corruption is critical
     */
    public boolean isCorruptionCritical(Nation nation) {
        return getCorruptionThresholdLevel(nation) == 4;
    }

    /**
     * Get the name of the corruption threshold for display
     *
     * @param thresholdLevel The threshold level (0-4)
     * @return The name of the threshold
     */
    public String getCorruptionThresholdName(int thresholdLevel) {
        return switch (thresholdLevel) {
            case 0 -> "Minimal";
            case 1 -> "Low";
            case 2 -> "Medium";
            case 3 -> "High";
            case 4 -> "Critical";
            default -> "Unknown";
        };
    }

    /**
     * Get the modifier for taxation based on corruption level
     * Higher corruption = higher potential maximum taxation
     *
     * @param nation The nation
     * @return The taxation modifier (0.0 to 1.0 representing 0-100%)
     */
    public double getTaxationModifier(Nation nation) {
        int thresholdLevel = getCorruptionThresholdLevel(nation);

        // Increasing effects based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 1.05;            // +5% max taxation
            case 2 -> 1.10;            // +10% max taxation
            case 3 -> 1.15;            // +15% max taxation
            case 4 -> 1.20;            // +20% max taxation
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for political power gain based on corruption level
     * Higher corruption = lower PP gain
     *
     * @param nation The nation
     * @return The PP gain modifier (0.0 to 1.0)
     */
    public double getPoliticalPowerModifier(Nation nation) {
        int thresholdLevel = getCorruptionThresholdLevel(nation);

        // Only medium+ corruption affects PP gain
        return switch (thresholdLevel) {
            case 0, 1 -> 1.0;          // No effect for minimal/low
            case 2 -> 0.90;            // -10% PP gain
            case 3 -> 0.75;            // -25% PP gain
            case 4 -> 0.50;            // -50% PP gain
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for resource output based on corruption level
     * Higher corruption = lower resource output
     *
     * @param nation The nation
     * @return The resource modifier (0.0 to 1.0)
     */
    public double getResourceModifier(Nation nation) {
        int thresholdLevel = getCorruptionThresholdLevel(nation);

        // Decreasing output at each threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 0.95;            // -5% resource output
            case 2 -> 0.85;            // -15% resource output
            case 3 -> 0.75;            // -25% resource output
            case 4 -> 0.60;            // -40% resource output
            default -> 1.0;
        };
    }

    /**
     * Get the modifier for spending based on corruption level
     * Higher corruption = higher spending
     *
     * @param nation The nation
     * @return The spending modifier (1.0 or higher)
     */
    public double getSpendingModifier(Nation nation) {
        int thresholdLevel = getCorruptionThresholdLevel(nation);

        // Increasing costs at each threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> 1.10;            // +10% spending
            case 2 -> 1.20;            // +20% spending
            case 3 -> 1.30;            // +30% spending
            case 4 -> 1.50;            // +50% spending
            default -> 1.0;
        };
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

            // Store old threshold level
            int oldThresholdLevel = getCorruptionThresholdLevel(nation);

            // Add corruption
            addCorruption(nation, gain);

            // Get new threshold level
            int newThresholdLevel = getCorruptionThresholdLevel(nation);
            double currentCorruption = getCorruption(nation);

            plugin.getLogger().info("Nation " + nation.getName() + " gained " +
                    String.format("%.2f", gain) + " corruption, now at " +
                    String.format("%.2f", currentCorruption) + "% (" +
                    getCorruptionThresholdName(newThresholdLevel) + ")");

            // Apply threshold-based effects
            applyThresholdEffects(nation, oldThresholdLevel, newThresholdLevel);
        });
    }

    /**
     * Apply effects based on corruption threshold level
     *
     * @param nation The nation
     * @param oldLevel Previous corruption threshold level
     * @param newLevel Current corruption threshold level
     */
    private void applyThresholdEffects(Nation nation, int oldLevel, int newLevel) {
        // If threshold level increased, notify and apply immediate effects
        if (newLevel > oldLevel) {
            String newLevelName = getCorruptionThresholdName(newLevel);

            // Log the threshold change
            plugin.getLogger().info("Nation " + nation.getName() +
                    " corruption increased to " + newLevelName + " level!");

            // Apply effects based on new threshold level
            applyCorruptionEffects(nation, newLevel);
        }
        // Always apply daily effects for high/critical corruption
        else if (newLevel >= 3) {
            applyCorruptionEffects(nation, newLevel);
        }
    }

    /**
     * Apply negative effects based on corruption threshold level
     *
     * @param nation The nation
     * @param thresholdLevel Current corruption threshold level (0-4)
     */
    private void applyCorruptionEffects(Nation nation, int thresholdLevel) {
        PoliticalPowerManager ppManager = plugin.getPPManager();
        double currentPP = ppManager.getPoliticalPower(nation);
        double reduction = 0;

        // Apply effects based on threshold level
        switch (thresholdLevel) {
            case 3: // High corruption
                // 2.5% political power reduction
                reduction = currentPP * 0.025;
                break;

            case 4: // Critical corruption
                // 5% political power reduction
                reduction = currentPP * 0.05;

                // Additional critical effects could go here
                // - Random event chance
                // - Possible revolts
                // - Blocked certain actions
                break;
        }

        // Apply political power reduction if any
        if (reduction > 0) {
            ppManager.removePoliticalPower(nation, reduction);
            plugin.getLogger().info("Nation " + nation.getName() +
                    " lost " + String.format("%.2f", reduction) +
                    " political power due to " + getCorruptionThresholdName(thresholdLevel) +
                    " corruption levels");
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