package com.orbismc.townyPolitics.election;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.managers.Manager;
import com.orbismc.townyPolitics.storage.IElectionStorage;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import com.palmergames.bukkit.towny.TownyAPI;
// Removed unused import: com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ElectionManager implements Manager {

    private final TownyPolitics plugin;
    private final IElectionStorage storage;
    private final DelegateLogger logger;
    private final TownyAPI townyAPI;

    private final Map<UUID, Election> activeTownElections; // Town UUID -> Election
    private final Map<UUID, Election> activeNationElections; // Nation UUID -> Election
    private BukkitTask electionUpdateTask;

    public ElectionManager(TownyPolitics plugin, IElectionStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.logger = new DelegateLogger(plugin, "ElectionManager");
        this.townyAPI = TownyAPI.getInstance();
        this.activeTownElections = new ConcurrentHashMap<>();
        this.activeNationElections = new ConcurrentHashMap<>();
        loadData();
        startUpdateTask();
    }

    @Override
    public void loadData() {
        activeTownElections.clear();
        activeNationElections.clear();
        activeTownElections.putAll(storage.loadAllActiveElections(false));
        activeNationElections.putAll(storage.loadAllActiveElections(true));
        logger.info("Loaded " + activeTownElections.size() + " active town elections.");
        logger.info("Loaded " + activeNationElections.size() + " active nation elections.");
        // Remove any already finished/cancelled elections that might have loaded
        activeTownElections.values().removeIf(Election::hasEnded);
        activeNationElections.values().removeIf(Election::hasEnded);
    }

    @Override
    public void saveAllData() {
        // Saving happens as elections update, but ensure final state is saved
        activeTownElections.values().forEach(storage::saveElection);
        activeNationElections.values().forEach(storage::saveElection);
        logger.info("Saved election data.");
    }

    public void stopUpdateTask() {
        if (electionUpdateTask != null && !electionUpdateTask.isCancelled()) {
            electionUpdateTask.cancel();
            logger.info("Election update task stopped.");
        }
    }

    private void startUpdateTask() {
        if (electionUpdateTask != null && !electionUpdateTask.isCancelled()) {
            electionUpdateTask.cancel(); // Cancel existing task if any
        }
        // Run every minute to check election statuses
        electionUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateElections, 1200L, 1200L); // Check every minute (1200 ticks)
        logger.info("Election update task started.");
    }

    private void updateElections() {
        long now = System.currentTimeMillis();

        // Update Town Elections
        for (Election election : new ArrayList<>(activeTownElections.values())) { // Iterate over a copy
            boolean changed = false;
            if (election.getStatus() == Election.ElectionStatus.CAMPAIGNING && now >= election.getCampaignEndTime()) {
                election.startVotingPeriod();
                logger.info("Town election " + election.getElectionId() + " for " + election.getEntityId() + " transitioned to " + election.getStatus());
                notifyTown(election.getEntityId(), ChatColor.GOLD + "The election campaigning period has ended. Voting has begun!");
                if (election.getStatus() == Election.ElectionStatus.CANCELLED) {
                    notifyTown(election.getEntityId(), ChatColor.RED + "The election was cancelled because no candidates ran.");
                }
                changed = true;
            } else if (election.getStatus() == Election.ElectionStatus.VOTING && now >= election.getVotingEndTime()) {
                election.concludeElection();
                logger.info("Town election " + election.getElectionId() + " for " + election.getEntityId() + " concluded. Winner: " + election.getWinnerId());
                if (election.getWinnerId() != null) {
                    Resident winner = townyAPI.getResident(election.getWinnerId());
                    String winnerName = winner != null ? winner.getName() : "Unknown";
                    notifyTown(election.getEntityId(), ChatColor.GREEN + "The election has concluded! The new Mayor is " + winnerName + "!");
                    assignTownRank(election.getEntityId(), election.getWinnerId());
                } else {
                    notifyTown(election.getEntityId(), ChatColor.YELLOW + "The election has concluded, but there was no winner.");
                }
                changed = true;
            }

            if (changed) {
                storage.saveElection(election);
                if (election.hasEnded()) {
                    activeTownElections.remove(election.getEntityId());
                }
            }
        }

        // Update Nation Elections
        for (Election election : new ArrayList<>(activeNationElections.values())) { // Iterate over a copy
            boolean changed = false;
            if (election.getStatus() == Election.ElectionStatus.CAMPAIGNING && now >= election.getCampaignEndTime()) {
                election.startVotingPeriod();
                logger.info("Nation election " + election.getElectionId() + " for " + election.getEntityId() + " transitioned to " + election.getStatus());
                notifyNation(election.getEntityId(), ChatColor.GOLD + "The election campaigning period has ended. Voting has begun!");
                if (election.getStatus() == Election.ElectionStatus.CANCELLED) {
                    notifyNation(election.getEntityId(), ChatColor.RED + "The election was cancelled because no candidates ran.");
                }
                changed = true;
            } else if (election.getStatus() == Election.ElectionStatus.VOTING && now >= election.getVotingEndTime()) {
                election.concludeElection();
                logger.info("Nation election " + election.getElectionId() + " for " + election.getEntityId() + " concluded. Winner: " + election.getWinnerId());
                if (election.getWinnerId() != null) {
                    Resident winner = townyAPI.getResident(election.getWinnerId());
                    String winnerName = winner != null ? winner.getName() : "Unknown";
                    notifyNation(election.getEntityId(), ChatColor.GREEN + "The election has concluded! The new Leader is " + winnerName + "!");
                    assignNationRank(election.getEntityId(), election.getWinnerId());
                } else {
                    notifyNation(election.getEntityId(), ChatColor.YELLOW + "The election has concluded, but there was no winner.");
                }
                changed = true;
            }
            if (changed) {
                storage.saveElection(election);
                if (election.hasEnded()) {
                    activeNationElections.remove(election.getEntityId());
                }
            }
        }
    }

    public Election getActiveTownElection(UUID townId) {
        return activeTownElections.get(townId);
    }

    public Election getActiveNationElection(UUID nationId) {
        return activeNationElections.get(nationId);
    }

    public boolean startElection(Town town) {
        if (activeTownElections.containsKey(town.getUUID())) {
            return false; // Election already active
        }
        Election election = new Election(town.getUUID(), Election.ElectionType.TOWN);
        activeTownElections.put(town.getUUID(), election);
        storage.saveElection(election);
        logger.info("Started new town election for " + town.getName());
        notifyTown(town.getUUID(), ChatColor.GREEN + "A mayoral election has begun! Use '/t election run' to become a candidate.");
        return true;
    }

    public boolean startElection(Nation nation) {
        if (activeNationElections.containsKey(nation.getUUID())) {
            return false; // Election already active
        }
        Election election = new Election(nation.getUUID(), Election.ElectionType.NATION);
        activeNationElections.put(nation.getUUID(), election);
        storage.saveElection(election);
        logger.info("Started new nation election for " + nation.getName());
        notifyNation(nation.getUUID(), ChatColor.GREEN + "A national election has begun! Use '/n election run' to become a candidate.");
        return true;
    }

    public boolean cancelElection(Town town) {
        Election election = activeTownElections.get(town.getUUID());
        if (election == null || election.getStatus() == Election.ElectionStatus.VOTING || election.getStatus() == Election.ElectionStatus.FINISHED) {
            return false; // No active election or cannot cancel
        }
        election.cancelElection();
        storage.saveElection(election); // Save cancelled status
        activeTownElections.remove(town.getUUID()); // Remove from active map
        logger.info("Cancelled town election for " + town.getName());
        notifyTown(town.getUUID(), ChatColor.RED + "The town election has been cancelled.");
        return true;
    }

    public boolean cancelElection(Nation nation) {
        Election election = activeNationElections.get(nation.getUUID());
        if (election == null || election.getStatus() == Election.ElectionStatus.VOTING || election.getStatus() == Election.ElectionStatus.FINISHED) {
            return false; // No active election or cannot cancel
        }
        election.cancelElection();
        storage.saveElection(election); // Save cancelled status
        activeNationElections.remove(nation.getUUID()); // Remove from active map
        logger.info("Cancelled nation election for " + nation.getName());
        notifyNation(nation.getUUID(), ChatColor.RED + "The nation election has been cancelled.");
        return true;
    }

    public boolean addCandidate(Town town, Resident resident) {
        Election election = activeTownElections.get(town.getUUID());
        if (election == null || !election.isCampaigningActive()) {
            return false;
        }
        boolean success = election.addCandidate(resident);
        if (success) {
            storage.saveElection(election);
            logger.info(resident.getName() + " is now running for mayor in " + town.getName());
        }
        return success;
    }

    public boolean addCandidate(Nation nation, Resident resident) {
        Election election = activeNationElections.get(nation.getUUID());
        if (election == null || !election.isCampaigningActive()) {
            return false;
        }
        boolean success = election.addCandidate(resident);
        if (success) {
            storage.saveElection(election);
            logger.info(resident.getName() + " is now running for leader in " + nation.getName());
        }
        return success;
    }

    public boolean removeCandidate(Town town, Resident resident) {
        Election election = activeTownElections.get(town.getUUID());
        if (election == null || !election.isCampaigningActive()) {
            return false;
        }
        boolean success = election.removeCandidate(resident);
        if (success) {
            storage.saveElection(election);
            logger.info(resident.getName() + " withdrew from the mayoral election in " + town.getName());
        }
        return success;
    }

    public boolean removeCandidate(Nation nation, Resident resident) {
        Election election = activeNationElections.get(nation.getUUID());
        if (election == null || !election.isCampaigningActive()) {
            return false;
        }
        boolean success = election.removeCandidate(resident);
        if (success) {
            storage.saveElection(election);
            logger.info(resident.getName() + " withdrew from the national election in " + nation.getName());
        }
        return success;
    }

    public boolean castVote(Town town, Resident voter, Resident candidate) {
        Election election = activeTownElections.get(town.getUUID());
        if (election == null || !election.isVotingActive()) {
            return false;
        }
        boolean success = election.castVote(voter, candidate);
        if (success) {
            storage.saveElection(election); // Save the vote implicitly by saving the election state
            logger.fine(voter.getName() + " voted for " + candidate.getName() + " in town " + town.getName());
        }
        return success;
    }

    public boolean castVote(Nation nation, Resident voter, Resident candidate) {
        Election election = activeNationElections.get(nation.getUUID());
        if (election == null || !election.isVotingActive()) {
            return false;
        }
        boolean success = election.castVote(voter, candidate);
        if (success) {
            storage.saveElection(election); // Save the vote implicitly by saving the election state
            logger.fine(voter.getName() + " voted for " + candidate.getName() + " in nation " + nation.getName());
        }
        return success;
    }


    // --- Helper Methods ---

    private void assignTownRank(UUID townId, UUID winnerId) {
        Town town = townyAPI.getTown(townId);
        Resident winner = townyAPI.getResident(winnerId);
        if (town != null && winner != null) {
            try {
                // Directly set the new mayor. Towny's setMayor should handle previous mayor demotion.
                town.setMayor(winner);
                // Save changes
                plugin.getTownyAPI().getDataSource().saveTown(town);
                logger.info("Assigned mayor rank to " + winner.getName() + " in town " + town.getName());
            } catch (Exception e) { // Catch general exceptions
                logger.severe("An unexpected error occurred while assigning mayor rank: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.warning("Could not assign mayor rank: Town or Winner not found. TownID: " + townId + ", WinnerID: " + winnerId);
        }
    }

    private void assignNationRank(UUID nationId, UUID winnerId) {
        Nation nation = townyAPI.getNation(nationId);
        Resident winner = townyAPI.getResident(winnerId);
        if (nation != null && winner != null) {
            try {
                // Directly set the new king/leader. Towny's setKing should handle demotion.
                nation.setKing(winner);
                // Save changes
                plugin.getTownyAPI().getDataSource().saveNation(nation);
                logger.info("Assigned leader rank to " + winner.getName() + " in nation " + nation.getName());
            } catch (Exception e) { // Catch general exceptions
                logger.severe("An unexpected error occurred while assigning leader rank: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            logger.warning("Could not assign leader rank: Nation or Winner not found. NationID: " + nationId + ", WinnerID: " + winnerId);
        }
    }

    private void notifyTown(UUID townId, String message) {
        Town town = townyAPI.getTown(townId);
        if (town != null) {
            // Send message to all online residents of the town
            for (Resident res : town.getResidents()) {
                Player player = Bukkit.getPlayer(res.getUUID());
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }

    private void notifyNation(UUID nationId, String message) {
        Nation nation = townyAPI.getNation(nationId);
        if (nation != null) {
            // Send message to all online members of the nation
            for (Resident res : nation.getResidents()) {
                Player player = Bukkit.getPlayer(res.getUUID());
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }
}