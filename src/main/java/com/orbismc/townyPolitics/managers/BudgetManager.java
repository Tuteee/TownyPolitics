package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.BudgetCategory;
import com.orbismc.townyPolitics.budget.BudgetAllocation;
import com.orbismc.townyPolitics.budget.MilitaryEffects;
import com.orbismc.townyPolitics.budget.InfrastructureEffects;
import com.orbismc.townyPolitics.budget.AdministrationEffects;
import com.orbismc.townyPolitics.budget.EducationEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

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

    // Maps to track budget failure penalties
    private final Map<UUID, Integer> townFailureStreak;
    private final Map<UUID, Integer> nationFailureStreak;

    public BudgetManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "BudgetManager");

        this.townBudgets = new HashMap<>();
        this.nationBudgets = new HashMap<>();
        this.townLastBudgetCycles = new HashMap<>();
        this.nationLastBudgetCycles = new HashMap<>();
        this.townFailureStreak = new HashMap<>();
        this.nationFailureStreak = new HashMap<>();

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
        Map<BudgetCategory, Double> actualCosts = new HashMap<>();

        for (BudgetCategory category : BudgetCategory.values()) {
            BudgetAllocation allocation = budget.get(category);
            double recommended = recommendedFunding.get(category);
            double actualCost = calculateActualCost(allocation, recommended);

            actualCosts.put(category, actualCost);
            totalCost += actualCost;
        }

        // Withdraw funds from town account
        if (totalCost > 0) {
            boolean success = town.getAccount().withdraw(totalCost, "Budget cycle expenses");

            if (success) {
                logger.info("Town " + town.getName() + " paid " + totalCost + " for budget cycle");

                // Reset failure streak on success
                townFailureStreak.put(townId, 0);

                // Apply effects after successful payment
                for (BudgetCategory category : BudgetCategory.values()) {
                    BudgetAllocation allocation = budget.get(category);
                    double recommended = recommendedFunding.get(category);

                    // Apply effects based on funding level
                    applyTownBudgetEffects(town, category, allocation, recommended);
                }

                // Notify town mayor if online
                notifyMayor(town, true, totalCost, actualCosts);

            } else {
                logger.warning("Town " + town.getName() + " couldn't afford budget costs: " + totalCost);

                // Increment failure streak
                int failures = townFailureStreak.getOrDefault(townId, 0) + 1;
                townFailureStreak.put(townId, failures);

                // Apply penalties for not being able to afford budget
                applyTownBudgetFailurePenalties(town, failures);

                // Notify town mayor if online
                notifyMayor(town, false, totalCost, actualCosts);
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
        Map<BudgetCategory, Double> actualCosts = new HashMap<>();

        for (BudgetCategory category : BudgetCategory.values()) {
            BudgetAllocation allocation = budget.get(category);
            double recommended = recommendedFunding.get(category);
            double actualCost = calculateActualCost(allocation, recommended);

            actualCosts.put(category, actualCost);
            totalCost += actualCost;
        }

        // Withdraw funds from nation account
        if (totalCost > 0) {
            boolean success = nation.getAccount().withdraw(totalCost, "Budget cycle expenses");

            if (success) {
                logger.info("Nation " + nation.getName() + " paid " + totalCost + " for budget cycle");

                // Reset failure streak on success
                nationFailureStreak.put(nationId, 0);

                // Apply effects after successful payment
                for (BudgetCategory category : BudgetCategory.values()) {
                    BudgetAllocation allocation = budget.get(category);
                    double recommended = recommendedFunding.get(category);

                    // Apply effects based on funding level
                    applyNationBudgetEffects(nation, category, allocation, recommended);
                }

                // Notify nation leader if online
                notifyNationLeader(nation, true, totalCost, actualCosts);

            } else {
                logger.warning("Nation " + nation.getName() + " couldn't afford budget costs: " + totalCost);

                // Increment failure streak
                int failures = nationFailureStreak.getOrDefault(nationId, 0) + 1;
                nationFailureStreak.put(nationId, failures);

                // Apply penalties for not being able to afford budget
                applyNationBudgetFailurePenalties(nation, failures);

                // Notify nation leader if online
                notifyNationLeader(nation, false, totalCost, actualCosts);
            }
        }

        // Update last cycle time
        nationLastBudgetCycles.put(nationId, now);
    }

    /**
     * Notify town mayor of budget results
     */
    private void notifyMayor(Town town, boolean success, double totalCost, Map<BudgetCategory, Double> costs) {
        if (town.getMayor() == null) return;

        Player mayor = town.getMayor().getPlayer();
        if (mayor == null || !mayor.isOnline()) return;

        if (success) {
            mayor.sendMessage(ChatColor.GREEN + "Town Budget: " + ChatColor.WHITE +
                    String.format("%.2f", totalCost) + " has been withdrawn for the town budget cycle.");

            // Send breakdown of costs
            mayor.sendMessage(ChatColor.YELLOW + "Budget breakdown:");
            for (Map.Entry<BudgetCategory, Double> entry : costs.entrySet()) {
                mayor.sendMessage(ChatColor.GRAY + "• " + entry.getKey().name() + ": " +
                        ChatColor.WHITE + String.format("%.2f", entry.getValue()));
            }
        } else {
            mayor.sendMessage(ChatColor.RED + "Town Budget: Your town could not afford the budget cycle cost of " +
                    String.format("%.2f", totalCost) + ". Some penalties have been applied.");

            mayor.sendMessage(ChatColor.YELLOW + "Consider adjusting your budget allocations or ensuring sufficient funds.");
        }
    }

    /**
     * Notify nation leader of budget results
     */
    private void notifyNationLeader(Nation nation, boolean success, double totalCost, Map<BudgetCategory, Double> costs) {
        if (nation.getKing() == null) return;

        Player leader = nation.getKing().getPlayer();
        if (leader == null || !leader.isOnline()) return;

        if (success) {
            leader.sendMessage(ChatColor.GREEN + "Nation Budget: " + ChatColor.WHITE +
                    String.format("%.2f", totalCost) + " has been withdrawn for the nation budget cycle.");

            // Send breakdown of costs
            leader.sendMessage(ChatColor.YELLOW + "Budget breakdown:");
            for (Map.Entry<BudgetCategory, Double> entry : costs.entrySet()) {
                leader.sendMessage(ChatColor.GRAY + "• " + entry.getKey().name() + ": " +
                        ChatColor.WHITE + String.format("%.2f", entry.getValue()));
            }
        } else {
            leader.sendMessage(ChatColor.RED + "Nation Budget: Your nation could not afford the budget cycle cost of " +
                    String.format("%.2f", totalCost) + ". Some penalties have been applied.");

            leader.sendMessage(ChatColor.YELLOW + "Consider adjusting your budget allocations or ensuring sufficient funds.");
        }
    }

    /**
     * Create a default budget allocation for a new town or nation
     */
    private Map<BudgetCategory, BudgetAllocation> createDefaultBudget(boolean isNation, int residents, int claims) {
        Map<BudgetCategory, BudgetAllocation> budget = new HashMap<>();
        double totalAllocation = 0;

        // Start with default allocations from config
        for (BudgetCategory category : BudgetCategory.values()) {
            double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
            double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

            // Default is midpoint of min and max
            double defaultPercent = (minPercent + maxPercent) / 2;
            budget.put(category, new BudgetAllocation(defaultPercent, 100));
            totalAllocation += defaultPercent;
        }

        // Normalize to 100% total
        if (Math.abs(totalAllocation - 100.0) > 0.001) {
            double scaleFactor = 100.0 / totalAllocation;
            for (Map.Entry<BudgetCategory, BudgetAllocation> entry : budget.entrySet()) {
                BudgetAllocation current = entry.getValue();
                double newPercentage = current.getPercentage() * scaleFactor;
                entry.setValue(new BudgetAllocation(newPercentage, current.getPriority()));
            }
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
     * Determine the funding level (underfunded, standard, overfunded)
     */
    private String getFundingLevel(BudgetAllocation allocation, double recommended) {
        double allocationRatio = (allocation.getPercentage() / 100.0);
        double underfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.underfunded", 70) / 100.0;
        double overfundedThreshold = plugin.getConfig().getDouble("budget.thresholds.overfunded", 130) / 100.0;

        if (allocationRatio < underfundedThreshold) {
            return "underfunded";
        } else if (allocationRatio > overfundedThreshold) {
            return "overfunded";
        } else {
            return "standard";
        }
    }

    /**
     * Apply budget effects for a town
     */
    private void applyTownBudgetEffects(Town town, BudgetCategory category, BudgetAllocation allocation, double recommended) {
        String fundingLevel = getFundingLevel(allocation, recommended);
        String configKey = "budget.categories." + category.getConfigKey() + ".effects." + fundingLevel;
        logger.fine("Town " + town.getName() + " has " + fundingLevel + " " + category.name() + " budget");

        // Apply category-specific effects
        switch (category) {
            case INFRASTRUCTURE:
                // Apply infrastructure effects
                applyInfrastructureEffects(town, false, configKey);
                break;

            case ADMINISTRATION:
                // Apply administration effects
                applyAdministrationEffects(town, false, configKey);
                break;

            case EDUCATION:
                // Apply education effects
                applyEducationEffects(town, false, configKey);
                break;

            case MILITARY:
                // Apply military effects
                applyMilitaryEffects(town, false, configKey);
                break;
        }
    }

    /**
     * Apply budget effects for a nation
     */
    private void applyNationBudgetEffects(Nation nation, BudgetCategory category, BudgetAllocation allocation, double recommended) {
        String fundingLevel = getFundingLevel(allocation, recommended);
        String configKey = "budget.categories." + category.getConfigKey() + ".effects." + fundingLevel;
        logger.fine("Nation " + nation.getName() + " has " + fundingLevel + " " + category.name() + " budget");

        // Apply category-specific effects
        switch (category) {
            case INFRASTRUCTURE:
                // Apply infrastructure effects
                applyNationInfrastructureEffects(nation, configKey);
                break;

            case ADMINISTRATION:
                // Apply administration effects
                applyNationAdministrationEffects(nation, configKey);
                break;

            case EDUCATION:
                // Apply education effects
                applyNationEducationEffects(nation, configKey);
                break;

            case MILITARY:
                // Apply military effects
                applyNationMilitaryEffects(nation, configKey);
                break;
        }
    }

    /**
     * Apply infrastructure effects for a town
     */
    private void applyInfrastructureEffects(Town town, boolean isNation, String effectsKey) {
        double claimCostMod = plugin.getConfig().getDouble(effectsKey + ".claim_cost_modifier", 1.0);
        double townBlockBonus = plugin.getConfig().getDouble(effectsKey + ".town_block_bonus", 1.0);
        double wallPlotsMod = plugin.getConfig().getDouble(effectsKey + ".wall_plots_modifier", 1.0);

        // Create the effects object
        InfrastructureEffects effects = new InfrastructureEffects(claimCostMod, townBlockBonus, wallPlotsMod);

        // Store these values in EffectsManager
        if (isNation) {
            plugin.getEffectsManager().registerNationInfrastructureEffects(
                    town.getNationOrNull().getUUID(), effects);
            logger.fine("Applied nation infrastructure effects - claim cost: " + claimCostMod +
                    ", block bonus: " + townBlockBonus);
        } else {
            plugin.getEffectsManager().registerTownInfrastructureEffects(town.getUUID(), effects);
            logger.fine("Applied town infrastructure effects - claim cost: " + claimCostMod +
                    ", block bonus: " + townBlockBonus);
        }
    }

    /**
     * Apply infrastructure effects for a nation
     */
    private void applyNationInfrastructureEffects(Nation nation, String effectsKey) {
        double claimCostMod = plugin.getConfig().getDouble(effectsKey + ".claim_cost_modifier", 1.0);
        double townBlockBonus = plugin.getConfig().getDouble(effectsKey + ".town_block_bonus", 1.0);
        double wallPlotsMod = plugin.getConfig().getDouble(effectsKey + ".wall_plots_modifier", 1.0);

        // Create the effects object
        InfrastructureEffects effects = new InfrastructureEffects(claimCostMod, townBlockBonus, wallPlotsMod);

        // Store in EffectsManager
        plugin.getEffectsManager().registerNationInfrastructureEffects(nation.getUUID(), effects);

        logger.fine("Applied nation infrastructure effects - claim cost: " + claimCostMod +
                ", block bonus: " + townBlockBonus);
    }

    /**
     * Apply administration effects for a town
     */
    private void applyAdministrationEffects(Town town, boolean isNation, String effectsKey) {
        double taxCollectionMod = plugin.getConfig().getDouble(effectsKey + ".tax_collection_modifier", 1.0);
        double corruptionGainMod = plugin.getConfig().getDouble(effectsKey + ".corruption_gain_modifier", 1.0);

        // Create effects object
        AdministrationEffects effects = new AdministrationEffects(taxCollectionMod, corruptionGainMod);

        // Store in effects manager
        if (isNation && town.hasNation()) {
            try {
                plugin.getEffectsManager().registerNationAdminEffects(town.getNation().getUUID(), effects);
                logger.fine("Applied nation admin effects for " + town.getNation().getName() +
                        " - tax collection: " + taxCollectionMod + ", corruption gain: " + corruptionGainMod);
            } catch (Exception e) {
                logger.warning("Error applying nation admin effects: " + e.getMessage());
            }
        } else {
            plugin.getEffectsManager().registerTownAdminEffects(town.getUUID(), effects);
            logger.fine("Applied town admin effects for " + town.getName() +
                    " - tax collection: " + taxCollectionMod + ", corruption gain: " + corruptionGainMod);
        }
    }

    /**
     * Apply administration effects for a nation
     */
    private void applyNationAdministrationEffects(Nation nation, String effectsKey) {
        double taxCollectionMod = plugin.getConfig().getDouble(effectsKey + ".tax_collection_modifier", 1.0);
        double corruptionGainMod = plugin.getConfig().getDouble(effectsKey + ".corruption_gain_modifier", 1.0);

        // Create effects object
        AdministrationEffects effects = new AdministrationEffects(taxCollectionMod, corruptionGainMod);

        // Store in effects manager
        plugin.getEffectsManager().registerNationAdminEffects(nation.getUUID(), effects);

        logger.fine("Applied nation admin effects for " + nation.getName() +
                " - tax collection: " + taxCollectionMod + ", corruption gain: " + corruptionGainMod);
    }

    /**
     * Apply education effects for a town
     */
    private void applyEducationEffects(Town town, boolean isNation, String effectsKey) {
        double ppGainMod = plugin.getConfig().getDouble(effectsKey + ".pp_gain_modifier", 1.0);
        double policyCostMod = plugin.getConfig().getDouble(effectsKey + ".policy_cost_modifier", 1.0);
        double technologyMod = plugin.getConfig().getDouble(effectsKey + ".technology_modifier", 1.0);

        // Create the effects object
        EducationEffects effects = new EducationEffects(ppGainMod, policyCostMod, technologyMod);

        // Store in effects manager
        if (isNation && town.hasNation()) {
            try {
                plugin.getEffectsManager().registerNationEducationEffects(town.getNation().getUUID(), effects);
                logger.fine("Applied nation education effects for " + town.getNation().getName() +
                        " - PP gain: " + ppGainMod + ", policy cost: " + policyCostMod);
            } catch (Exception e) {
                logger.warning("Error applying nation education effects: " + e.getMessage());
            }
        } else {
            plugin.getEffectsManager().registerTownEducationEffects(town.getUUID(), effects);
            logger.fine("Applied town education effects for " + town.getName() +
                    " - PP gain: " + ppGainMod + ", policy cost: " + policyCostMod);
        }
    }

    /**
     * Apply education effects for a nation
     */
    private void applyNationEducationEffects(Nation nation, String effectsKey) {
        double ppGainMod = plugin.getConfig().getDouble(effectsKey + ".pp_gain_modifier", 1.0);
        double policyCostMod = plugin.getConfig().getDouble(effectsKey + ".policy_cost_modifier", 1.0);
        double technologyMod = plugin.getConfig().getDouble(effectsKey + ".technology_modifier", 1.0);

        // Create effects object
        EducationEffects effects = new EducationEffects(ppGainMod, policyCostMod, technologyMod);

        // Store in effects manager
        plugin.getEffectsManager().registerNationEducationEffects(nation.getUUID(), effects);

        logger.fine("Applied nation education effects for " + nation.getName() +
                " - PP gain: " + ppGainMod + ", policy cost: " + policyCostMod);
    }

    /**
     * Apply military effects for a town
     */
    private void applyMilitaryEffects(Town town, boolean isNation, String effectsKey) {
        double strengthMod = plugin.getConfig().getDouble(effectsKey + ".strength_modifier", 1.0);
        double buildingDamageMod = plugin.getConfig().getDouble(effectsKey + ".building_damage_modifier", 1.0);

        // Create effects object
        MilitaryEffects effects = new MilitaryEffects(strengthMod, buildingDamageMod);

        // Store in effects manager
        if (isNation && town.hasNation()) {
            try {
                plugin.getEffectsManager().registerNationMilitaryEffects(town.getNation().getUUID(), effects);
                logger.fine("Applied nation military effects for " + town.getNation().getName() +
                        " - strength: " + strengthMod + ", building damage: " + buildingDamageMod);
            } catch (Exception e) {
                logger.warning("Error applying nation military effects: " + e.getMessage());
            }
        } else {
            plugin.getEffectsManager().registerTownMilitaryEffects(town.getUUID(), effects);
            logger.fine("Applied town military effects for " + town.getName() +
                    " - strength: " + strengthMod + ", building damage: " + buildingDamageMod);
        }
    }

    /**
     * Apply military effects for a nation
     */
    private void applyNationMilitaryEffects(Nation nation, String effectsKey) {
        double strengthMod = plugin.getConfig().getDouble(effectsKey + ".strength_modifier", 1.0);
        double buildingDamageMod = plugin.getConfig().getDouble(effectsKey + ".building_damage_modifier", 1.0);

        // Create effects object
        MilitaryEffects effects = new MilitaryEffects(strengthMod, buildingDamageMod);

        // Store in effects manager
        plugin.getEffectsManager().registerNationMilitaryEffects(nation.getUUID(), effects);

        logger.fine("Applied nation military effects for " + nation.getName() +
                " - strength: " + strengthMod + ", building damage: " + buildingDamageMod);
    }

    /**
     * Apply penalties when a town can't afford its budget
     */
    private void applyTownBudgetFailurePenalties(Town town, int failureStreak) {
        // Get the town's corruption manager
        TownCorruptionManager corruptionManager = plugin.getTownCorruptionManager();

        // Base corruption gain from budget failure
        double corruptionGain = 2.0 * failureStreak;  // Escalating corruption for repeated failures

        // Add corruption to the town
        corruptionManager.addCorruption(town, corruptionGain);

        logger.warning("Town " + town.getName() + " failed to pay budget (streak: " + failureStreak +
                "). Added " + corruptionGain + " corruption.");

        // For higher failure streaks, apply more severe penalties
        if (failureStreak >= 3) {
            // Reduce town political power if available
            TownPoliticalPowerManager ppManager = plugin.getTownPPManager();
            if (ppManager != null) {
                double currentPP = ppManager.getPoliticalPower(town);
                double reduction = currentPP * 0.1 * (failureStreak - 2); // 10% per streak above 2
                reduction = Math.min(reduction, currentPP); // Don't go negative

                if (reduction > 0) {
                    ppManager.removePoliticalPower(town, reduction);
                    logger.warning("Town " + town.getName() + " lost " + reduction +
                            " political power due to budget failure streak.");
                }
            }
        }
    }

    /**
     * Apply penalties when a nation can't afford its budget
     */
    private void applyNationBudgetFailurePenalties(Nation nation, int failureStreak) {
        // Get the nation's corruption manager
        CorruptionManager corruptionManager = plugin.getCorruptionManager();

        // Base corruption gain from budget failure
        double corruptionGain = 3.0 * failureStreak;  // Escalating corruption for repeated failures

        // Add corruption to the nation
        corruptionManager.addCorruption(nation, corruptionGain);

        logger.warning("Nation " + nation.getName() + " failed to pay budget (streak: " + failureStreak +
                "). Added " + corruptionGain + " corruption.");

        // For higher failure streaks, apply more severe penalties
        if (failureStreak >= 3) {
            // Reduce political power
            PoliticalPowerManager ppManager = plugin.getPPManager();
            double currentPP = ppManager.getPoliticalPower(nation);
            double reduction = currentPP * 0.15 * (failureStreak - 2); // 15% per streak above 2
            reduction = Math.min(reduction, currentPP); // Don't go negative

            if (reduction > 0) {
                ppManager.removePoliticalPower(nation, reduction);
                logger.warning("Nation " + nation.getName() + " lost " + reduction +
                        " political power due to budget failure streak.");
            }

            // Broadcast to all town mayors in the nation
            if (failureStreak >= 5) {
                String message = ChatColor.DARK_RED + "CRITICAL: " + ChatColor.RED +
                        "Your nation has failed to pay its budget for " + failureStreak +
                        " consecutive cycles. Severe penalties are being applied!";

                for (Town town : nation.getTowns()) {
                    if (town.getMayor() != null && town.getMayor().getPlayer() != null) {
                        town.getMayor().getPlayer().sendMessage(message);
                    }
                }
            }
        }
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
     * Set a budget allocation for a town, ensuring total allocations equal 100%
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

        // Calculate difference between new and current allocation
        double currentPercentage = budget.get(category).getPercentage();
        double difference = percentage - currentPercentage;

        if (difference == 0) {
            return true; // No change needed
        }

        // Adjust other categories to maintain 100% total
        adjustOtherCategories(budget, category, difference);

        // Update the allocation for the requested category
        BudgetAllocation current = budget.get(category);
        BudgetAllocation updated = new BudgetAllocation(percentage, current.getPriority());
        budget.put(category, updated);

        return true;
    }

    /**
     * Adjust other category allocations to maintain 100% total
     */
    private void adjustOtherCategories(Map<BudgetCategory, BudgetAllocation> budget, BudgetCategory excludeCategory, double difference) {
        // Count categories that can be adjusted
        List<BudgetCategory> adjustableCategories = new ArrayList<>();
        for (BudgetCategory cat : BudgetCategory.values()) {
            if (cat != excludeCategory) {
                adjustableCategories.add(cat);
            }
        }

        if (adjustableCategories.isEmpty()) {
            return;
        }

        // Distribute the difference equally among other categories
        double adjustmentPerCategory = -difference / adjustableCategories.size();

        for (BudgetCategory cat : adjustableCategories) {
            BudgetAllocation current = budget.get(cat);
            double newPercentage = current.getPercentage() + adjustmentPerCategory;

            // Ensure minimum percentage
            double minPercent = plugin.getConfig().getDouble("budget.categories." + cat.getConfigKey() + ".min_percent", 0);
            newPercentage = Math.max(minPercent, newPercentage);

            // Update allocation
            BudgetAllocation updated = new BudgetAllocation(newPercentage, current.getPriority());
            budget.put(cat, updated);
        }

        // Normalize to ensure exactly 100%
        normalizeAllocations(budget);
    }

    /**
     * Normalize allocations to ensure they total exactly 100%
     */
    private void normalizeAllocations(Map<BudgetCategory, BudgetAllocation> budget) {
        double total = 0;
        for (BudgetAllocation allocation : budget.values()) {
            total += allocation.getPercentage();
        }

        if (Math.abs(total - 100.0) < 0.001) {
            return; // Already totals 100%
        }

        // Scale all allocations to total 100%
        double scaleFactor = 100.0 / total;
        for (Map.Entry<BudgetCategory, BudgetAllocation> entry : budget.entrySet()) {
            BudgetAllocation current = entry.getValue();
            double newPercentage = current.getPercentage() * scaleFactor;
            BudgetAllocation updated = new BudgetAllocation(newPercentage, current.getPriority());
            entry.setValue(updated);
        }
    }

    /**
     * Set a budget allocation for a nation, ensuring total allocations equal 100%
     */
    public boolean setNationBudgetAllocation(Nation nation, BudgetCategory category, double percentage) {
        // Same validation logic as town method
        double minPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".min_percent", 0);
        double maxPercent = plugin.getConfig().getDouble("budget.categories." + category.getConfigKey() + ".max_percent", 100);

        if (percentage < minPercent || percentage > maxPercent) {
            return false;
        }

        // Get the nation's budget or create a default one
        Map<BudgetCategory, BudgetAllocation> budget = nationBudgets.computeIfAbsent(
                nation.getUUID(),
                k -> createDefaultBudget(true, nation.getNumResidents(), getTotalNationClaims(nation))
        );

        // Calculate difference between new and current allocation
        double currentPercentage = budget.get(category).getPercentage();
        double difference = percentage - currentPercentage;

        if (difference == 0) {
            return true; // No change needed
        }

        // Adjust other categories to maintain 100% total
        adjustOtherCategories(budget, category, difference);

        // Update the allocation for the requested category
        BudgetAllocation current = budget.get(category);
        BudgetAllocation updated = new BudgetAllocation(percentage, current.getPriority());
        budget.put(category, updated);

        return true;
    }

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

    /**
     * Get days until next budget cycle for an entity
     */
    public int getDaysUntilNextCycle(UUID entityId, boolean isNation) {
        long lastCycle = isNation ?
                nationLastBudgetCycles.getOrDefault(entityId, 0L) :
                townLastBudgetCycles.getOrDefault(entityId, 0L);

        if (lastCycle == 0L) {
            // No cycle yet, budget is due immediately
            return 0;
        }

        long now = System.currentTimeMillis();
        long timeElapsed = now - lastCycle;

        if (timeElapsed >= budgetCycleDuration) {
            // Budget is past due
            return 0;
        }

        long timeRemaining = budgetCycleDuration - timeElapsed;
        return (int) Math.ceil(timeRemaining / (24.0 * 60 * 60 * 1000));
    }

    //Get a formatted string with time until next budget cycle
    public String getFormattedTimeUntilNextCycle(UUID entityId, boolean isNation) {
        int days = getDaysUntilNextCycle(entityId, isNation);

        if (days == 0) {
            return "today";
        } else if (days == 1) {
            return "tomorrow";
        } else {
            return days + " days";
        }
    }
}