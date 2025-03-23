package com.orbismc.townyPolitics.government;

/**
 * Enum representing different government types available for towns and nations
 */
public enum GovernmentType {
    AUTOCRACY("Autocracy", "Autocracy Effects:\n• Decreased Corruption: -3% Corruption Gain\n• Economic Interventionism: +7% Increase of maximum taxation\n• One Head, One Power: +5% Political Power Gain"),
    OLIGARCHY("Oligarchy", "Oligarchy Effects:\n• Economic Interventionism II: +10% Increase of maximum taxation\n• Increased Corruption Risk: +5% Corruption Gain\n• Efficient Spending II: -10% All spending necessity"),
    CONSTITUTIONAL_MONARCHY("Constitutional Monarchy", "Constitutional Monarchy Effects:\n• Collective Security: -25% Political Power cost of alliances\n• For the Motherland: +15% (1 level strength potion boost) Strength boost to all eligible soldiers\n• Efficient Head: -30% Political Power cost of laws decree"),
    REPUBLIC("Republic", "Republic Effects:\n• Increased Corruption Risk: +1% Corruption Gain (not yet implemented)\n• Efficient Taxation: +5% Increase of maximum taxation (not yet implemented)\n• Efficient Bureaucracy: -25% Political Power cost of policies decree"),
    DIRECT_DEMOCRACY("Direct Democracy", "Direct Democracy Effects:\n• Collective Security II: -35% Political Power cost of alliances\n• Efficient Spending: -5% All spending necessity\n• Inefficient Bureaucracy: +15% Political Power cost for laws & policies");

    private final String displayName;
    private final String description;

    GovernmentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get the display name of the government type
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the description of the government type
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Check if the government type is democratic
     * @return True if the government is a democracy
     */
    public boolean isDemocracy() {
        return this == CONSTITUTIONAL_MONARCHY || this == REPUBLIC || this == DIRECT_DEMOCRACY;
    }

    /**
     * Get a government type by name (case-insensitive)
     * @param name The name of the government type
     * @return The government type or null if not found
     */
    public static GovernmentType getByName(String name) {
        for (GovernmentType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}