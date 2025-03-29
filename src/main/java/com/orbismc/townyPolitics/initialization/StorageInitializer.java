package com.orbismc.townyPolitics.initialization;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.DatabaseManager;
import com.orbismc.townyPolitics.storage.*;
import com.orbismc.townyPolitics.storage.mysql.*;
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class StorageInitializer {
    private final TownyPolitics plugin;
    private final DatabaseManager dbManager;
    private final DelegateLogger logger;

    // Storage interfaces
    private IPoliticalPowerStorage ppStorage;
    private IGovernmentStorage govStorage;
    private ICorruptionStorage corruptionStorage;
    private ITownGovernmentStorage townGovStorage;
    private ITownCorruptionStorage townCorruptionStorage;
    private ITownPoliticalPowerStorage townPpStorage;
    private IPolicyStorage policyStorage;

    public StorageInitializer(TownyPolitics plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.logger = new DelegateLogger(plugin, "StorageInit");
    }

    public void initialize() {
        // Initialize MySQL storage implementations
        ppStorage = new MySQLPoliticalPowerStorage(plugin, dbManager);
        govStorage = new MySQLGovernmentStorage(plugin, dbManager);
        corruptionStorage = new MySQLCorruptionStorage(plugin, dbManager);
        townGovStorage = new MySQLTownGovernmentStorage(plugin, dbManager);
        townCorruptionStorage = new MySQLTownCorruptionStorage(plugin, dbManager);
        townPpStorage = new MySQLTownPoliticalPowerStorage(plugin, dbManager);
        policyStorage = new MySQLPolicyStorage(plugin, dbManager);

        logger.info("MySQL storage initialized");
    }

    // Getters for the storage interfaces
    public IPoliticalPowerStorage getPpStorage() { return ppStorage; }
    public IGovernmentStorage getGovStorage() { return govStorage; }
    public ICorruptionStorage getCorruptionStorage() { return corruptionStorage; }
    public ITownGovernmentStorage getTownGovStorage() { return townGovStorage; }
    public ITownCorruptionStorage getTownCorruptionStorage() { return townCorruptionStorage; }
    public ITownPoliticalPowerStorage getTownPpStorage() { return townPpStorage; }
    public IPolicyStorage getPolicyStorage() { return policyStorage; }
}