// ITownGovernmentStorage.java
package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.government.GovernmentType;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for Town Government storage implementations
 */
public interface ITownGovernmentStorage {
    void saveGovernment(UUID uuid, GovernmentType type);
    void saveChangeTime(UUID uuid, long timestamp);
    GovernmentType getGovernment(UUID uuid);
    long getChangeTime(UUID uuid);
    Map<UUID, GovernmentType> loadAllGovernments();
    Map<UUID, Long> loadAllChangeTimes();
    void saveAll();
}