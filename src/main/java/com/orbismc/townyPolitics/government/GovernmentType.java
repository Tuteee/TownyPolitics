package com.orbismc.townyPolitics.government;

import java.util.Arrays;

public enum GovernmentType {
    AUTOCRACY("Autocracy", "Autocracy Effects:\n• Decreased Corruption: -3% Corruption gain\n• Effective Control: +5% Tax income", false),
    OLIGARCHY("Oligarchy", "Oligarchy Effects:\n• Elite Connections: +8% Trade income\n• Increased Corruption Risk: +5% Corruption gain", false),
    REPUBLIC("Republic", "Republic Effects:\n• Economic Boom: +5% Trade Income, +2% Tax income\n• Increased Corruption Risk: +1% Corruption gain", false),
    DIRECT_DEMOCRACY("Direct Democracy", "Direct Democracy Effects:\n• Economic Prosperity: +2% Tax income\n• Transparent Ruling: -10% Corruption gain", false),
    CONSTITUTIONAL_MONARCHY("Constitutional Monarchy", "Constitutional Monarchy Effects:\n• For the Motherland: +15% Strength boost to all eligible soldiers\n• Efficient Head: -30% Political Power cost of policies", true);

    private final String displayName;
    private final String description;
    private final boolean nationOnly;

    GovernmentType(String displayName, String description, boolean nationOnly) {
        this.displayName = displayName;
        this.description = description;
        this.nationOnly = nationOnly;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNationOnly() {
        return nationOnly;
    }

    public boolean isDemocracy() {
        return this == CONSTITUTIONAL_MONARCHY || this == REPUBLIC || this == DIRECT_DEMOCRACY;
    }

    public static GovernmentType[] getTownGovernmentTypes() {
        return Arrays.stream(values())
                .filter(type -> !type.isNationOnly())
                .toArray(GovernmentType[]::new);
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