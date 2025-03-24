package com.orbismc.townyPolitics.storage;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Corruption storage implementations
 */
public interface ICorruptionStorage {
    void saveCorruption(UUID uuid, double amount, boolean isNation);
    Map<UUID, Double> loadAllCorruption(boolean isNation);
    void saveAll();
}