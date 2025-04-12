package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.ITownCorruptionStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TownCorruptionManager implements Manager {

    private final TownyPolitics plugin;
    private final ITownCorruptionStorage storage;
    private final DelegateLogger logger;

    // Cache of town UUIDs to their corruption levels
    private final Map<UUID, Double> townCorruption;

    // Maximum corruption level (100% is completely corrupt)
    private final double MAX_CORRUPTION = 100.0;

    // Corruption thresholds
    private final double LOW_THRESHOLD;
    private final double MEDIUM_THRESHOLD;
    private final double HIGH_THRESHOLD;
    private final double CRITICAL_THRESHOLD;

    public TownCorruptionManager(TownyPolitics plugin, ITownCorruptionStorage storage, TownGovernmentManager townGovManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.townCorruption = new HashMap<>();
        this.logger = new DelegateLogger(plugin, "TownCorruption");

        // Load configuration - threshold values
        this.LOW_THRESHOLD = plugin.getConfig().getDouble("town_corruption.thresholds.low", 25.0);
        this.MEDIUM_THRESHOLD = plugin.getConfig().getDouble("town_corruption.thresholds.medium", 50.0);
        this.HIGH_THRESHOLD = plugin.getConfig().getDouble("town_corruption.thresholds.high", 75.0);
        this.CRITICAL_THRESHOLD = plugin.getConfig().getDouble("town_corruption.thresholds.critical", 90.0);

        // Load data from storage
        loadData();
    }

    @Override
    public void loadData() {
        townCorruption.clear();
        townCorruption.putAll(storage.loadAllCorruption());
        logger.info("Loaded corruption data for " + townCorruption.size() + " towns");
    }

    @Override
    public void saveAllData() {
        storage.saveAll();
        logger.info("Saved town corruption data to storage");
    }

    public double getCorruption(Town town) {
        return townCorruption.getOrDefault(town.getUUID(), 0.0);
    }

    public void setCorruption(Town town, double amount) {
        // Ensure corruption can't go below 0 or above MAX_CORRUPTION
        double newAmount = Math.min(MAX_CORRUPTION, Math.max(0, amount));
        townCorruption.put(town.getUUID(), newAmount);
        storage.saveCorruption(town.getUUID(), newAmount);
        logger.info("Set corruption for town " + town.getName() + " to " + newAmount);
    }

    public void addCorruption(Town town, double amount) {
        double current = getCorruption(town);
        double newAmount = current + amount;
        setCorruption(town, newAmount);
        logger.info("Added " + amount + " corruption to town " + town.getName() +
                " (now " + newAmount + "%)");
    }

    public void reduceCorruption(Town town, double amount) {
        double current = getCorruption(town);
        double newAmount = current - amount;
        setCorruption(town, newAmount);
        logger.info("Reduced corruption for town " + town.getName() +
                " by " + amount + " (now " + newAmount + "%)");
    }

    /**
     * Get the corruption threshold level for a town
     *
     * @param town The town
     * @return The threshold level (0-4, where 4 is critical)
     */
    public int getCorruptionThresholdLevel(Town town) {
        double corruption = getCorruption(town);

        if (corruption >= CRITICAL_THRESHOLD) return 4; // Critical
        if (corruption >= HIGH_THRESHOLD) return 3;     // High
        if (corruption >= MEDIUM_THRESHOLD) return 2;   // Medium
        if (corruption >= LOW_THRESHOLD) return 1;      // Low
        return 0;                                       // Minimal
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
     * Calculate daily corruption gain for a town based on its government type
     *
     * @param town The town
     * @return The daily corruption gain
     */
    public double calculateDailyCorruptionGain(Town town) {
        // Base daily corruption gain from config
        double baseGain = plugin.getConfig().getDouble("town_corruption.base_daily_gain", 0.4);

        // Government type modifier
        TownGovernmentManager townGovManager = plugin.getTownGovManager();
        GovernmentType govType = townGovManager.getGovernmentType(town);
        double govModifier = getGovernmentTypeCorruptionModifier(govType);

        // Apply policy modifiers
        double policyModifier = 1.0;
        if (plugin.getPolicyEffectsHandler() != null) {
            policyModifier = plugin.getPolicyEffectsHandler().getCorruptionGainModifier(town);
        }

        // Calculate final gain (minimum 0)
        double finalGain = Math.max(0, baseGain * govModifier * policyModifier);

        logger.fine("Daily corruption gain for " + town.getName() +
                ": base=" + baseGain +
                ", govMod=" + govModifier +
                ", policyMod=" + policyModifier +
                ", final=" + finalGain);

        return finalGain;
    }

    /**
     * Get corruption modifier based on town government type
     *
     * @param govType The government type
     * @return The corruption gain multiplier
     */
    private double getGovernmentTypeCorruptionModifier(GovernmentType govType) {
        return switch (govType) {
            case AUTOCRACY -> 0.97;    // -3% corruption gain
            case OLIGARCHY -> 1.05;    // +5% corruption gain
            case REPUBLIC -> 1.01;     // +1% corruption gain
            case DIRECT_DEMOCRACY -> 0.90;    // -10% corruption gain
            default -> 1.0;
        };
    }

    /**
     * Process daily corruption changes for all towns
     */
    public void processNewDay() {
        logger.info("Processing daily corruption changes for all towns");

        plugin.getTownyAPI().getTowns().forEach(town -> {
            double gain = calculateDailyCorruptionGain(town);

            // Store old threshold level
            int oldThresholdLevel = getCorruptionThresholdLevel(town);

            // Add corruption
            addCorruption(town, gain);

            // Get new threshold level
            int newThresholdLevel = getCorruptionThresholdLevel(town);
            double currentCorruption = getCorruption(town);

            logger.info("Town " + town.getName() + " gained " +
                    String.format("%.2f", gain) + " corruption, now at " +
                    String.format("%.2f", currentCorruption) + "% (" +
                    getCorruptionThresholdName(newThresholdLevel) + ")");

            // Apply threshold-based effects
            applyThresholdEffects(town, oldThresholdLevel, newThresholdLevel);
        });
    }

    /**
     * Apply effects based on corruption threshold level
     *
     * @param town The town
     * @param oldLevel Previous corruption threshold level
     * @param newLevel Current corruption threshold level
     */
    private void applyThresholdEffects(Town town, int oldLevel, int newLevel) {
        // If threshold level increased, notify and apply immediate effects
        if (newLevel > oldLevel) {
            String newLevelName = getCorruptionThresholdName(newLevel);

            // Log the threshold change
            logger.info("Town " + town.getName() +
                    " corruption increased to " + newLevelName + " level!");

            // Apply effects based on new threshold level
            applyCorruptionEffects(town, newLevel);
        }
        // Always apply daily effects for high/critical corruption
        else if (newLevel >= 3) {
            applyCorruptionEffects(town, newLevel);
        }
    }

    /**
     * Apply negative effects based on corruption threshold level
     *
     * @param town The town
     * @param thresholdLevel Current corruption threshold level (0-4)
     */
    private void applyCorruptionEffects(Town town, int thresholdLevel) {
        // Town corruption doesn't affect political power or spending as specified
        // But we can add future effects here if needed

        switch (thresholdLevel) {
            case 3: // High corruption
                // Future high corruption effects could go here
                break;

            case 4: // Critical corruption
                // Future critical corruption effects could go here
                break;
        }
    }

    /**
     * Get the taxation modifier for towns based on corruption
     *
     * @param town The town
     * @return The taxation modifier (lower with higher corruption)
     */
    public double getTaxationModifier(Town town) {
        int thresholdLevel = getCorruptionThresholdLevel(town);

        // Get values from config or use defaults
        double lowMod = plugin.getConfig().getDouble("town_corruption.effects.taxation.low", 0.95);
        double mediumMod = plugin.getConfig().getDouble("town_corruption.effects.taxation.medium", 0.90);
        double highMod = plugin.getConfig().getDouble("town_corruption.effects.taxation.high", 0.80);
        double criticalMod = plugin.getConfig().getDouble("town_corruption.effects.taxation.critical", 0.70);

        // Return appropriate modifier based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> lowMod;          // -5% tax income by default
            case 2 -> mediumMod;       // -10% tax income by default
            case 3 -> highMod;         // -20% tax income by default
            case 4 -> criticalMod;     // -30% tax income by default
            default -> 1.0;
        };
    }

    /**
     * Get the trade modifier for towns based on corruption
     *
     * @param town The town
     * @return The trade modifier (lower with higher corruption)
     */
    public double getTradeModifier(Town town) {
        int thresholdLevel = getCorruptionThresholdLevel(town);

        // Get values from config or use defaults
        double lowMod = plugin.getConfig().getDouble("town_corruption.effects.trade.low", 0.95);
        double mediumMod = plugin.getConfig().getDouble("town_corruption.effects.trade.medium", 0.90);
        double highMod = plugin.getConfig().getDouble("town_corruption.effects.trade.high", 0.80);
        double criticalMod = plugin.getConfig().getDouble("town_corruption.effects.trade.critical", 0.70);

        // Return appropriate modifier based on threshold
        return switch (thresholdLevel) {
            case 0 -> 1.0;             // No effect
            case 1 -> lowMod;          // -5% trade income by default
            case 2 -> mediumMod;       // -10% trade income by default
            case 3 -> highMod;         // -20% trade income by default
            case 4 -> criticalMod;     // -30% trade income by default
            default -> 1.0;
        };
    }
}