package com.orbismc.townyPolitics.storage;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Town Political Power storage implementations
 */
public interface ITownPoliticalPowerStorage {
    void savePP(UUID townUUID, double amount);
    Map<UUID, Double> loadAllPP();
    void saveAll();
}