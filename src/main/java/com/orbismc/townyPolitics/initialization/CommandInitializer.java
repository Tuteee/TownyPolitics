package com.orbismc.townyPolitics.initialization;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.commands.*; // Import ElectionCommand
import com.orbismc.townyPolitics.utils.DelegateLogger;

public class CommandInitializer {
    private final TownyPolitics plugin;
    private final DelegateLogger logger;

    public CommandInitializer(TownyPolitics plugin) {
        this.plugin = plugin;
        this.logger = new DelegateLogger(plugin, "CommandInit");
    }

    public void initialize() {
        try {
            // Create command executors for nations
            GovernmentCommand nationGovCommand = new GovernmentCommand(plugin, plugin.getGovManager(), "nation");
            OverviewCommand nationOverviewCommand = new OverviewCommand(plugin, plugin.getGovManager(),
                    plugin.getPPManager(), plugin.getCorruptionManager());
            CorruptionCommand nationCorruptionCommand = new CorruptionCommand(plugin,
                    plugin.getCorruptionManager(), plugin.getPPManager());
            PoliticalPowerCommand ppCommand = new PoliticalPowerCommand(plugin, plugin.getPPManager());

            // Create command executors for towns
            GovernmentCommand townGovCommand = new GovernmentCommand(plugin, plugin.getGovManager(), "town");
            TownPoliticalPowerCommand townPpCommand = new TownPoliticalPowerCommand(plugin, plugin.getTownPPManager());

            // Create command executors for policies
            PolicyCommand townPolicyCommand = new PolicyCommand(plugin, plugin.getPolicyManager(), "town");
            PolicyCommand nationPolicyCommand = new PolicyCommand(plugin, plugin.getPolicyManager(), "nation");

            // Create budget commands
            BudgetCommand townBudgetCommand = new BudgetCommand(plugin, plugin.getBudgetManager(), "town");
            BudgetCommand nationBudgetCommand = new BudgetCommand(plugin, plugin.getBudgetManager(), "nation");

            // Create election commands - ADDED
            ElectionCommand townElectionCommand = new ElectionCommand(plugin, plugin.getElectionManager(), "town");
            ElectionCommand nationElectionCommand = new ElectionCommand(plugin, plugin.getElectionManager(), "nation");


            // Register nation commands
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "government", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "gov", nationGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "overview", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "o", nationOverviewCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "corruption", nationCorruptionCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "pp", ppCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "policy", nationPolicyCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "budget", nationBudgetCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.NATION, "election", nationElectionCommand); // ADDED

            // Register town commands
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "government", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "gov", townGovCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "policy", townPolicyCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "pp", townPpCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "budget", townBudgetCommand);
            TownyCommandAddonAPI.addSubCommand(CommandType.TOWN, "election", townElectionCommand); // ADDED

            // Register TownyAdmin command
            new TownyAdminPoliticsCommand(plugin, plugin.getGovManager(), plugin.getPPManager(),
                    plugin.getCorruptionManager());

            // Register test command for embezzlement
            TestEmbezzlementCommand testEmbezzlementCommand = new TestEmbezzlementCommand(plugin);
            plugin.getCommand("taxtest").setExecutor(testEmbezzlementCommand);

            // Register migration command
            // new MigrationCommand(plugin);

            logger.info("All commands registered");
        } catch (Exception e) {
            logger.severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }
}