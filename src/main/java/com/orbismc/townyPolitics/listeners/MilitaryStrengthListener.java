package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Nation;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.MilitaryEffects;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MilitaryStrengthListener implements Listener {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;
    private final TownyAPI townyAPI;

    public MilitaryStrengthListener(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "MilitaryListener");
        this.townyAPI = TownyAPI.getInstance();

        logger.info("Military Strength Listener initialized");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if attacker is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        // Check if player has the soldier permission
        if (!player.hasPermission("townypolitics.soldier")) {
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
        }
    }
}