package com.orbismc.townyPolitics.commands;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;
import com.orbismc.townyPolitics.TownyPolitics;
import com.orbismc.townyPolitics.government.GovernmentType;
import com.orbismc.townyPolitics.managers.CorruptionManager;
import com.orbismc.townyPolitics.managers.GovernmentManager;
import com.orbismc.townyPolitics.managers.PoliticalPowerManager;
import com.orbismc.townyPolitics.managers.TownCorruptionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TownyAdminPoliticsCommand extends BaseCommand implements TabExecutor {

    private static final List<String> townyPoliticsAdminTabCompletes = Arrays.asList(
            "setgovernment", "addpp", "setpp", "addcorruption", "setcorruption",
            "addtowncorruption", "settowncorruption", "reload");

    private final TownyPolitics plugin;
    private final GovernmentManager govManager;
    private final PoliticalPowerManager ppManager;
    private final CorruptionManager corruptionManager;
    private final TownCorruptionManager townCorruptionManager;
    private final TownyAPI townyAPI;

    public TownyAdminPoliticsCommand(TownyPolitics plugin, GovernmentManager govManager,
                                     PoliticalPowerManager ppManager, CorruptionManager corruptionManager) {
        this.plugin = plugin;
        this.govManager = govManager;
        this.ppManager = ppManager;
        this.corruptionManager = corruptionManager;
        this.townCorruptionManager = plugin.getTownCorruptionManager();
        this.townyAPI = TownyAPI.getInstance();

        // Register the command with Towny
        AddonCommand townyAdminPoliticsCommand = new AddonCommand(TownyCommandAddonAPI.CommandType.TOWNYADMIN, "politics", this);
        TownyCommandAddonAPI.addSubCommand(townyAdminPoliticsCommand);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return NameUtil.filterByStart(townyPoliticsAdminTabCompletes, args[0]);
            case 2:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    return NameUtil.filterByStart(Arrays.asList("town", "nation"), args[1]);
                } else if (args[0].equalsIgnoreCase("addpp") || args[0].equalsIgnoreCase("setpp") ||
                        args[0].equalsIgnoreCase("addcorruption") || args[0].equalsIgnoreCase("setcorruption")) {
                    return getTownyStartingWith(args[1], "n");
                } else if (args[0].equalsIgnoreCase("addtowncorruption") || args[0].equalsIgnoreCase("settowncorruption") ||
                        args[0].equalsIgnoreCase("addtowncorrupt") || args[0].equalsIgnoreCase("settowncorrupt")) {
                    return getTownyStartingWith(args[1], "t");
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    if (args[1].equalsIgnoreCase("town")) {
                        return getTownyStartingWith(args[2], "t");
                    } else if (args[1].equalsIgnoreCase("nation")) {
                        return getTownyStartingWith(args[2], "n");
                    }
                }
                break;
            case 4:
                if (args[0].equalsIgnoreCase("setgovernment")) {
                    return Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.toList());
                }
                break;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "reload" -> parseReloadCommand(sender);
                case "setgovernment", "setgov" -> parseSetGovernmentCommand(sender, StringMgmt.remFirstArg(args));
                case "addpp" -> parseAddPPCommand(sender, StringMgmt.remFirstArg(args));
                case "setpp" -> parseSetPPCommand(sender, StringMgmt.remFirstArg(args));
                case "addcorruption", "addcorrupt" -> parseAddCorruptionCommand(sender, StringMgmt.remFirstArg(args));
                case "setcorruption", "setcorrupt" -> parseSetCorruptionCommand(sender, StringMgmt.remFirstArg(args));
                case "addtowncorruption", "addtowncorrupt" -> parseAddTownCorruptionCommand(sender, StringMgmt.remFirstArg(args));
                case "settowncorruption", "settowncorrupt" -> parseSetTownCorruptionCommand(sender, StringMgmt.remFirstArg(args));
                default -> showHelp(sender);
            }
        } catch (TownyException e) {
            TownyMessaging.sendErrorMsg(sender, e.getMessage());
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/townyadmin politics"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "reload", "Reload the plugin configuration"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setgovernment town [town] [type]", "Force set a town's government"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setgovernment nation [nation] [type]", "Force set a nation's government"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "addpp [nation] [amount]", "Add political power to a nation"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setpp [nation] [amount]", "Set a nation's political power"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "addcorruption [nation] [amount]", "Add corruption to a nation"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "setcorruption [nation] [amount]", "Set a nation's corruption level"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "addtowncorruption [town] [amount]", "Add corruption to a town"));
        TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/ta politics", "settowncorruption [town] [amount]", "Set a town's corruption level"));
    }

    private void parseReloadCommand(CommandSender sender) {
        plugin.reload();
        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "TownyPolitics configuration reloaded!");
    }

    private void parseSetGovernmentCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 3) {
            throw new TownyException("Not enough arguments. Use: /ta politics setgovernment [town/nation] [name] [type]");
        }

        String targetType = args[0].toLowerCase();
        String targetName = args[1];
        String govTypeName = args[2].toUpperCase();

        // Try to get the government type
        GovernmentType govType = GovernmentType.getByName(govTypeName);
        if (govType == null) {
            throw new TownyException("Invalid government type: " + govTypeName + ". Available types: " +
                    Arrays.stream(GovernmentType.values())
                            .map(GovernmentType::name)
                            .collect(Collectors.joining(", ")));
        }

        // Set government based on target type
        if (targetType.equals("town")) {
            Town town = TownyUniverse.getInstance().getTown(targetName);
            if (town == null) {
                throw new TownyException("Town not found: " + targetName);
            }

            // Force set government type and bypass cooldown
            govManager.setGovernmentType(town, govType, true);
            TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Successfully set " + town.getName() + "'s government to " + govType.getDisplayName() + ".");
        } else if (targetType.equals("nation")) {
            Nation nation = TownyUniverse.getInstance().getNation(targetName);
            if (nation == null) {
                throw new TownyException("Nation not found: " + targetName);
            }

            // Force set government type and bypass cooldown
            govManager.setGovernmentType(nation, govType, true);
            TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Successfully set " + nation.getName() + "'s government to " + govType.getDisplayName() + ".");
        } else {
            throw new TownyException("Invalid target type. Use 'town' or 'nation'.");
        }
    }

    private void parseAddPPCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics addpp [nation] [amount]");
        }

        String nationName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Nation nation = TownyUniverse.getInstance().getNation(nationName);
        if (nation == null) {
            throw new TownyException("Nation not found: " + nationName);
        }

        // Add political power to the nation
        ppManager.addPoliticalPower(nation, amount);
        double currentPP = ppManager.getPoliticalPower(nation);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Added " + String.format("%.2f", amount) +
                " political power to " + nation.getName() + ". Current PP: " +
                String.format("%.2f", currentPP));
    }

    private void parseSetPPCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics setpp [nation] [amount]");
        }

        String nationName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Nation nation = TownyUniverse.getInstance().getNation(nationName);
        if (nation == null) {
            throw new TownyException("Nation not found: " + nationName);
        }

        // Set political power for the nation
        ppManager.setPoliticalPower(nation, amount);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Set " + nation.getName() + "'s political power to " +
                String.format("%.2f", amount));
    }

    private void parseAddCorruptionCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics addcorruption [nation] [amount]");
        }

        String nationName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Nation nation = TownyUniverse.getInstance().getNation(nationName);
        if (nation == null) {
            throw new TownyException("Nation not found: " + nationName);
        }

        // Add corruption to the nation
        corruptionManager.addCorruption(nation, amount);
        double currentCorrupt = corruptionManager.getCorruption(nation);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Added " + String.format("%.2f", amount) +
                " corruption to " + nation.getName() + ". Current corruption: " +
                String.format("%.2f", currentCorrupt) + "%");
    }

    private void parseSetCorruptionCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics setcorruption [nation] [amount]");
        }

        String nationName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Nation nation = TownyUniverse.getInstance().getNation(nationName);
        if (nation == null) {
            throw new TownyException("Nation not found: " + nationName);
        }

        // Set corruption for the nation
        corruptionManager.setCorruption(nation, amount);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Set " + nation.getName() + "'s corruption level to " +
                String.format("%.2f", amount) + "%");
    }

    private void parseAddTownCorruptionCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics addtowncorruption [town] [amount]");
        }

        String townName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Town town = TownyUniverse.getInstance().getTown(townName);
        if (town == null) {
            throw new TownyException("Town not found: " + townName);
        }

        // Add corruption to the town
        townCorruptionManager.addCorruption(town, amount);
        double currentCorrupt = townCorruptionManager.getCorruption(town);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Added " + String.format("%.2f", amount) +
                " corruption to " + town.getName() + ". Current corruption: " +
                String.format("%.2f", currentCorrupt) + "%");
    }

    private void parseSetTownCorruptionCommand(CommandSender sender, String[] args) throws TownyException {
        if (args.length < 2) {
            throw new TownyException("Not enough arguments. Use: /ta politics settowncorruption [town] [amount]");
        }

        String townName = args[0];
        double amount;

        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            throw new TownyException("Invalid amount: " + args[1] + ". Please provide a valid number.");
        }

        Town town = TownyUniverse.getInstance().getTown(townName);
        if (town == null) {
            throw new TownyException("Town not found: " + townName);
        }

        // Set corruption for the town
        townCorruptionManager.setCorruption(town, amount);

        TownyMessaging.sendMsg(sender, ChatColor.GREEN + "Set " + town.getName() + "'s corruption level to " +
                String.format("%.2f", amount) + "%");
    }
}