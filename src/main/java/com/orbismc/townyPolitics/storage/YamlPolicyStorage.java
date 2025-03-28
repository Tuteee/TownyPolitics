package com.orbismc.townyPolitics.storage;

import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.policy.ActivePolicy;
import com.orbismc.townyPolitics.utils.DelegateLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YamlPolicyStorage implements IPolicyStorage {

    private final TownyPolitics plugin;
    private final DelegateLogger logger;
    private File townPoliciesFile;
    private File nationPoliciesFile;
    private FileConfiguration townPoliciesConfig;
    private FileConfiguration nationPoliciesConfig;

    public YamlPolicyStorage(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "YamlPolicyStorage");
        setupStorage();
    }

    /**
     * Set up storage files
     */
    private void setupStorage() {
        // Town policies file
        townPoliciesFile = new File(plugin.getDataFolder(), "town_policies.yml");
        if (!townPoliciesFile.exists()) {
            townPoliciesFile.getParentFile().mkdirs();
            try {
                townPoliciesFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Could not create town policies file: " + e.getMessage());
            }
        }
        townPoliciesConfig = YamlConfiguration.loadConfiguration(townPoliciesFile);

        // Nation policies file
        nationPoliciesFile = new File(plugin.getDataFolder(), "nation_policies.yml");
        if (!nationPoliciesFile.exists()) {
            nationPoliciesFile.getParentFile().mkdirs();
            try {
                nationPoliciesFile.createNewFile();
            } catch (IOException e) {
                logger.severe("Could not create nation policies file: " + e.getMessage());
            }
        }
        nationPoliciesConfig = YamlConfiguration.loadConfiguration(nationPoliciesFile);

        logger.info("YAML Policy Storage initialized");
    }

    @Override
    public void saveActivePolicy(ActivePolicy policy) {
        FileConfiguration config = policy.isNation() ? nationPoliciesConfig : townPoliciesConfig;
        String entityPath = policy.getEntityId().toString();
        String policyPath = entityPath + "." + policy.getId().toString();

        config.set(policyPath + ".policy_id", policy.getPolicyId());
        config.set(policyPath + ".enacted_time", policy.getEnactedTime());
        config.set(policyPath + ".expiry_time", policy.getExpiryTime());

        saveData(policy.isNation());
        logger.fine("Saved active policy: " + policy.getId());
    }

    @Override
    public void removeActivePolicy(UUID policyId) {
        // Check in town policies
        for (String entityId : townPoliciesConfig.getKeys(false)) {
            if (townPoliciesConfig.contains(entityId + "." + policyId.toString())) {
                townPoliciesConfig.set(entityId + "." + policyId.toString(), null);

                // If entity has no more policies, remove it
                if (townPoliciesConfig.getConfigurationSection(entityId).getKeys(false).isEmpty()) {
                    townPoliciesConfig.set(entityId, null);
                }

                saveData(false);
                logger.fine("Removed town policy: " + policyId);
                return;
            }
        }

        // Check in nation policies
        for (String entityId : nationPoliciesConfig.getKeys(false)) {
            if (nationPoliciesConfig.contains(entityId + "." + policyId.toString())) {
                nationPoliciesConfig.set(entityId + "." + policyId.toString(), null);

                // If entity has no more policies, remove it
                if (nationPoliciesConfig.getConfigurationSection(entityId).getKeys(false).isEmpty()) {
                    nationPoliciesConfig.set(entityId, null);
                }

                saveData(true);
                logger.fine("Removed nation policy: " + policyId);
                return;
            }
        }

        logger.warning("Attempted to remove non-existent policy: " + policyId);
    }

    @Override
    public Map<UUID, Set<ActivePolicy>> loadActivePolicies(boolean isNation) {
        Map<UUID, Set<ActivePolicy>> result = new ConcurrentHashMap<>();
        FileConfiguration config = isNation ? nationPoliciesConfig : townPoliciesConfig;

        for (String entityIdStr : config.getKeys(false)) {
            try {
                UUID entityId = UUID.fromString(entityIdStr);
                ConfigurationSection entitySection = config.getConfigurationSection(entityIdStr);
                if (entitySection == null) continue;

                Set<ActivePolicy> policies = new HashSet<>();

                for (String policyIdStr : entitySection.getKeys(false)) {
                    try {
                        UUID policyId = UUID.fromString(policyIdStr);
                        String policyTypeId = entitySection.getString(policyIdStr + ".policy_id");
                        long enactedTime = entitySection.getLong(policyIdStr + ".enacted_time");
                        long expiryTime = entitySection.getLong(policyIdStr + ".expiry_time");

                        ActivePolicy policy = new ActivePolicy(policyId, policyTypeId, entityId, isNation, enactedTime, expiryTime);

                        // Skip expired policies
                        if (policy.isExpired()) {
                            // Remove from config
                            entitySection.set(policyIdStr, null);
                            continue;
                        }

                        policies.add(policy);

                    } catch (IllegalArgumentException e) {
                        logger.warning("Invalid policy UUID in storage: " + policyIdStr);
                    }
                }

                if (!policies.isEmpty()) {
                    result.put(entityId, policies);
                } else if (entitySection.getKeys(false).isEmpty()) {
                    // Clean up empty sections
                    config.set(entityIdStr, null);
                }

            } catch (IllegalArgumentException e) {
                logger.warning("Invalid entity UUID in storage: " + entityIdStr);
            }
        }

        // Save any changes (like removed expired policies)
        saveData(isNation);

        logger.info("Loaded " + result.size() + " " + (isNation ? "nations" : "towns") +
                " with active policies from YAML");

        return result;
    }

    @Override
    public void saveAll() {
        saveData(false);
        saveData(true);
    }

    /**
     * Save data to file
     * @param isNation Whether to save nation (true) or town (false) data
     */
    private void saveData(boolean isNation) {
        try {
            if (isNation) {
                nationPoliciesConfig.save(nationPoliciesFile);
            } else {
                townPoliciesConfig.save(townPoliciesFile);
            }
        } catch (IOException e) {
            logger.severe("Could not save policy data: " + e.getMessage());
        }
    }
}