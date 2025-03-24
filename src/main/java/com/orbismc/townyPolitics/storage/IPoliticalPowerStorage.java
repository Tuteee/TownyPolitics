package com.orbismc.townyPolitics.storage;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Political Power storage implementations
 */
public interface IPoliticalPowerStorage {
    void savePP(UUID nationUUID, double amount);
    Map<UUID, Double> loadAllPP();
    void saveAll();
}