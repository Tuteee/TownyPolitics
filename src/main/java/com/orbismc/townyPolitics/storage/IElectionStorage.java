package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.election.Election;

import java.util.Map;
import java.util.UUID;

public interface IElectionStorage {

    /**
     * Save or update an election's state.
     * @param election The election object to save.
     */
    void saveElection(Election election);

    /**
     * Delete an election record (used when concluded or cancelled).
     * @param electionId The UUID of the election to delete.
     */
    void deleteElection(UUID electionId);

    /**
     * Load a specific election by its ID.
     * @param electionId The UUID of the election.
     * @return The Election object, or null if not found.
     */
    Election loadElection(UUID electionId);

    /**
     * Load all active (not finished or cancelled) elections.
     * @param isNation True to load nation elections, false for town elections.
     * @return A map of Entity UUID (Town/Nation) to its active Election.
     */
    Map<UUID, Election> loadAllActiveElections(boolean isNation);

    /**
     * General save all method (might not be strictly needed if saves happen individually).
     */
    void saveAll();
}