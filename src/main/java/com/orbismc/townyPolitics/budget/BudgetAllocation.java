package com.orbismc.townyPolitics.budget;

public class BudgetAllocation {
    private final double percentage;
    private final int priority;

    public BudgetAllocation(double percentage, int priority) {
        this.percentage = percentage;
        this.priority = priority;
    }

    public double getPercentage() {
        return percentage;
    }

    public int getPriority() {
        return priority;
    }
}