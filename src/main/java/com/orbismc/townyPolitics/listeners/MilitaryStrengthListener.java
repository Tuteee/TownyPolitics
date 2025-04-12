package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.MilitaryEffects;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Enhanced Military Strength Listener
 * Handles military-related effects for players and buildings
 */
public class MilitaryStrengthListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;
    private final TownyAPI townyAPI;
    private static final String SOLDIER_PERMISSION = "townypolitics.soldier";
    private static final String MILITARY_BUFF_META = "townypolitics_military_buff";
    private static final String BUILDING_DAMAGE_META = "townypolitics_building_damage";

    // Constants for military buffs
    private static final double MAX_STRENGTH_BONUS = 0.50; // 50% max damage bonus
    private static final double MAX_RESISTANCE_BONUS = 0.25; // 25% max damage reduction
    private static final int BUFF_DURATION_TICKS = 600; // 30 seconds (20 ticks per second)

    // Monarchy bonus for nations with CONSTITUTIONAL_MONARCHY
    private static final double MONARCHY_BONUS = 0.15; // 15% additional strength

    public MilitaryStrengthListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "MilitaryListener");
        this.townyAPI = TownyAPI.getInstance();

        logger.info("Enhanced Military Strength Listener initialized");
    }

    /**
     * Handle player combat damage modification based on military budget
     * This applies combat buffs to players with the soldier permission
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if attacker is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        // Check if player has the soldier permission
        if (!player.hasPermission(SOLDIER_PERMISSION)) {
            return;
        }

        // Get player's resident
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null || !resident.hasTown()) {
            return;
        }

        // Get player's town and nation
        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();

        if (town == null) {
            return;
        }

        // Get strength modifiers
        double townModifier = plugin.getEffectsManager().getTownMilitaryEffects(town).getStrengthModifier();
        double nationModifier = 1.0;

        if (nation != null) {
            nationModifier = plugin.getEffectsManager().getNationMilitaryEffects(nation).getStrengthModifier();

            // Apply constitutional monarchy bonus if applicable
            if (plugin.getGovManager().getGovernmentType(nation) == GovernmentType.CONSTITUTIONAL_MONARCHY) {
                nationModifier += MONARCHY_BONUS;
                logger.fine("Applied monarchy bonus to " + player.getName() + " - new nation modifier: " + nationModifier);
            }
        }

        // Apply the higher of the two modifiers
        double strengthModifier = Math.max(townModifier, nationModifier);

        // Apply budget-based combat effects
        if (strengthModifier != 1.0) {
            // Modify damage
            double newDamage = event.getDamage() * strengthModifier;
            event.setDamage(newDamage);

            // Apply visual effect for feedback
            applyMilitaryBuffEffects(player, strengthModifier);

            logger.fine("Modified player damage: " + player.getName() + " - Original: " +
                    event.getDamage() + ", Modified: " + newDamage + " (mod: " + strengthModifier + ")");

            // Store the strength modifier temporarily on the player for logging purposes
            player.setMetadata("strength_modifier", new FixedMetadataValue(plugin, strengthModifier));

            // Send feedback to player (only once every minute to avoid spam)
            if (!player.hasMetadata(MILITARY_BUFF_META) ||
                    (System.currentTimeMillis() - player.getMetadata(MILITARY_BUFF_META).get(0).asLong() > 60000)) {

                String modifierStr = String.format("%+.0f%%", (strengthModifier - 1.0) * 100);
                player.sendMessage(ChatColor.GOLD + "Military Training: " + ChatColor.GREEN + modifierStr +
                        ChatColor.GOLD + " combat effectiveness");

                player.setMetadata(MILITARY_BUFF_META,
                        new FixedMetadataValue(plugin, System.currentTimeMillis()));
            }
        }
    }

    /**
     * Apply visual effects to enhance the military buff feedback
     */
    private void applyMilitaryBuffEffects(Player player, double strengthModifier) {
        if (strengthModifier > 1.2) {
            // Strong buff - strength effect
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.STRENGTH,
                    BUFF_DURATION_TICKS,
                    0, // Level 1
                    false, // No particles
                    false, // No icon
                    true)); // Show particles
        } else if (strengthModifier > 1.0) {
            // Moderate buff - speed effect
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    BUFF_DURATION_TICKS,
                    0, // Level 1
                    false, // No particles
                    false, // No icon
                    true)); // Show particles
        } else if (strengthModifier < 0.8) {
            // Significant debuff - weakness effect
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.WEAKNESS,
                    BUFF_DURATION_TICKS,
                    0, // Level 1
                    false, // No particles
                    false, // No icon
                    true)); // Show particles
        }
    }

    /**
     * Handle damage to buildings based on military budget
     * This is a placeholder for future implementation of building protection
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBuildingDamage(EntityDamageEvent event) {
        // Only process block damage events (for blocks that can have entity damage events)
        if (event.getEntityType() != EntityType.ARMOR_STAND &&
                event.getEntityType() != EntityType.ITEM_FRAME &&
                event.getEntityType() != EntityType.PAINTING &&
                event.getEntityType() != EntityType.MINECART &&
                event.getEntityType() != EntityType.BOAT) {
            return;
        }

        // Implementation for building damage will be completed in future updates
        // Current placeholder just logs the event for debugging
        logger.fine("Building damage event detected: " + event.getEntityType() + " at " +
                event.getEntity().getLocation());
    }

    /**
     * Get the current soldier strength modifier for a player
     * Used by other systems that need to know the strength value
     */
    public double getPlayerStrengthModifier(Player player) {
        // Check if the player has a stored modifier value
        if (player.hasMetadata("strength_modifier")) {
            return player.getMetadata("strength_modifier").get(0).asDouble();
        }

        // No stored value, calculate it
        Resident resident = townyAPI.getResident(player.getUniqueId());
        if (resident == null || !resident.hasTown()) {
            return 1.0; // Default modifier
        }

        Town town = resident.getTownOrNull();
        Nation nation = resident.getNationOrNull();

        if (town == null) {
            return 1.0;
        }

        // Get strength modifiers
        double townModifier = plugin.getEffectsManager().getTownMilitaryEffects(town).getStrengthModifier();
        double nationModifier = nation != null ?
                plugin.getEffectsManager().getNationMilitaryEffects(nation).getStrengthModifier() : 1.0;

        // Apply constitutional monarchy bonus if applicable
        if (nation != null && plugin.getGovManager().getGovernmentType(nation) == GovernmentType.CONSTITUTIONAL_MONARCHY) {
            nationModifier += MONARCHY_BONUS;
        }

        // Apply the higher of the two modifiers
        return Math.max(townModifier, nationModifier);
    }

    /**
     * Get the current building damage modifier for a location
     * Used by other systems that need to know the building damage reduction
     */
    public double getBuildingDamageModifier(Town town) {
        double townModifier = plugin.getEffectsManager().getTownMilitaryEffects(town).getBuildingDamageModifier();
        double nationModifier = 1.0;

        // If town has a nation, check nation modifier
        if (town.hasNation()) {
            try {
                Nation nation = town.getNation();
                nationModifier = plugin.getEffectsManager().getNationMilitaryEffects(nation).getBuildingDamageModifier();
            } catch (Exception e) {
                logger.warning("Error getting nation for building damage modifier: " + e.getMessage());
            }
        }

        // Use the better protection (lower value)
        return Math.min(townModifier, nationModifier);
    }
}