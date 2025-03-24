package com.orbismc.townyPolitics.hooks;

import com.palmergames.bukkit.towny.event.economy.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyPreTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.orbismc.townyPolitics.TownyPolitics;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class DiagnosticTransactionHandler implements Listener {
    private final TownyPolitics plugin;

    public DiagnosticTransactionHandler(TownyPolitics plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPreTownPaysNationTax(PreTownPaysNationTaxEvent event) {
        plugin.getLogger().info("DIAGNOSTIC: PreTownPaysNationTaxEvent fired - " +
                event.getTown().getName() + " -> " + event.getNation().getName() +
                " amount: " + event.getTax());
    }

    @EventHandler
    public void onTownyPreTransaction(TownyPreTransactionEvent event) {
        plugin.getLogger().info("DIAGNOSTIC: TownyPreTransactionEvent fired - " +
                "Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }

    @EventHandler
    public void onTownyTransaction(TownyTransactionEvent event) {
        plugin.getLogger().info("DIAGNOSTIC: TownyTransactionEvent fired - " +
                "Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }

    @EventHandler
    public void onNationTransaction(NationTransactionEvent event) {
        plugin.getLogger().info("DIAGNOSTIC: NationTransactionEvent fired - " +
                "Nation: " + event.getNation().getName() +
                ", Amount: " + event.getTransaction().getAmount() +
                ", Type: " + event.getTransaction().getType());
    }
}