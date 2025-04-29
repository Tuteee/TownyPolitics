package com.orbismc.townyPolitics.storage.mysql;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.election.Election;
import com.orbismc.townyPolitics.storage.AbstractMySQLStorage;
import com.orbismc.townyPolitics.storage.IElectionStorage;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLElectionStorage extends AbstractMySQLStorage implements IElectionStorage {

    private final Gson gson = new Gson();
    private final Type candidateMapType = new TypeToken<Map<UUID, String>>() {}.getType();
    private final Type voteMapType = new TypeToken<Map<UUID, UUID>>() {}.getType();

    private final String TABLE_NAME;

    public MySQLElectionStorage(TownyPolitics plugin, DatabaseManager dbManager) {
        super(plugin, dbManager, "MySQLElectionStorage");
        this.TABLE_NAME = prefix + "elections";
        createTable();
        logger.info("MySQL Election Storage initialized");
    }

    private void createTable() {
        // Use LONGTEXT for JSON fields as they can become large
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "election_id VARCHAR(36) PRIMARY KEY, " +
                "entity_id VARCHAR(36) NOT NULL, " +
                "election_type VARCHAR(10) NOT NULL, " +
                "status VARCHAR(15) NOT NULL, " +
                "start_time BIGINT NOT NULL, " +
                "campaign_end_time BIGINT NOT NULL, " +
                "voting_end_time BIGINT NOT NULL, " +
                "candidates LONGTEXT, " + // Store as JSON string
                "votes LONGTEXT, " +      // Store as JSON string
                "winner_id VARCHAR(36), " +
                "INDEX idx_entity_id (entity_id), " + // Index for faster lookups by town/nation
                "INDEX idx_status (status)" +         // Index for faster lookup of active elections
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            logger.fine("Created/verified election table: " + TABLE_NAME);
        } catch (SQLException e) {
            logger.severe("Failed to create election table: " + e.getMessage());
        }
    }

    @Override
    public void saveElection(Election election) {
        String sql = "INSERT INTO " + TABLE_NAME + " (election_id, entity_id, election_type, status, start_time, campaign_end_time, voting_end_time, candidates, votes, winner_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "entity_id = VALUES(entity_id), election_type = VALUES(election_type), status = VALUES(status), " +
                "start_time = VALUES(start_time), campaign_end_time = VALUES(campaign_end_time), voting_end_time = VALUES(voting_end_time), " +
                "candidates = VALUES(candidates), votes = VALUES(votes), winner_id = VALUES(winner_id)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, election.getElectionId().toString());
            pstmt.setString(2, election.getEntityId().toString());
            pstmt.setString(3, election.getElectionType().name());
            pstmt.setString(4, election.getStatus().name());
            pstmt.setLong(5, election.getStartTime());
            pstmt.setLong(6, election.getCampaignEndTime());
            pstmt.setLong(7, election.getVotingEndTime());
            pstmt.setString(8, gson.toJson(election.getCandidates()));
            pstmt.setString(9, gson.toJson(election.getVotes()));
            pstmt.setString(10, election.getWinnerId() != null ? election.getWinnerId().toString() : null);

            pstmt.executeUpdate();
            logger.fine("Saved election: " + election.getElectionId());

        } catch (SQLException e) {
            logger.severe("Failed to save election " + election.getElectionId() + ": " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
        }
    }


    @Override
    public void deleteElection(UUID electionId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE election_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, electionId.toString());
            pstmt.executeUpdate();
            logger.fine("Deleted election: " + electionId);
        } catch (SQLException e) {
            logger.severe("Failed to delete election " + electionId + ": " + e.getMessage());
        }
    }

    @Override
    public Election loadElection(UUID electionId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE election_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, electionId.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToElection(rs);
            }
        } catch (SQLException e) {
            logger.severe("Failed to load election " + electionId + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public Map<UUID, Election> loadAllActiveElections(boolean isNation) {
        Map<UUID, Election> activeElections = new ConcurrentHashMap<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE election_type = ? AND status IN (?, ?)"; // CAMPAIGNING or VOTING

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, (isNation ? Election.ElectionType.NATION : Election.ElectionType.TOWN).name());
            pstmt.setString(2, Election.ElectionStatus.CAMPAIGNING.name());
            pstmt.setString(3, Election.ElectionStatus.VOTING.name());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Election election = mapResultSetToElection(rs);
                if (election != null) {
                    // Double check if it's actually ended based on time, might have missed an update cycle
                    if (!election.hasEnded()) {
                        activeElections.put(election.getEntityId(), election);
                    } else {
                        // If DB load shows it ended but status wasn't updated, update it now
                        if (election.getStatus() != Election.ElectionStatus.FINISHED && election.getStatus() != Election.ElectionStatus.CANCELLED) {
                            logger.warning("Found ended election in DB with non-final status: " + election.getElectionId() + ". Updating status.");
                            election.concludeElection(); // Try to conclude it properly
                            saveElection(election); // Save the final state
                        }
                    }
                }
            }
            logger.fine("Loaded " + activeElections.size() + " active " + (isNation ? "nation" : "town") + " elections.");

        } catch (SQLException e) {
            logger.severe("Failed to load active elections: " + e.getMessage());
        }
        return activeElections;
    }

    private Election mapResultSetToElection(ResultSet rs) throws SQLException {
        try {
            UUID electionId = UUID.fromString(rs.getString("election_id"));
            UUID entityId = UUID.fromString(rs.getString("entity_id"));
            Election.ElectionType type = Election.ElectionType.valueOf(rs.getString("election_type"));
            Election.ElectionStatus status = Election.ElectionStatus.valueOf(rs.getString("status"));
            long startTime = rs.getLong("start_time");
            long campaignEnd = rs.getLong("campaign_end_time");
            long votingEnd = rs.getLong("voting_end_time");
            String winnerIdStr = rs.getString("winner_id");
            UUID winnerId = (winnerIdStr != null) ? UUID.fromString(winnerIdStr) : null;

            String candidatesJson = rs.getString("candidates");
            String votesJson = rs.getString("votes");

            Map<UUID, String> candidates = gson.fromJson(candidatesJson, candidateMapType);
            Map<UUID, UUID> votes = gson.fromJson(votesJson, voteMapType);

            // Ensure maps are not null after deserialization
            if (candidates == null) candidates = new HashMap<>();
            if (votes == null) votes = new HashMap<>();


            return new Election(electionId, entityId, type, status, startTime, campaignEnd, votingEnd, candidates, votes, winnerId);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid data format in election table: " + e.getMessage() + " - Election ID: " + rs.getString("election_id"));
            return null;
        }
    }
}