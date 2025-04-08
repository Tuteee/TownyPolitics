package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.BudgetCategory;
import com.orbismc.townyPolitics.budget.BudgetAllocation;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.*;

public class BudgetManager implements Manager {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Maps to store budget allocations
    private final Map<UUID, Map<BudgetCategory, BudgetAllocation>> townBudgets;
    private final Map<UUID, Map<BudgetCategory, BudgetAllocation>> nationBudgets;

    // Maps to store last budget cycle times
    private final Map<UUID, Long> townLastBudgetCycles;
    private final Map<UUID, Long> nationLastBudgetCycles;

    // Budget cycle duration in milliseconds
    private final long budgetCycleDuration;

    public BudgetManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "BudgetManager");

        this.townBudgets = new HashMap<>();
        this.nationBudgets = new HashMap<>();
        this.townLastBudgetCycles = new HashMap<>();
        this.nationLastBudgetCycles = new HashMap<>();

        // Get budget cycle duration from config
        int cycleDays = plugin.getConfig().getInt("budget.cycle_days", 7);
        this.budgetCycleDuration = cycleDays * 24L * 60L * 60L * 1000L;

        logger.info("Budget Manager initialized with cycle duration: " + cycleDays + " days");
    }

    @Override
    public void loadData() {
        // Load budget data from storage
        // This would be implemented with a new storage interface
    }

    @Override
    public void saveAllData() {
        // Save budget data to storage
    }

    /**
     * Process the budget cycle for all towns and nations
     * Called during Towny's new day event
     */
    public void processBudgetCycle() {
        logger.info("Processing budget cycle");

        // Process town budgets
        plugin.getTownyAPI().getTowns().forEach(this::processTownBudget);

        // Process nation budgets
        plugin.getTownyAPI().getNations().forEach(this::processNationBudget);
    }

    /**
     * Process a town's budget
     */
    private void processTownBudget(Town town) {
        UUID townId = town.getUUID();

        // Check if it's time for this town's budget cycle
        long lastCycle = townLastBudgetCycles.getOrDefault(townId, 0L);
        long now = System.currentTimeMillis();

        if (now - lastCycle < budgetCycleDuration) {
            // Not time yet
            return;
        }

        // Get this town's budget or create a default one
        Map<BudgetCategory, BudgetAllocation> budget = townBudgets.computeIfAbsent(
                townId, k -> createDefaultBudget(false, town.getResidents().size(), town.getTownBlocks().size())
        );

        // Calculate recommended funding levels
        Map<BudgetCategory, Double> recommendedFunding = calculateRecommendedFunding(false, town);

        // Calculate actual costs and apply effects
        double totalCost = 0;
        for (BudgetCategory category : BudgetCategory.values()) {
            BudgetAllocation allocation = budget.get(category);
            double recommended = recommendedFunding.get(category);
            double actualCost = calculateActualCost(allocation, recommended);

            totalCost += actualCost;

            // Apply effects based on funding level
            applyTownBudgetEffects(town, category, allocation, recommended);
        }

        // Withdraw funds from town account
        if (totalCost > 0) {
            boolean success = town.getAccount().withdraw(totalCost, "Budget cycle expenses");

            if (success) {
                logger.info("Town " + town.getName() + " paid " + totalCost + " for budget cycle");
            } else {
                logger.warning("Town " + town.getName() + " couldn't afford budget costs: " + totalCost);
                // Apply penalties for not being able to afford budget
                applyTownBudgetFailurePenalties(town);
            }
        }

        // Update last cycle time
        townLastBudgetCycles.put(townId, now);
    }

    /**
     * Process a nation's budget
     */
    private void processNationBudget(Nation nation) {
        UUID nationId = nation.getUUID();

        // Check if it's time for this nation's budget cycle
        long lastCycle = nationLastBudgetCycles.getOrDefault(nationId, 0L);
        long now = System.currentTimeMillis();

        if (now - lastCycle < budgetCycleDuration) {
            // Not time yet
            return;
        }

        // Get this nation's budget or create a default one
        Map<BudgetCategory, BudgetAllocation> budget = nationBudgets.computeIfAbsent(
                nationId, k -> createDefaultBudget(true, nation.getNumResidents(), getTotalNationClaims(nation))
        );

        // Calculate recommended funding levels
        Map<BudgetCategory, Double> recommendedFunding = calculateRecommendedFunding(true, nation);

        // Calculate actual costs and apply effects
        double totalCost = 0;
        for (BudgetCategory category : BudgetCategory.values()) {
            BudgetAllocation allocation = budget.get(category);
            double recommended = recommendedFunding.get(category);
            double actualCost = calculateActualCost(allocation, recommended);

            totalCost += actualCost;

            // Apply effects based on funding level
            applyNationBudgetEffects(nation, category, allocation, recommended);
        }

        // Withdraw funds from nation account
        if (totalCost > 0) {
            boolean success = nation.getAccount().withdraw(totalCost, "Budget cycle expenses");

            if (success) {
                logger.info("Nation " + nation.getName() + " paid " + totalCost + " for budget cycle");
            } else {
                logger.warning("Nation " + nation.getName() + " couldn't afford budget costs: " + totalCost);
                // Apply penalties for not being able to afford budget
                applyNationBudgetFailurePenalties(nation);
            }
        }

        // Update last cycle time
        nationLastBudgetCycles.put(nationId, now);
    }

    /**
     * Create a default budget allocation for a new town or nation
     */
    private Map<BudgetCategory, BudgetAllocation> createDefaultBudget(boolean isNation, int residents, int claims) {
        Map<BudgetCategory, BudgetAllocation> budget = new HashMap<>();

        // Create default allocations based on recommended amounts
        for (BudgetCategory category : BudgetCategory.values()) {
            double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
            double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

            // Default allocation is midpoint of min and max
            double defaultPercent = (minPercent + maxPercent) / 2;
            budget.put(category, new BudgetAllocation(defaultPercent, 100));
        }

        return budget;
    }

    /**
     * Calculate recommended funding for each category
     */
    private Map<BudgetCategory, Double> calculateRecommendedFunding(boolean isNation, Object entity) {
        Map<BudgetCategory, Double> recommended = new HashMap<>();

        int residents;
        int claims;

        if (isNation) {
            Nation nation = (Nation) entity;
            residents = nation.getNumResidents();
            claims = getTotalNationClaims(nation);
        } else {
            Town town = (Town) entity;
            residents = town.getResidents().size();
            claims = town.getTownBlocks().size();
        }

        // Calculate recommended funding based on config formulas
        for (BudgetCategory category : BudgetCategory.values()) {
            String configKey = "budget.categories." + category.getConfigKey();

            double baseCost;
            switch (category) {
                case MILITARY, ADMINISTRATION, EDUCATION ->
                        baseCost = plugin.getConfig().getDouble(configKey + ".base_cost_per_resident", 1.0) * residents;
                case INFRASTRUCTURE ->
                        baseCost = plugin.getConfig().getDouble(configKey + ".base_cost_per_claim", 1.0) * claims;
                default -> baseCost = 0;
            }

            recommended.put(category, baseCost);
        }

        return recommended;
    }

    /**
     * Calculate actual cost based on allocation and recommended amount
     */
    private double calculateActualCost(BudgetAllocation allocation, double recommended) {
        // Formula: (allocation percentage / 100) * recommended amount
        return (allocation.getPercentage() / 100.0) * recommended;
    }

    /**
     * Apply budget effects for a town
     */
    private void applyTownBudgetEffects(Town town, BudgetCategory category, BudgetAllocation allocation, double recommended) {
        String configKey = "budget.categories." + category.getConfigKey() + ".effects";
        double allocationRatio = (allocation.getPercentage() / 100.0) * recommended / recommended;
        double underfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.underfunded", 70) / 100.0;
        double overfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.overfunded", 130) / 100.0;

        String effectsKey;
        if (allocationRatio < underfundedThreshold) {
            effectsKey = configKey + ".underfunded";
            logger.fine("Town " + town.getName() + " is underfunding " + category.name());
        } else if (allocationRatio > overfundedThreshold) {
            effectsKey = configKey + ".overfunded";
            logger.fine("Town " + town.getName() + " is overfunding " + category.name());
        } else {
            effectsKey = configKey + ".standard";
            logger.fine("Town " + town.getName() + " has standard funding for " + category.name());
        }

        // Apply category-specific effects
        switch (category) {
            case INFRASTRUCTURE:
                // Apply infrastructure effects
                applyInfrastructureEffects(town, false, effectsKey);
                break;

            case ADMINISTRATION:
                // Apply administration effects
                applyAdministrationEffects(town, false, effectsKey);
                break;

            case EDUCATION:
                // Apply education effects
                applyEducationEffects(town, false, effectsKey);
                break;

            case MILITARY:
                // Apply military effects
                applyMilitaryEffects(town, false, effectsKey);
                break;
        }
    }

    /**
     * Apply budget effects for a nation
     */
    private void applyNationBudgetEffects(Nation nation, BudgetCategory category, BudgetAllocation allocation, double recommended) {
        String configKey = "budget.categories." + category.getConfigKey() + ".effects";
        double allocationRatio = (allocation.getPercentage() / 100.0) * recommended / recommended;
        double underfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.underfunded", 70) / 100.0;
        double overfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.overfunded", 130) / 100.0;

        String effectsKey;
        if (allocationRatio < underfundedThreshold) {
            effectsKey = configKey + ".underfunded";
            logger.fine("Nation " + nation.getName() + " is underfunding " + category.name());
        } else if (allocationRatio > overfundedThreshold) {
            effectsKey = configKey + ".overfunded";
            logger.fine("Nation " + nation.getName() + " is overfunding " + category.name());
        } else {
            effectsKey = configKey + ".standard";
            logger.fine("Nation " + nation.getName() + " has standard funding for " + category.name());
        }

        // Apply category-specific effects
        switch (category) {
            case INFRASTRUCTURE:
                // Apply infrastructure effects
                applyNationInfrastructureEffects(nation, effectsKey);
                break;

            case ADMINISTRATION:
                // Apply administration effects
                applyNationAdministrationEffects(nation, effectsKey);
                break;

            case EDUCATION:
                // Apply education effects
                applyNationEducationEffects(nation, effectsKey);
                break;

            case MILITARY:
                // Apply military effects
                applyNationMilitaryEffects(nation, effectsKey);
                break;
        }
    }

    // Implement the various effect application methods here...

    /**
     * Apply infrastructure effects for a town
     */
    private void applyInfrastructureEffects(Town town, boolean isNation, String effectsKey) {
        double claimCostMod = plugin.getConfig().getDouble(effectsKey + ".claim_cost_modifier", 1.0);
        double townBlockBonus = plugin.getConfig().getDouble(effectsKey + ".town_block_bonus", 1.0);

        // Store these values in some cached map to be used by other systems
        // Example: townInfrastructureEffects.put(town.getUUID(), new InfrastructureEffects(claimCostMod, townBlockBonus));
    }

    /**
     * Apply administration effects for a town
     */
    private void applyAdministrationEffects(Town town, boolean isNation, String effectsKey) {
        double taxCollectionMod = plugin.getConfig().getDouble(effectsKey + ".tax_collection_modifier", 1.0);
        double corruptionGainMod = plugin.getConfig().getDouble(effectsKey + ".corruption_gain_modifier", 1.0);

        // Store these values to be used by other systems
        // Example: townAdministrationEffects.put(town.getUUID(), new AdministrationEffects(taxCollectionMod, corruptionGainMod));
    }

    /**
     * Apply education effects for a town
     */
    private void applyEducationEffects(Town town, boolean isNation, String effectsKey) {
        double ppGainMod = plugin.getConfig().getDouble(effectsKey + ".pp_gain_modifier", 1.0);
        double policyCostMod = plugin.getConfig().getDouble(effectsKey + ".policy_cost_modifier", 1.0);

        // Store these values to be used by other systems
    }

    /**
     * Apply military effects for a town
     */
    private void applyMilitaryEffects(Town town, boolean isNation, String effectsKey) {
        double strengthMod = plugin.getConfig().getDouble(effectsKey + ".strength_modifier", 1.0);
        double buildingDamageMod = plugin.getConfig().getDouble(effectsKey + ".building_damage_modifier", 1.0);

        // Store these values to be used by other systems
    }

    // Nation-specific effect application methods

    private void applyNationInfrastructureEffects(Nation nation, String effectsKey) {
        // Similar to town effects but nation-specific
    }

    private void applyNationAdministrationEffects(Nation nation, String effectsKey) {
        // Similar to town effects but nation-specific
    }

    private void applyNationEducationEffects(Nation nation, String effectsKey) {
        // Similar to town effects but nation-specific
    }

    private void applyNationMilitaryEffects(Nation nation, String effectsKey) {
        // Similar to town effects but nation-specific
    }

    /**
     * Apply penalties when a town can't afford its budget
     */
    private void applyTownBudgetFailurePenalties(Town town) {
        // Apply penalties - maybe increase corruption, reduce PP gain, etc.
    }

    /**
     * Apply penalties when a nation can't afford its budget
     */
    private void applyNationBudgetFailurePenalties(Nation nation) {
        // Apply penalties - maybe increase corruption, reduce PP gain, etc.
    }

    /**
     * Get the total claims of all towns in a nation
     */
    private int getTotalNationClaims(Nation nation) {
        int totalClaims = 0;
        for (Town town : nation.getTowns()) {
            totalClaims += town.getTownBlocks().size();
        }
        return totalClaims;
    }

    // Command methods for setting budget allocations

    /**
     * Set a budget allocation for a town
     */
    public boolean setTownBudgetAllocation(Town town, BudgetCategory category, double percentage) {
        // Validate percentage is within min/max range
        double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
        double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

        if (percentage < minPercent || percentage > maxPercent) {
            return false;
        }

        // Get the town's budget or create a default one
        Map<BudgetCategory, BudgetAllocation> budget = townBudgets.computeIfAbsent(
                town.getUUID(),
                k -> createDefaultBudget(false, town.getResidents().size(), town.getTownBlocks().size())
        );

        // Update the allocation
        BudgetAllocation current = budget.get(category);
        BudgetAllocation updated = new BudgetAllocation(percentage, current.getPriority());
        budget.put(category, updated);

        return true;
    }

    /**
     * Set a budget allocation for a nation
     */
    public boolean setNationBudgetAllocation(Nation nation, BudgetCategory category, double percentage) {
        // Similar to town method but for nations
        return true;
    }

    // Add getters for current allocation and effect values

    /**
     * Get all budget allocations for a town
     */
    public Map<BudgetCategory, BudgetAllocation> getTownBudgetAllocations(Town town) {
        return townBudgets.getOrDefault(town.getUUID(),
                createDefaultBudget(false, town.getResidents().size(), town.getTownBlocks().size()));
    }

    /**
     * Get all budget allocations for a nation
     */
    public Map<BudgetCategory, BudgetAllocation> getNationBudgetAllocations(Nation nation) {
        return nationBudgets.getOrDefault(nation.getUUID(),
                createDefaultBudget(true, nation.getNumResidents(), getTotalNationClaims(nation)));
    }
}