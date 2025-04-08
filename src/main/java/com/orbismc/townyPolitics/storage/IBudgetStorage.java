package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.budget.BudgetAllocation;
import com.orbismc.townyPolitics.budget.BudgetCategory;

import java.util.Map;
import java.util.UUID;

public interface IBudgetStorage {
    /**
     * Save a budget allocation for an entity
     */
    void saveBudgetAllocation(UUID entityId, BudgetCategory category, BudgetAllocation allocation, boolean isNation);

    /**
     * Save the last budget cycle time for an entity
     */
    void saveLastBudgetCycle(UUID entityId, long timestamp, boolean isNation);

    /**
     * Load all budget allocations for towns or nations
     */
    Map<UUID, Map<BudgetCategory, BudgetAllocation>> loadAllBudgetAllocations(boolean isNation);

    /**
     * Load all last budget cycle times for towns or nations
     */
    Map<UUID, Long> loadAllLastBudgetCycles(boolean isNation);

    /**
     * Save all data
     */
    void saveAll();
}