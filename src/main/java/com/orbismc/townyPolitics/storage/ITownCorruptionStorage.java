// ITownCorruptionStorage.java
package com.orbismc.townyPolitics.storage;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Town Corruption storage implementations
 */
public interface ITownCorruptionStorage {
    void saveCorruption(UUID uuid, double amount);
    Map<UUID, Double> loadAllCorruption();
    void saveAll();
}