package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.budget.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectsManager {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    // Maps to store effects
    private final Map<UUID, MilitaryEffects> townMilitaryEffects;
    private final Map<UUID, InfrastructureEffects> townInfrastructureEffects;
    private final Map<UUID, AdministrationEffects> townAdminEffects;
    private final Map<UUID, EducationEffects> townEducationEffects;

    private final Map<UUID, MilitaryEffects> nationMilitaryEffects;
    private final Map<UUID, InfrastructureEffects> nationInfrastructureEffects;
    private final Map<UUID, AdministrationEffects> nationAdminEffects;
    private final Map<UUID, EducationEffects> nationEducationEffects;

    public EffectsManager(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "EffectsManager");

        this.townMilitaryEffects = new ConcurrentHashMap<>();
        this.townInfrastructureEffects = new ConcurrentHashMap<>();
        this.townAdminEffects = new ConcurrentHashMap<>();
        this.townEducationEffects = new ConcurrentHashMap<>();

        this.nationMilitaryEffects = new ConcurrentHashMap<>();
        this.nationInfrastructureEffects = new ConcurrentHashMap<>();
        this.nationAdminEffects = new ConcurrentHashMap<>();
        this.nationEducationEffects = new ConcurrentHashMap<>();

        logger.info("Effects Manager initialized");
    }

    // Methods to register effects

    public void registerTownMilitaryEffects(UUID townId, MilitaryEffects effects) {
        townMilitaryEffects.put(townId, effects);
    }

    public void registerTownInfrastructureEffects(UUID townId, InfrastructureEffects effects) {
        townInfrastructureEffects.put(townId, effects);
    }

    public void registerTownAdminEffects(UUID townId, AdministrationEffects effects) {
        townAdminEffects.put(townId, effects);
    }

    public void registerTownEducationEffects(UUID townId, EducationEffects effects) {
        townEducationEffects.put(townId, effects);
    }

    public void registerNationMilitaryEffects(UUID nationId, MilitaryEffects effects) {
        nationMilitaryEffects.put(nationId, effects);
    }

    public void registerNationInfrastructureEffects(UUID nationId, InfrastructureEffects effects) {
        nationInfrastructureEffects.put(nationId, effects);
    }

    public void registerNationAdminEffects(UUID nationId, AdministrationEffects effects) {
        nationAdminEffects.put(nationId, effects);
    }

    public void registerNationEducationEffects(UUID nationId, EducationEffects effects) {
        nationEducationEffects.put(nationId, effects);
    }

    // Methods to get effects

    public MilitaryEffects getTownMilitaryEffects(Town town) {
        return townMilitaryEffects.getOrDefault(town.getUUID(),
                new MilitaryEffects(1.0, 1.0)); // Default values
    }

    public InfrastructureEffects getTownInfrastructureEffects(Town town) {
        return townInfrastructureEffects.getOrDefault(town.getUUID(),
                new InfrastructureEffects(1.0, 1.0)); // Default values
    }

    public AdministrationEffects getTownAdminEffects(Town town) {
        return townAdminEffects.getOrDefault(town.getUUID(),
                new AdministrationEffects(1.0, 1.0)); // Default values
    }

    public EducationEffects getTownEducationEffects(Town town) {
        return townEducationEffects.getOrDefault(town.getUUID(),
                new EducationEffects(1.0, 1.0)); // Default values
    }

    public MilitaryEffects getNationMilitaryEffects(Nation nation) {
        return nationMilitaryEffects.getOrDefault(nation.getUUID(),
                new MilitaryEffects(1.0, 1.0)); // Default values
    }

    public InfrastructureEffects getNationInfrastructureEffects(Nation nation) {
        return nationInfrastructureEffects.getOrDefault(nation.getUUID(),
                new InfrastructureEffects(1.0, 1.0)); // Default values
    }

    public AdministrationEffects getNationAdminEffects(Nation nation) {
        return nationAdminEffects.getOrDefault(nation.getUUID(),
                new AdministrationEffects(1.0, 1.0)); // Default values
    }

    public EducationEffects getNationEducationEffects(Nation nation) {
        return nationEducationEffects.getOrDefault(nation.getUUID(),
                new EducationEffects(1.0, 1.0)); // Default values
    }

    // Methods to clear effects

    public void clearTownEffects(UUID townId) {
        townMilitaryEffects.remove(townId);
        townInfrastructureEffects.remove(townId);
        townAdminEffects.remove(townId);
        townEducationEffects.remove(townId);
    }

    public void clearNationEffects(UUID nationId) {
        nationMilitaryEffects.remove(nationId);
        nationInfrastructureEffects.remove(nationId);
        nationAdminEffects.remove(nationId);
        nationEducationEffects.remove(nationId);
    }
}