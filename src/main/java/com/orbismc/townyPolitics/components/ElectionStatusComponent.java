package com.orbismc.townyPolitics.components; // Adjust package as needed

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.election.Election;
import com.orbismc.townyPolitics.election.ElectionManager;
import com.orbismc.townyPolitics.utils.EventHelper; // Added import
import com.palmergames.adventure.text.Component;
import com.palmergames.adventure.text.TextComponent; // Using TextComponent.Builder
import com.palmergames.adventure.text.event.HoverEvent;
import com.palmergames.adventure.text.format.NamedTextColor;
import com.palmergames.adventure.text.format.TextDecoration; // Added import
import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident; // Added import (used internally by buildHoverText)
import com.palmergames.bukkit.towny.object.Town;

import java.util.Map;
import java.util.UUID; // Added import

public class ElectionStatusComponent extends StatusComponent { // Assuming StatusComponent exists

    private final ElectionManager electionManager;

    public ElectionStatusComponent(TownyPolitics plugin, ElectionManager electionManager) {
        super(plugin, "Election");
        this.electionManager = electionManager;
    }

    @Override
    public void addToNationScreen(NationStatusScreenEvent event, Nation nation) {
        Election election = electionManager.getActiveNationElection(nation.getUUID());
        if (election != null) {
            Component hoverText = buildHoverText(election, nation.getName());
            // Call EventHelper statically
            Component electionComponent = EventHelper.createHoverComponent("Election", hoverText, NamedTextColor.AQUA);
            addComponentToScreen(event, "election_display", electionComponent);
        }
    }

    @Override
    public void addToTownScreen(TownStatusScreenEvent event, Town town) {
        Election election = electionManager.getActiveTownElection(town.getUUID());
        if (election != null) {
            Component hoverText = buildHoverText(election, town.getName());
            // Call EventHelper statically
            Component electionComponent = EventHelper.createHoverComponent("Election", hoverText, NamedTextColor.AQUA);
            addComponentToScreen(event, "election_display", electionComponent);
        }
    }

    private Component buildHoverText(Election election, String entityName) {
        // Use a TextComponent.Builder to avoid lambda reassignment issues
        TextComponent.Builder hoverBuilder = Component.text()
                .content("Election Status: " + entityName).color(NamedTextColor.DARK_AQUA)
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Status: ").color(NamedTextColor.YELLOW))
                .append(Component.text(election.getStatus().name()).color(NamedTextColor.WHITE));

        if (election.getStatus() == Election.ElectionStatus.CAMPAIGNING) {
            hoverBuilder.append(Component.newline())
                    .append(Component.text("Ends in: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(election.getFormattedTimeRemaining(election.getCampaignEndTime())).color(NamedTextColor.WHITE));
        } else if (election.getStatus() == Election.ElectionStatus.VOTING) {
            hoverBuilder.append(Component.newline())
                    .append(Component.text("Ends in: ").color(NamedTextColor.YELLOW))
                    .append(Component.text(election.getFormattedTimeRemaining(election.getVotingEndTime())).color(NamedTextColor.WHITE));
        }

        Map<UUID, String> candidates = election.getCandidates();
        if (!candidates.isEmpty()) {
            hoverBuilder.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Candidates:").color(NamedTextColor.GOLD));
            for (String name : candidates.values()) {
                hoverBuilder.append(Component.newline())
                        .append(Component.text("- " + name).color(NamedTextColor.WHITE));
            }
        } else if (election.getStatus() == Election.ElectionStatus.CAMPAIGNING) {
            hoverBuilder.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("No candidates yet.").color(NamedTextColor.GRAY));
        }

        // Show results if finished
        if (election.getStatus() == Election.ElectionStatus.FINISHED) {
            hoverBuilder.append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Results:").color(NamedTextColor.GOLD));
            Map<String, Integer> voteCounts = election.getVoteCounts();
            if (voteCounts.isEmpty()) {
                hoverBuilder.append(Component.newline()).append(Component.text("No votes were cast.").color(NamedTextColor.GRAY));
            } else {
                // Build the results section within the loop using the builder
                voteCounts.forEach((name, count) ->
                        hoverBuilder.append(Component.newline())
                                .append(Component.text("- " + name + ": ").color(NamedTextColor.WHITE))
                                .append(Component.text(count + " votes").color(NamedTextColor.YELLOW))
                );
            }
            if (election.getWinnerId() != null) {
                // Get winner's name (safer with null check)
                String winnerName = "Unknown";
                Resident winnerResident = plugin.getTownyAPI().getResident(election.getWinnerId()); // Use TownyAPI to get resident
                if (winnerResident != null) {
                    winnerName = winnerResident.getName();
                } else {
                    // Fallback to stored name if resident object not found
                    winnerName = election.getCandidates().getOrDefault(election.getWinnerId(), "Unknown");
                }

                hoverBuilder.append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Winner: ").color(NamedTextColor.GREEN))
                        // Apply BOLD decoration
                        .append(Component.text(winnerName).decorate(TextDecoration.BOLD));
            }
        }

        return hoverBuilder.build(); // Build the final component
    }
}