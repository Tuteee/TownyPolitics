package com.orbismc.townyPolitics.managers;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.storage.GovernmentStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GovernmentManager {

    private final TownyPolitics plugin;
    private final GovernmentStorage storage;

    private Map<UUID, GovernmentType> townGovernments;
    private Map<UUID, GovernmentType> nationGovernments;

    // Maps to track the last time a government change occurred
    private Map<UUID, Long> townChangeTimes;
    private Map<UUID, Long> nationChangeTimes;

    public GovernmentManager(TownyPolitics plugin, GovernmentStorage storage) {
        this.plugin = plugin;
        this.storage = storage;

        this.townGovernments = new HashMap<>();
        this.nationGovernments = new HashMap<>();
        this.townChangeTimes = new HashMap<>();
        this.nationChangeTimes = new HashMap<>();

        // Load data from storage
        loadData();
    }

    public void loadData() {
        townGovernments.clear();
        nationGovernments.clear();
        townChangeTimes.clear();
        nationChangeTimes.clear();

        townGovernments.putAll(storage.loadAllGovernments(false));
        nationGovernments.putAll(storage.loadAllGovernments(true));
        townChangeTimes.putAll(storage.loadAllChangeTimes(false));
        nationChangeTimes.putAll(storage.loadAllChangeTimes(true));
    }

    public void reload() {
        loadData();
    }

    public GovernmentType getGovernmentType(Town town) {
        return townGovernments.getOrDefault(town.getUUID(), GovernmentType.AUTOCRACY);
    }

    public GovernmentType getGovernmentType(Nation nation) {
        return nationGovernments.getOrDefault(nation.getUUID(), GovernmentType.AUTOCRACY);
    }

    public long getLastChangeTime(Town town) {
        return townChangeTimes.getOrDefault(town.getUUID(), 0L);
    }

    public long getLastChangeTime(Nation nation) {
        return nationChangeTimes.getOrDefault(nation.getUUID(), 0L);
    }

    public boolean isOnCooldown(Town town) {
        long lastChange = getLastChangeTime(town);
        if (lastChange == 0) {
            return false; // Never changed before
        }

        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastChange) < cooldownMillis;
    }

    public boolean isOnCooldown(Nation nation) {
        long lastChange = getLastChangeTime(nation);
        if (lastChange == 0) {
            return false; // Never changed before
        }

        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastChange) < cooldownMillis;
    }

    public long getCooldownTimeRemaining(Town town) {
        if (!isOnCooldown(town)) {
            return 0;
        }

        long lastChange = getLastChangeTime(town);
        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        return cooldownMillis - (System.currentTimeMillis() - lastChange);
    }

    public long getCooldownTimeRemaining(Nation nation) {
        if (!isOnCooldown(nation)) {
            return 0;
        }

        long lastChange = getLastChangeTime(nation);
        long cooldownDays = plugin.getConfig().getLong("government.change_cooldown", 30);
        long cooldownMillis = cooldownDays * 24 * 60 * 60 * 1000;
        return cooldownMillis - (System.currentTimeMillis() - lastChange);
    }

    public String formatCooldownTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        hours = hours % 24;

        return days + " days, " + hours + " hours";
    }

    public boolean setGovernmentType(Town town, GovernmentType type, boolean bypassCooldown) {
        // Check cooldown if not bypassing
        if (!bypassCooldown && isOnCooldown(town)) {
            return false;
        }

        // Update government type
        townGovernments.put(town.getUUID(), type);
        storage.saveGovernment(town.getUUID(), type, false);

        // Update change time
        long now = System.currentTimeMillis();
        townChangeTimes.put(town.getUUID(), now);
        storage.saveChangeTime(town.getUUID(), now, false);

        return true;
    }

    public boolean setGovernmentType(Town town, GovernmentType type) {
        return setGovernmentType(town, type, false);
    }

    public boolean setGovernmentType(Nation nation, GovernmentType type, boolean bypassCooldown) {
        // Check cooldown if not bypassing
        if (!bypassCooldown && isOnCooldown(nation)) {
            return false;
        }

        // Update government type
        nationGovernments.put(nation.getUUID(), type);
        storage.saveGovernment(nation.getUUID(), type, true);

        // Update change time
        long now = System.currentTimeMillis();
        nationChangeTimes.put(nation.getUUID(), now);
        storage.saveChangeTime(nation.getUUID(), now, true);

        return true;
    }

    public boolean setGovernmentType(Nation nation, GovernmentType type) {
        return setGovernmentType(nation, type, false);
    }

    public boolean hasDemocraticGovernment(Town town) {
        GovernmentType type = getGovernmentType(town);
        return type.isDemocracy();
    }

    public boolean hasDemocraticGovernment(Nation nation) {
        GovernmentType type = getGovernmentType(nation);
        return type.isDemocracy();
    }
}