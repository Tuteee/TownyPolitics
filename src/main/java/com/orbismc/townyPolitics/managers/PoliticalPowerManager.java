package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.listeners.TownyEventListener;
import com.orbismc.townyPolitics.storage.IPoliticalPowerStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoliticalPowerManager {

    private final TownyPolitics plugin;
    private final IPoliticalPowerStorage storage;
    private final Map<UUID, Double> nationPP; // Cache of nation UUIDs to their political power
    private TownyEventListener eventListener;

    // Maximum political power limit
    private final double MAX_PP = 1000.0;

    public PoliticalPowerManager(TownyPolitics plugin, IPoliticalPowerStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.nationPP = new HashMap<>();

        // Load data from storage
        loadData();
    }

    public void setEventListener(TownyEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public void loadData() {
        nationPP.clear();
        nationPP.putAll(storage.loadAllPP());
    }

    public double getPoliticalPower(Nation nation) {
        return nationPP.getOrDefault(nation.getUUID(), 0.0);
    }

    public void setPoliticalPower(Nation nation, double amount) {
        // Ensure PP can't go below 0 or above MAX_PP
        double newAmount = Math.min(MAX_PP, Math.max(0, amount));
        nationPP.put(nation.getUUID(), newAmount);
        storage.savePP(nation.getUUID(), newAmount);

        // Update the nation's information if the event listener is available
        if (eventListener != null) {
            try {
                eventListener.updateNationPoliticalPowerMetadata(nation);
            } catch (Exception e) {
                plugin.getLogger().warning("Error updating political power display: " + e.getMessage());
            }
        }
    }

    public void addPoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        setPoliticalPower(nation, current + amount);
    }

    public boolean removePoliticalPower(Nation nation, double amount) {
        double current = getPoliticalPower(nation);
        if (current >= amount) {
            setPoliticalPower(nation, current - amount);
            return true;
        }
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

        return Math.min(maxGain, Math.max(minGain, ppGain)); // Ensure between minGain and maxGain
    }

    public void processNewDay() {
        plugin.getTownyAPI().getNations().forEach(nation -> {
            double gain = calculateDailyPPGain(nation);
            addPoliticalPower(nation, gain);
            plugin.getLogger().info("Nation " + nation.getName() + " gained " + String.format("%.2f", gain) + " political power");
        });
    }
}