package com.orbismc.townyPolitics.listeners;

import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.orbismc.townypolitics.TownyPolitics;
import com.orbismc.townypolitics.managers.PoliticalPowerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownyEventListener implements Listener {

    private final TownyPolitics plugin;
    private final PoliticalPowerManager ppManager;

    public TownyEventListener(TownyPolitics plugin, PoliticalPowerManager ppManager) {
        this.plugin = plugin;
        this.ppManager = ppManager;
    }

    /**
     * Handle Towny's NewDayEvent to distribute political power
     *
     * @param event The NewDayEvent
     */
    @EventHandler
    public void onNewDay(NewDayEvent event) {
        plugin.getLogger().info("Processing new day for political power distribution...");
        ppManager.processNewDay();
        plugin.getLogger().info("Political power distribution complete.");
    }
}