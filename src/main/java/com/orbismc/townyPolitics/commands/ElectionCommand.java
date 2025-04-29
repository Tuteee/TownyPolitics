package com.orbismc.townyPolitics.commands;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.commands.base.BaseCommand; // Assuming you have this base class
import com.orbismc.townyPolitics.election.Election;
import com.orbismc.townyPolitics.election.ElectionManager;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ElectionCommand extends BaseCommand {

    private final ElectionManager electionManager;
    private final String commandSource; // "town" or "nation"

    public ElectionCommand(TownyPolitics plugin, ElectionManager electionManager, String commandSource) {
        super(plugin, "ElectionCommand"); // Pass logger prefix
        this.electionManager = electionManager;
        this.commandSource = commandSource;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;
        Resident resident = getResident(player);
        if (resident == null) return true;

        if (args.length == 0) {
            showElectionInfo(player, resident);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStart(player, resident);
                break;
            case "cancel":
                handleCancel(player, resident);
                break;
            case "run":
                handleRun(player, resident);
                break;
            case "withdraw":
                handleWithdraw(player, resident);
                break;
            case "vote":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /" + commandSource + " election vote <candidate_name>");
                    return true;
                }
                handleVote(player, resident, args[1]);
                break;
            case "info":
                showElectionInfo(player, resident);
                break;
            case "candidates":
                showCandidates(player, resident);
                break;
            case "results":
                showResults(player, resident);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommand);
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== " + commandSource.toUpperCase() + " Election Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election" + ChatColor.WHITE + " - View current election info.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election info" + ChatColor.WHITE + " - View current election info.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election start" + ChatColor.WHITE + " - Start a new election (Leader/Mayor only).");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election cancel" + ChatColor.WHITE + " - Cancel the ongoing election (Leader/Mayor only, before voting).");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election run" + ChatColor.WHITE + " - Run as a candidate during campaigning.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election withdraw" + ChatColor.WHITE + " - Withdraw your candidacy during campaigning.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election candidates" + ChatColor.WHITE + " - List current candidates.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election vote <name>" + ChatColor.WHITE + " - Vote for a candidate during voting period.");
        player.sendMessage(ChatColor.YELLOW + "/" + commandSource + " election results" + ChatColor.WHITE + " - View the results of the last concluded election.");
    }

    private void handleStart(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            if (!isTownMayor(resident, town, player)) return;

            if (electionManager.startElection(town)) {
                // Message sent by manager
            } else {
                player.sendMessage(ChatColor.RED + "Could not start election. An election might already be active.");
            }
        } else { // nation
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            if (!isNationLeader(resident, nation, player)) return;

            if (electionManager.startElection(nation)) {
                // Message sent by manager
            } else {
                player.sendMessage(ChatColor.RED + "Could not start election. An election might already be active.");
            }
        }
    }

    private void handleCancel(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            if (!isTownMayor(resident, town, player)) return;

            if (electionManager.cancelElection(town)) {
                // Message sent by manager
            } else {
                player.sendMessage(ChatColor.RED + "Could not cancel election. No election active or voting has already started.");
            }
        } else { // nation
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            if (!isNationLeader(resident, nation, player)) return;

            if (electionManager.cancelElection(nation)) {
                // Message sent by manager
            } else {
                player.sendMessage(ChatColor.RED + "Could not cancel election. No election active or voting has already started.");
            }
        }
    }

    private void handleRun(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            Election election = electionManager.getActiveTownElection(town.getUUID());
            if (election == null || !election.isCampaigningActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election campaign in your town right now.");
                return;
            }
            if (!town.hasResident(resident)) {
                player.sendMessage(ChatColor.RED + "You must be a resident of this town to run for mayor.");
                return;
            }

            if (electionManager.addCandidate(town, resident)) {
                player.sendMessage(ChatColor.GREEN + "You are now running for Mayor of " + town.getName() + "!");
                notifyTownMembers(town, ChatColor.AQUA + resident.getName() + " is now running for Mayor!");
            } else {
                player.sendMessage(ChatColor.RED + "Could not run for election. You might already be running, or the campaigning period is over.");
            }
        } else { // nation
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            Election election = electionManager.getActiveNationElection(nation.getUUID());
            if (election == null || !election.isCampaigningActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election campaign in your nation right now.");
                return;
            }
            if (!nation.hasResident(resident)) {
                player.sendMessage(ChatColor.RED + "You must be a member of this nation to run for leader.");
                return;
            }

            if (electionManager.addCandidate(nation, resident)) {
                player.sendMessage(ChatColor.GREEN + "You are now running for Leader of " + nation.getName() + "!");
                notifyNationMembers(nation, ChatColor.AQUA + resident.getName() + " is now running for Leader!");
            } else {
                player.sendMessage(ChatColor.RED + "Could not run for election. You might already be running, or the campaigning period is over.");
            }
        }
    }

    private void handleWithdraw(Player player, Resident resident) {
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            Election election = electionManager.getActiveTownElection(town.getUUID());
            if (election == null || !election.isCampaigningActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election campaign to withdraw from.");
                return;
            }

            if (electionManager.removeCandidate(town, resident)) {
                player.sendMessage(ChatColor.YELLOW + "You have withdrawn your candidacy for Mayor.");
                notifyTownMembers(town, ChatColor.YELLOW + resident.getName() + " has withdrawn their candidacy for Mayor.");
            } else {
                player.sendMessage(ChatColor.RED + "Could not withdraw. You might not be running, or the campaigning period is over.");
            }
        } else { // nation
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            Election election = electionManager.getActiveNationElection(nation.getUUID());
            if (election == null || !election.isCampaigningActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election campaign to withdraw from.");
                return;
            }

            if (electionManager.removeCandidate(nation, resident)) {
                player.sendMessage(ChatColor.YELLOW + "You have withdrawn your candidacy for Leader.");
                notifyNationMembers(nation, ChatColor.YELLOW + resident.getName() + " has withdrawn their candidacy for Leader.");
            } else {
                player.sendMessage(ChatColor.RED + "Could not withdraw. You might not be running, or the campaigning period is over.");
            }
        }
    }

    private void handleVote(Player player, Resident resident, String candidateName) {
        Resident targetCandidate = townyAPI.getResident(candidateName);
        if (targetCandidate == null) {
            player.sendMessage(ChatColor.RED + "Could not find a player named '" + candidateName + "'.");
            return;
        }

        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            Election election = electionManager.getActiveTownElection(town.getUUID());
            if (election == null || !election.isVotingActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election vote in your town right now.");
                return;
            }
            if (!town.hasResident(resident)) {
                player.sendMessage(ChatColor.RED + "You must be a resident of this town to vote.");
                return;
            }

            if (electionManager.castVote(town, resident, targetCandidate)) {
                player.sendMessage(ChatColor.GREEN + "You have successfully voted for " + targetCandidate.getName() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Could not cast vote. Ensure the player is a candidate and you haven't voted yet.");
            }
        } else { // nation
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            Election election = electionManager.getActiveNationElection(nation.getUUID());
            if (election == null || !election.isVotingActive()) {
                player.sendMessage(ChatColor.RED + "There is no active election vote in your nation right now.");
                return;
            }
            if (!nation.hasResident(resident)) {
                player.sendMessage(ChatColor.RED + "You must be a member of this nation to vote.");
                return;
            }

            if (electionManager.castVote(nation, resident, targetCandidate)) {
                player.sendMessage(ChatColor.GREEN + "You have successfully voted for " + targetCandidate.getName() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "Could not cast vote. Ensure the player is a candidate and you haven't voted yet.");
            }
        }
    }

    private void showElectionInfo(Player player, Resident resident) {
        Election election = null;
        String entityName = "";
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            election = electionManager.getActiveTownElection(town.getUUID());
            entityName = town.getName();
        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            election = electionManager.getActiveNationElection(nation.getUUID());
            entityName = nation.getName();
        }

        if (election == null) {
            player.sendMessage(ChatColor.YELLOW + "There is no active election in " + entityName + ".");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== " + entityName + " Election Status ===");
        player.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.WHITE + election.getStatusString());
        if (election.getStatus() == Election.ElectionStatus.CAMPAIGNING) {
            player.sendMessage(ChatColor.YELLOW + "Campaigning ends in: " + ChatColor.WHITE + election.getFormattedTimeRemaining(election.getCampaignEndTime()));
            showCandidates(player, resident, election);
        } else if (election.getStatus() == Election.ElectionStatus.VOTING) {
            player.sendMessage(ChatColor.YELLOW + "Voting ends in: " + ChatColor.WHITE + election.getFormattedTimeRemaining(election.getVotingEndTime()));
            showCandidates(player, resident, election);
        }
    }

    private void showCandidates(Player player, Resident resident) {
        Election election = null;
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            election = electionManager.getActiveTownElection(town.getUUID());
        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            election = electionManager.getActiveNationElection(nation.getUUID());
        }

        if (election == null || election.getStatus() == Election.ElectionStatus.FINISHED || election.getStatus() == Election.ElectionStatus.CANCELLED) {
            player.sendMessage(ChatColor.YELLOW + "There is no active election with candidates right now.");
            return;
        }
        showCandidates(player, resident, election);
    }

    private void showCandidates(Player player, Resident resident, Election election) {
        Map<UUID, String> candidates = election.getCandidates();
        if (candidates.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No candidates have run yet.");
            return;
        }
        player.sendMessage(ChatColor.GOLD + "Candidates:");
        for (String name : candidates.values()) {
            player.sendMessage(ChatColor.WHITE + "- " + name);
        }
    }

    private void showResults(Player player, Resident resident) {
        // This would ideally fetch the *last completed* election from storage,
        // even if one isn't currently active. For simplicity now, it shows current results if finished.
        Election election = null;
        String entityName = "";
        if (commandSource.equals("town")) {
            Town town = getTown(resident, player);
            if (town == null) return;
            election = electionManager.getActiveTownElection(town.getUUID()); // Need a way to get last completed election
            entityName = town.getName();
        } else {
            Nation nation = getNation(resident, player);
            if (nation == null) return;
            election = electionManager.getActiveNationElection(nation.getUUID()); // Need a way to get last completed election
            entityName = nation.getName();
        }

        if (election == null || election.getStatus() != Election.ElectionStatus.FINISHED) {
            // TODO: Enhance storage to fetch the *last* completed election, not just active ones.
            player.sendMessage(ChatColor.YELLOW + "No completed election results available for " + entityName + " right now.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== " + entityName + " Election Results ===");
        Map<String, Integer> voteCounts = election.getVoteCounts();
        if (voteCounts.isEmpty() && election.getCandidates().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "The election was cancelled (no candidates).");
        } else if (voteCounts.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No votes were cast.");
            // List candidates anyway
            if (!election.getCandidates().isEmpty()) {
                player.sendMessage(ChatColor.GOLD + "Candidates who ran:");
                election.getCandidates().values().forEach(name -> player.sendMessage(ChatColor.WHITE + "- " + name));
            }
        } else {
            // Sort results by votes descending
            List<Map.Entry<String, Integer>> sortedResults = voteCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());

            for (Map.Entry<String, Integer> entry : sortedResults) {
                player.sendMessage(ChatColor.WHITE + "- " + entry.getKey() + ": " + ChatColor.YELLOW + entry.getValue() + " votes");
            }
        }

        Resident winner = townyAPI.getResident(election.getWinnerId());
        if (winner != null) {
            player.sendMessage(ChatColor.GREEN + "Winner: " + ChatColor.BOLD + winner.getName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "No winner was determined.");
        }
    }

    private void notifyTownMembers(Town town, String message) {
        if (town == null) return;
        for (Resident res : town.getResidents()) {
            Player p = Bukkit.getPlayer(res.getUUID());
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    private void notifyNationMembers(Nation nation, String message) {
        if (nation == null) return;
        for (Resident res : nation.getResidents()) {
            Player p = Bukkit.getPlayer(res.getUUID());
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player)) return completions;

        Player player = (Player) sender;
        Resident resident = getResident(player);
        if (resident == null) return completions;

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("start", "cancel", "run", "withdraw", "vote", "info", "candidates", "results");
            return filterCompletions(subCommands, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
            // Suggest candidates from the current election
            Election election = null;
            if (commandSource.equals("town")) {
                Town town = getTown(resident, player);
                if (town != null) election = electionManager.getActiveTownElection(town.getUUID());
            } else {
                Nation nation = getNation(resident, player);
                if (nation != null) election = electionManager.getActiveNationElection(nation.getUUID());
            }

            if (election != null && (election.isVotingActive() || election.isCampaigningActive())) {
                return filterCompletions(new ArrayList<>(election.getCandidates().values()), args[1]);
            }
        }

        return completions;
    }
}