package com.orbismc.townyPolitics.government;

public enum GovernmentType {
    AUTOCRACY("Autocracy", "Autocracy Effects:\n• Decreased Corruption: -3% Corruption Gain\n• Streamlined Decision-Making: -15% time for policy implementation\n• One Head, One Power: +5% Political Power Gain"),
    OLIGARCHY("Oligarchy", "Oligarchy Effects:\n• Elite Connections: +8% trade income with allied nations\n• Increased Corruption Risk: +5% Corruption Gain\n• Efficient Spending II: -10% All spending necessity"),
    CONSTITUTIONAL_MONARCHY("Constitutional Monarchy", "Constitutional Monarchy Effects:\n• Collective Security: -25% Political Power cost of alliances\n• For the Motherland: +15% Strength boost to all eligible soldiers\n• Efficient Head: -30% Political Power cost of laws decree"),
    REPUBLIC("Republic", "Republic Effects:\n• Economic Prosperity: +7% town income generation\n• Increased Corruption Risk: +1% Corruption Gain\n• Efficient Bureaucracy: -25% Political Power cost of policies decree"),
    DIRECT_DEMOCRACY("Direct Democracy", "Direct Democracy Effects:\n• Collective Security II: -35% Political Power cost of alliances\n• Efficient Spending: -5% All spending necessity\n• Inefficient Bureaucracy: +15% Political Power cost for laws & policies");

    private final String displayName;
    private final String description;

    GovernmentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDemocracy() {
        return this == CONSTITUTIONAL_MONARCHY || this == REPUBLIC || this == DIRECT_DEMOCRACY;
    }

    public static GovernmentType getByName(String name) {
        for (GovernmentType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}