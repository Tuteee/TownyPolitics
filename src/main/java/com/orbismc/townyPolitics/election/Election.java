package com.orbismc.townyPolitics.election;

import com.palmergames.bukkit.towny.object.Resident;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Election {

    public enum ElectionType {
        TOWN, NATION
    }

    public enum ElectionStatus {
        CAMPAIGNING,
        VOTING,
        FINISHED,
        CANCELLED
    }

    private final UUID electionId;
    private final UUID entityId; // Town or Nation UUID
    private final ElectionType electionType;
    private ElectionStatus status;
    private final long startTime;
    private long campaignEndTime;
    private long votingEndTime;
    private final Map<UUID, String> candidates; // Resident UUID -> Resident Name
    private final Map<UUID, UUID> votes; // Voter UUID -> Candidate UUID
    private UUID winnerId; // Resident UUID of the winner

    // Configuration values (could be loaded from config)
    private static final long DEFAULT_CAMPAIGN_DURATION = 2 * 24 * 60 * 60 * 1000; // 2 days
    private static final long DEFAULT_VOTING_DURATION = 1 * 24 * 60 * 60 * 1000; // 1 day

    public Election(UUID entityId, ElectionType electionType) {
        this.electionId = UUID.randomUUID();
        this.entityId = entityId;
        this.electionType = electionType;
        this.status = ElectionStatus.CAMPAIGNING;
        this.startTime = System.currentTimeMillis();
        this.campaignEndTime = startTime + DEFAULT_CAMPAIGN_DURATION;
        this.votingEndTime = campaignEndTime + DEFAULT_VOTING_DURATION;
        this.candidates = new ConcurrentHashMap<>();
        this.votes = new ConcurrentHashMap<>();
        this.winnerId = null;
    }

    // Constructor for loading from storage
    public Election(UUID electionId, UUID entityId, ElectionType electionType, ElectionStatus status,
                    long startTime, long campaignEndTime, long votingEndTime,
                    Map<UUID, String> candidates, Map<UUID, UUID> votes, UUID winnerId) {
        this.electionId = electionId;
        this.entityId = entityId;
        this.electionType = electionType;
        this.status = status;
        this.startTime = startTime;
        this.campaignEndTime = campaignEndTime;
        this.votingEndTime = votingEndTime;
        this.candidates = new ConcurrentHashMap<>(candidates != null ? candidates : Collections.emptyMap());
        this.votes = new ConcurrentHashMap<>(votes != null ? votes : Collections.emptyMap());
        this.winnerId = winnerId;
    }

    // --- Getters ---
    public UUID getElectionId() { return electionId; }
    public UUID getEntityId() { return entityId; }
    public ElectionType getElectionType() { return electionType; }
    public ElectionStatus getStatus() { return status; }
    public long getStartTime() { return startTime; }
    public long getCampaignEndTime() { return campaignEndTime; }
    public long getVotingEndTime() { return votingEndTime; }
    public Map<UUID, String> getCandidates() { return Collections.unmodifiableMap(candidates); }
    public Map<UUID, UUID> getVotes() { return Collections.unmodifiableMap(votes); }
    public UUID getWinnerId() { return winnerId; }
    public boolean isTownElection() { return electionType == ElectionType.TOWN; }
    public boolean isNationElection() { return electionType == ElectionType.NATION; }

    // --- Status Checks ---
    public boolean isCampaigningActive() {
        return status == ElectionStatus.CAMPAIGNING && System.currentTimeMillis() < campaignEndTime;
    }

    public boolean isVotingActive() {
        return status == ElectionStatus.VOTING && System.currentTimeMillis() < votingEndTime;
    }

    public boolean hasEnded() {
        return status == ElectionStatus.FINISHED || status == ElectionStatus.CANCELLED || System.currentTimeMillis() >= votingEndTime;
    }

    // --- Actions ---
    public boolean addCandidate(Resident resident) {
        if (status != ElectionStatus.CAMPAIGNING || !isCampaigningActive()) {
            return false; // Can only run during active campaigning
        }
        return candidates.putIfAbsent(resident.getUUID(), resident.getName()) == null;
    }

    public boolean removeCandidate(Resident resident) {
        if (status != ElectionStatus.CAMPAIGNING || !isCampaigningActive()) {
            return false; // Can only withdraw during active campaigning
        }
        // Also remove any votes cast for this candidate if somehow they voted before withdrawing (shouldn't happen)
        votes.values().removeIf(candidateId -> candidateId.equals(resident.getUUID()));
        return candidates.remove(resident.getUUID()) != null;
    }

    public boolean castVote(Resident voter, Resident candidate) {
        if (status != ElectionStatus.VOTING || !isVotingActive()) {
            return false; // Can only vote during active voting
        }
        if (!candidates.containsKey(candidate.getUUID())) {
            return false; // Candidate not running
        }
        if (votes.containsKey(voter.getUUID())) {
            return false; // Already voted
        }
        votes.put(voter.getUUID(), candidate.getUUID());
        return true;
    }

    public void cancelElection() {
        if (status == ElectionStatus.VOTING || status == ElectionStatus.FINISHED) {
            return; // Cannot cancel once voting starts or election is finished
        }
        this.status = ElectionStatus.CANCELLED;
    }

    public void startVotingPeriod() {
        if (status == ElectionStatus.CAMPAIGNING) {
            // Check if campaign time has ended
            if (System.currentTimeMillis() >= campaignEndTime) {
                if (candidates.isEmpty()) {
                    // No candidates ran, cancel the election
                    this.status = ElectionStatus.CANCELLED;
                } else {
                    // Lock candidates and start voting
                    this.status = ElectionStatus.VOTING;
                    // Adjust voting end time if needed (e.g., if campaigning ended early)
                    this.votingEndTime = Math.max(System.currentTimeMillis(), campaignEndTime) + DEFAULT_VOTING_DURATION;
                }
            }
            // else: Campaigning period not over yet
        }
    }

    public void concludeElection() {
        if (status != ElectionStatus.VOTING || System.currentTimeMillis() < votingEndTime) {
            return; // Not time to conclude yet or already concluded/cancelled
        }

        if (candidates.isEmpty()) {
            this.status = ElectionStatus.CANCELLED;
            return;
        }

        Map<UUID, Integer> voteCounts = new HashMap<>();
        for (UUID candidateId : candidates.keySet()) {
            voteCounts.put(candidateId, 0);
        }
        for (UUID votedFor : votes.values()) {
            voteCounts.computeIfPresent(votedFor, (k, v) -> v + 1);
        }

        int maxVotes = -1;
        List<UUID> topCandidates = new ArrayList<>();

        if (votes.isEmpty()) {
            // Zero votes cast, choose randomly from all candidates
            topCandidates.addAll(candidates.keySet());
        } else {
            for (Map.Entry<UUID, Integer> entry : voteCounts.entrySet()) {
                if (entry.getValue() > maxVotes) {
                    maxVotes = entry.getValue();
                    topCandidates.clear();
                    topCandidates.add(entry.getKey());
                } else if (entry.getValue() == maxVotes) {
                    topCandidates.add(entry.getKey());
                }
            }
        }

        // Determine winner
        if (topCandidates.isEmpty()) {
            // Should not happen if there were candidates, but handle defensively
            this.status = ElectionStatus.CANCELLED;
        } else if (topCandidates.size() == 1) {
            this.winnerId = topCandidates.get(0);
        } else {
            // Tie-breaker: random selection among tied candidates
            Random random = new Random();
            this.winnerId = topCandidates.get(random.nextInt(topCandidates.size()));
        }

        this.status = ElectionStatus.FINISHED;
    }

    // --- Helper Methods ---
    public String getFormattedTimeRemaining(long endTime) {
        if (endTime <= 0) return "N/A";
        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) return "Ended";

        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours %= 24;
        minutes %= 60;

        if (days > 0) return String.format("%dd %dh", days, hours);
        if (hours > 0) return String.format("%dh %dm", hours, minutes);
        return String.format("%dm", minutes);
    }

    public String getStatusString() {
        return switch (status) {
            case CAMPAIGNING -> "Campaigning (" + getFormattedTimeRemaining(campaignEndTime) + " left)";
            case VOTING -> "Voting (" + getFormattedTimeRemaining(votingEndTime) + " left)";
            case FINISHED -> "Finished" + (winnerId != null ? " (Winner: " + candidates.getOrDefault(winnerId, "Unknown") + ")" : "");
            case CANCELLED -> "Cancelled";
        };
    }

    public Map<String, Integer> getVoteCounts() {
        Map<String, Integer> results = new LinkedHashMap<>(); // Use LinkedHashMap to maintain order if needed
        Map<UUID, Integer> counts = new HashMap<>();

        // Initialize counts for all candidates
        for (UUID candidateId : candidates.keySet()) {
            counts.put(candidateId, 0);
        }

        // Count votes
        for (UUID candidateId : votes.values()) {
            counts.computeIfPresent(candidateId, (k, v) -> v + 1);
        }

        // Convert UUIDs to names for the result map
        for (Map.Entry<UUID, Integer> entry : counts.entrySet()) {
            String candidateName = candidates.getOrDefault(entry.getKey(), "Unknown Candidate");
            results.put(candidateName, entry.getValue());
        }

        return results;
    }
}