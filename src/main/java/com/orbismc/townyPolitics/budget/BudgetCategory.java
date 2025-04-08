package com.orbismc.townyPolitics.budget;

public enum BudgetCategory {
    MILITARY("military"),
    INFRASTRUCTURE("infrastructure"),
    ADMINISTRATION("administration"),
    EDUCATION("education");

    private final String configKey;

    BudgetCategory(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static BudgetCategory fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MILITARY; // Default
        }
    }
}