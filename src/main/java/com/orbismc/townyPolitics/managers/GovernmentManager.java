package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.GovernmentStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GovernmentManager {

    private final TownyPolitics plugin;
    private final GovernmentStorage storage;

    private Map<UUID, GovernmentType> townGovernments;
    private Map<UUID, GovernmentType> nationGovernments;

    public GovernmentManager(TownyPolitics plugin, GovernmentStorage storage) {
        this.plugin = plugin;
        this.storage = storage;

        this.townGovernments = new HashMap<>();
        this.nationGovernments = new HashMap<>();

        // Load data from storage
        loadData();
    }

    /**
     * Load government data from storage
     */
    public void loadData() {
        townGovernments.clear();
        nationGovernments.clear();

        townGovernments.putAll(storage.loadAllGovernments(false));
        nationGovernments.putAll(storage.loadAllGovernments(true));
    }

    /**
     * Get the government type of a town
     *
     * @param town The town
     * @return The government type
     */
    public GovernmentType getGovernmentType(Town town) {
        return townGovernments.getOrDefault(town.getUUID(), GovernmentType.AUTOCRACY);
    }

    /**
     * Get the government type of a nation
     *
     * @param nation The nation
     * @return The government type
     */
    public GovernmentType getGovernmentType(Nation nation) {
        return nationGovernments.getOrDefault(nation.getUUID(), GovernmentType.AUTOCRACY);
    }

    /**
     * Set the government type of a town
     *
     * @param town The town
     * @param type The government type
     */
    public void setGovernmentType(Town town, GovernmentType type) {
        townGovernments.put(town.getUUID(), type);
        storage.saveGovernment(town.getUUID(), type, false);
    }

    /**
     * Set the government type of a nation
     *
     * @param nation The nation
     * @param type The government type
     */
    public void setGovernmentType(Nation nation, GovernmentType type) {
        nationGovernments.put(nation.getUUID(), type);
        storage.saveGovernment(nation.getUUID(), type, true);
    }

    /**
     * Check if a town has a democratic government
     *
     * @param town The town
     * @return True if the town has a democratic government
     */
    public boolean hasDemocraticGovernment(Town town) {
        GovernmentType type = getGovernmentType(town);
        return type.isDemocracy();
    }

    /**
     * Check if a nation has a democratic government
     *
     * @param nation The nation
     * @return True if the nation has a democratic government
     */
    public boolean hasDemocraticGovernment(Nation nation) {
        GovernmentType type = getGovernmentType(nation);
        return type.isDemocracy();
    }
}