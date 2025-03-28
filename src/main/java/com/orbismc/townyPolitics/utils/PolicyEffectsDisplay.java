package com.orbismc.townyPolitics.utils;

import com.orbismc.townyPolitics.policy.PolicyEffects;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Utility class for displaying policy effects to players
 */
public class PolicyEffectsDisplay {

    /**
     * Display policy effects to a player
     * @param player The player to display effects to
     * @param effects The policy effects to display
     */
    public static void displayEffects(Player player, PolicyEffects effects) {
        displayModifier(player, "Tax Rate", effects.getTaxModifier());
        displayModifier(player, "Trade Income", effects.getTradeModifier());
        displayModifier(player, "Economy", effects.getEconomyModifier());
        displayModifier(player, "Political Power Gain", effects.getPoliticalPowerGainModifier());
        displayModifier(player, "Corruption Gain", effects.getCorruptionGainModifier(), true);
        displayModifier(player, "Max Political Power", effects.getMaxPoliticalPowerModifier());
        displayModifier(player, "Resource Output", effects.getResourceOutputModifier());
        displayModifier(player, "Spending Costs", effects.getSpendingModifier(), true);
    }

    /**
     * Display a modifier with appropriate formatting
     * @param player The player to display to
     * @param label The modifier label
     * @param value The modifier value
     */
    private static void displayModifier(Player player, String label, double value) {
        displayModifier(player, label, value, false);
    }

    /**
     * Display a modifier with appropriate formatting
     * @param player The player to display to
     * @param label The modifier label
     * @param value The modifier value
     * @param invert Whether higher values are negative (e.g., corruption)
     */
    private static void displayModifier(Player player, String label, double value, boolean invert) {
        if (value == 1.0) {
            // No effect
            return;
        }

        // Format the percentage
        String formattedValue = formatModifier(value);

        // Determine color based on value and invert flag
        ChatColor color;
        if (invert) {
            color = (value > 1.0) ? ChatColor.RED : ChatColor.GREEN;
        } else {
            color = (value > 1.0) ? ChatColor.GREEN : ChatColor.RED;
        }

        player.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + label + ": " +
                color + formattedValue);
    }

    /**
     * Format a modifier as a percentage
     * @param value The modifier value
     * @return The formatted string
     */
    public static String formatModifier(double value) {
        return String.format("%+.1f%%", (value - 1.0) * 100);
    }
}