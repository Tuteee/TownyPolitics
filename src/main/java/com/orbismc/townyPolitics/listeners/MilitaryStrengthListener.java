package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.MilitaryEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Handles military-related effects for players and buildings
 */
public class MilitaryStrengthListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;
    private final TownyAPI townyAPI;
    private static final String SOLDIER_PERMISSION = "townypolitics.soldier";
    private static final String BUILDING_DAMAGE_META = "townypolitics_building_damage";

    public MilitaryStrengthListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "MilitaryListener");
        this.townyAPI = TownyAPI.getInstance();

        logger.info("Military Strength Listener initialized");
    }

    /**
     * Handle player combat damage modification based on military budget
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
        double nationModifier = nation != null ?
                plugin.getEffectsManager().getNationMilitaryEffects(nation).getStrengthModifier() : 1.0;

        // Apply the higher of the two modifiers
        double strengthModifier = Math.max(townModifier, nationModifier);

        // Only apply if there's an actual modification
        if (strengthModifier != 1.0) {
            // Modify damage
            double newDamage = event.getDamage() * strengthModifier;
            event.setDamage(newDamage);

            logger.fine("Modified player damage: " + player.getName() + " - Original: " +
                    event.getDamage() + ", Modified: " + newDamage + " (mod: " + strengthModifier + ")");

            // Store the strength modifier temporarily on the player for logging purposes
            player.setMetadata("strength_modifier", new FixedMetadataValue(plugin, strengthModifier));
        }
    }

    /**
     * Handle damage to buildings based on military budget
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

        Entity damagedEntity = event.getEntity();

        // Check if this entity is in a town
        if (!townyAPI.isTownBlock(damagedEntity.getLocation())) {
            return;
        }

        // Get the town at this location
        Town town = townyAPI.getTown(damagedEntity.getLocation());
        if (town == null) {
            return;
        }

        // Get the nation if available
        Nation nation = null;
        try {
            if (town.hasNation()) {
                nation = town.getNation();
            }
        } catch (Exception e) {
            logger.warning("Error getting nation for town " + town.getName() + ": " + e.getMessage());
        }

        // Get building damage modifiers
        double townModifier = plugin.getEffectsManager().getTownMilitaryEffects(town).getBuildingDamageModifier();
        double nationModifier = nation != null ?
                plugin.getEffectsManager().getNationMilitaryEffects(nation).getBuildingDamageModifier() : 1.0;

        // Use the better protection (lower value)
        double damageModifier = Math.min(townModifier, nationModifier);

        // Apply the damage modifier
        if (damageModifier != 1.0) {
            double originalDamage = event.getDamage();
            double newDamage = originalDamage * damageModifier;

            event.setDamage(newDamage);

            // Store the modification for the damage logging system
            damagedEntity.setMetadata(BUILDING_DAMAGE_META,
                    new FixedMetadataValue(plugin, damageModifier));

            logger.fine("Modified building damage in town " + town.getName() +
                    " - Original: " + originalDamage + ", Modified: " + newDamage +
                    " (mod: " + damageModifier + ")");
        }
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

        // Apply the higher of the two modifiers
        return Math.max(townModifier, nationModifier);
    }
}