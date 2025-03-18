package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townypolitics.TownyPolitics;
import com.orbismc.townyPolitics.storage.PoliticalPowerStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoliticalPowerManager {

    private final TownyPolitics plugin;
    private final PoliticalPowerStorage storage;
    private final Map<UUID, Double> nationPP; // Cache of nation UUIDs to their political power

    public PoliticalPowerManager(TownyPolitics plugin, PoliticalPowerStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.nationPP = new HashMap<>();

        // Load data from storage
        loadData();
    }

    /**
     * Load political power data from storage
     */
    public void loadData() {
        nationPP.clear();
        nationPP.putAll(storage.loadAllPP());
    }

    /**
     * Get the political power of a nation
     *
     * @param nation The nation
     * @return The political power
     */
    public double getPoliticalPower(Nation nation) {
        return nationPP.getOrDefault(nation.getUUID(), 0.0);
    }

    /**
     * Set the political power of a nation
     *
     * @param nation The nation
     * @param amount The amount
     */
    public void setPoliticalPower(Nation nation, double amount) {
        nationPP.put(nation.getUUID(), Math.max(0, amount)); // Ensure PP can't go below 0
        storage.savePP(nation.getUUID(), Math.max(0, amount));
    }

    /**
     * Add political power to a nation
     *
     * @param nation The nation
     * @param amount The amount to add
     */
    public void addPoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        setPoliticalPower(nation, current + amount);
    }

    /**
     * Remove political power from a nation
     *
     * @param nation The nation
     * @param amount The amount to remove
     * @return true if the nation had enough PP, false otherwise
     */
    public boolean removePoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        if (current >= amount) {
            setPoliticalPower(nation, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Calculate the daily political power gain for a nation based on resident count
     *
     * @param nation The nation
     * @return The political power gain
     */
    public double calculateDailyPPGain(Nation nation) {
        int residents = nation.getNumResidents();

        // Calculate PP gain based on resident count
        double ppGain;
        if (residents <= 0) {
            ppGain = 0; // No residents, no PP
        } else if (residents == 1) {
            ppGain = 1.0; // 1 resident = 1 PP
        } else if (residents <= 5) {
            // Scale linearly from 1.0 to 1.5 PP for 1-5 residents
            ppGain = 1.0 + (residents - 1) * 0.125;
        } else if (residents <= 10) {
            // Scale linearly from 1.5 to 2.0 PP for 5-10 residents
            ppGain = 1.5 + (residents - 5) * 0.1;
        } else {
            // More complex scaling for larger nations
            // This is a simple logarithmic scale that caps at 5 PP
            ppGain = Math.min(5.0, 2.0 + Math.log10(residents / 10.0) * 2);
        }

        return Math.min(5.0, Math.max(1.0, ppGain)); // Ensure between 1 and 5
    }

    /**
     * Process the new day event for all nations
     */
    public void processNewDay() {
        plugin.getTownyAPI().getNations().forEach(nation -> {
            double gain = calculateDailyPPGain(nation);
            addPoliticalPower(nation, gain);
            plugin.getLogger().info("Nation " + nation.getName() + " gained " + String.format("%.2f", gain) + " political power");
        });
    }
}