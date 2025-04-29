package com.orbismc.townyPolitics.api;

import com.orbismc.townyPolitics.TownyPolitics;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.policy.Policy;
import com.orbismc.townyPolitics.election.Election; // Assuming Election class exists
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * API class for interacting with TownyPolitics features.
 * Provides methods to access data about nations and towns related to politics,
 * corruption, policies, and elections.
 *
 * Get an instance using {@link #getInstance()}.
 * Ensure TownyPolitics is loaded and enabled before calling getInstance().
 *
 * @version 1.0.0
 * @author YourName (Update with your name/organization)
 */
public class TownyPoliticsAPI {

    private static TownyPoliticsAPI instance;
    private final TownyPolitics plugin;

    /**
     * Private constructor to enforce singleton pattern.
     * @param plugin The instance of the main TownyPolitics plugin.
     */
    private TownyPoliticsAPI(TownyPolitics plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the singleton instance of the TownyPoliticsAPI.
     *
     * It's recommended to check if the returned instance is null,
     * especially during server startup or if TownyPolitics might be disabled.
     *
     * @return The TownyPoliticsAPI instance, or null if TownyPolitics is not enabled.
     */
    public static synchronized TownyPoliticsAPI getInstance() {
        if (instance == null) {
            try {
                TownyPolitics mainPlugin = JavaPlugin.getPlugin(TownyPolitics.class);
                if (mainPlugin != null && mainPlugin.isEnabled()) {
                    instance = new TownyPoliticsAPI(mainPlugin);
                } else {
                    // Log a warning only if getInstance is called after server load potentially
                    if (Bukkit.getServer().getPluginManager().isPluginEnabled("TownyPolitics")) {
                        Bukkit.getLogger().warning("[YourOtherPlugin] TownyPoliticsAPI called but TownyPolitics might not be fully enabled yet!");
                    }
                    return null; // Return null if plugin isn't ready
                }
            } catch (IllegalStateException e) {
                // This can happen if called during server shutdown or before plugins are loaded
                Bukkit.getLogger().warning("[YourOtherPlugin] TownyPoliticsAPI.getInstance() called at an invalid time (server stopping or plugin disabled?).");
                return null;
            }
        }
        return instance;
    }

    // --- Nation Methods ---

    /**
     * Gets the current political power of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's political power, or 0.0 if the PP manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public double getNationPoliticalPower(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getPPManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PoliticalPowerManager is not available!");
            return 0.0;
        }
        return plugin.getPPManager().getPoliticalPower(nation);
    }

    /**
     * Gets the current government type of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's GovernmentType. Returns {@link GovernmentType#AUTOCRACY} if the government manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public GovernmentType getNationGovernmentType(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getGovManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: GovernmentManager is not available!");
            return GovernmentType.AUTOCRACY;
        }
        return plugin.getGovManager().getGovernmentType(nation);
    }

    /**
     * Gets the current corruption level of a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The nation's corruption level (0.0 - 100.0), or 0.0 if the corruption manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public double getNationCorruption(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getCorruptionManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: CorruptionManager is not available!");
            return 0.0;
        }
        return plugin.getCorruptionManager().getCorruption(nation);
    }

    /**
     * Gets the set of active policies for a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return An unmodifiable set of {@link ActivePolicy} objects, or an empty set if none are active or the policy manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public Set<ActivePolicy> getActiveNationPolicies(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(plugin.getPolicyManager().getActivePolicies(nation));
    }

    /**
     * Checks if an election is currently active for a nation.
     *
     * @param nation The nation to check. Cannot be null.
     * @return true if an election is active (Campaigning or Voting), false otherwise or if the election manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public boolean isNationElectionActive(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getElectionManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: ElectionManager is not available!");
            return false;
        }
        Election election = plugin.getElectionManager().getActiveNationElection(nation.getUUID());
        return election != null && (election.isCampaigningActive() || election.isVotingActive());
    }

    /**
     * Gets the currently active election for a nation, if one exists.
     *
     * @param nation The nation to check. Cannot be null.
     * @return The active {@link Election} object, or null if no election is active or the election manager isn't available.
     * @throws NullPointerException if nation is null.
     */
    public Election getActiveNationElection(Nation nation) {
        if (nation == null) throw new NullPointerException("Nation cannot be null");
        if (plugin.getElectionManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: ElectionManager is not available!");
            return null;
        }
        return plugin.getElectionManager().getActiveNationElection(nation.getUUID());
    }


    // --- Town Methods ---

    /**
     * Gets the current political power of a town.
     * Note: This depends on the town-level political power system being enabled in TownyPolitics config.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's political power, or 0.0 if the Town PP system isn't enabled or the manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public double getTownPoliticalPower(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownPPManager() == null) {
            // This isn't necessarily an error, the feature might be disabled. Log finer?
            // plugin.getLogger().fine("TownyPoliticsAPI: TownPoliticalPowerManager is not available (feature may be disabled).");
            return 0.0;
        }
        return plugin.getTownPPManager().getPoliticalPower(town);
    }

    /**
     * Gets the current government type of a town.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's GovernmentType. Returns {@link GovernmentType#AUTOCRACY} if the town government manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public GovernmentType getTownGovernmentType(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownGovManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: TownGovernmentManager is not available!");
            return GovernmentType.AUTOCRACY;
        }
        return plugin.getTownGovManager().getGovernmentType(town);
    }

    /**
     * Gets the current corruption level of a town.
     * Note: This depends on the town-level corruption system being enabled in TownyPolitics config.
     *
     * @param town The town to check. Cannot be null.
     * @return The town's corruption level (0.0 - 100.0), or 0.0 if the town corruption system isn't enabled or the manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public double getTownCorruption(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getTownCorruptionManager() == null) {
            // plugin.getLogger().fine("TownyPoliticsAPI: TownCorruptionManager is not available (feature may be disabled).");
            return 0.0;
        }
        return plugin.getTownCorruptionManager().getCorruption(town);
    }

    /**
     * Gets the set of active policies for a town.
     *
     * @param town The town to check. Cannot be null.
     * @return An unmodifiable set of {@link ActivePolicy} objects, or an empty set if none are active or the policy manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public Set<ActivePolicy> getActiveTownPolicies(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(plugin.getPolicyManager().getActivePolicies(town));
    }

    /**
     * Checks if an election is currently active for a town.
     *
     * @param town The town to check. Cannot be null.
     * @return true if an election is active (Campaigning or Voting), false otherwise or if the election manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public boolean isTownElectionActive(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getElectionManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: ElectionManager is not available!");
            return false;
        }
        Election election = plugin.getElectionManager().getActiveTownElection(town.getUUID());
        return election != null && (election.isCampaigningActive() || election.isVotingActive());
    }

    /**
     * Gets the currently active election for a town, if one exists.
     *
     * @param town The town to check. Cannot be null.
     * @return The active {@link Election} object, or null if no election is active or the election manager isn't available.
     * @throws NullPointerException if town is null.
     */
    public Election getActiveTownElection(Town town) {
        if (town == null) throw new NullPointerException("Town cannot be null");
        if (plugin.getElectionManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: ElectionManager is not available!");
            return null;
        }
        return plugin.getElectionManager().getActiveTownElection(town.getUUID());
    }

    // --- Utility/General Methods ---

    /**
     * Retrieves a specific policy definition by its ID.
     *
     * @param policyId The unique identifier of the policy (e.g., "progressive_taxation"). Cannot be null or empty.
     * @return The {@link Policy} object, or null if no policy with that ID exists or the policy manager isn't available.
     * @throws NullPointerException if policyId is null.
     * @throws IllegalArgumentException if policyId is empty.
     */
    public Policy getPolicyDefinition(String policyId) {
        if (policyId == null) throw new NullPointerException("Policy ID cannot be null");
        if (policyId.isEmpty()) throw new IllegalArgumentException("Policy ID cannot be empty");
        if (plugin.getPolicyManager() == null) {
            plugin.getLogger().warning("TownyPoliticsAPI: PolicyManager is not available!");
            return null;
        }
        return plugin.getPolicyManager().getPolicy(policyId);
    }

    /**
     * Gets the main TownyPolitics plugin instance.
     * Use this carefully, prefer using dedicated API methods when possible.
     *
     * @return The TownyPolitics plugin instance.
     */
    public TownyPolitics getPlugin() {
        return plugin;
    }
}