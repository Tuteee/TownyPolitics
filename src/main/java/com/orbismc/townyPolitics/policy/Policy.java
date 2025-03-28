package com.orbismc.townyPolitics.policy;

import com.orbismc.townyPolitics.government.GovernmentType;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a policy that can be enacted by a town or nation
 */
public class Policy {
    private final String id;
    private final String name;
    private final String description;
    private final double cost;
    private final int duration; // -1 for permanent
    private final PolicyType type;
    private final Set<GovernmentType> allowedGovernments;
    private final double minPoliticalPower;
    private final double maxCorruption;
    private final PolicyEffects effects;

    public Policy(String id, String name, String description, double cost, int duration,
                  PolicyType type, Set<GovernmentType> allowedGovernments,
                  double minPoliticalPower, double maxCorruption, PolicyEffects effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.duration = duration;
        this.type = type;
        this.allowedGovernments = allowedGovernments != null ? allowedGovernments : new HashSet<>();
        this.minPoliticalPower = minPoliticalPower;
        this.maxCorruption = maxCorruption;
        this.effects = effects;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getCost() { return cost; }
    public int getDuration() { return duration; }
    public PolicyType getType() { return type; }
    public Set<GovernmentType> getAllowedGovernments() { return allowedGovernments; }
    public double getMinPoliticalPower() { return minPoliticalPower; }
    public double getMaxCorruption() { return maxCorruption; }
    public PolicyEffects getEffects() { return effects; }

    /**
     * Checks if a government type is allowed for this policy
     * @param type The government type to check
     * @return true if allowed or if no restrictions are set
     */
    public boolean isGovernmentAllowed(GovernmentType type) {
        return allowedGovernments.isEmpty() || allowedGovernments.contains(type);
    }

    /**
     * Policy type enum to categorize policies
     */
    public enum PolicyType {
        ECONOMIC,
        POLITICAL,
        MILITARY,
        SOCIAL
    }
}