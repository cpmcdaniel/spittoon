package org.kowboy.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

import static org.kowboy.util.ChatUtils.sendError;
import static org.kowboy.util.ChatUtils.sendHelp;

/**
 * An abstract command executor that provides the basis for a heirarchy/tree of subcommands.
 *
 * Example:
 *
 * /spit light-level apothem 8
 *
 * In the above example, the command executor for "spit" would be a composite which may contain
 * additional subcommands beyond "light-level". The command executor for "light-level" may in turn be a composite
 * with subcommands "on", "off", and "apothem".
 *
 * Composite commands also provide a default "help" subcommand in order to provide usage summary, since capturing
 * an entire tree of subcommands in the usage text of the top-level command could scroll off the screen entirely. Such
 * top-level composite commands should only document their first level of subcommands in their usage text.
 *
 * Note: only the top-level composite executor should be registered with the plugin.
 *
 * Whenever {@link CommandExecutor#onCommand} is called and the first argument is equal to one of the subcommand names,
 * that subcommand's <code>onCommand</code> method is called with the args array shifted by one. Thus, subcommand
 * executors can safely assume they are called with only the args that come after the subcommand name in the original
 * args array.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public abstract class CompositeCommandExecutor implements CommandExecutor, TabCompleter {
    private final Map<String, CommandExecutor> subCommands = new LinkedHashMap<>();
    private final Map<String, TabCompleter> subCompleters = new LinkedHashMap<>();
    private final String commandName;
    private final String description;

    protected CompositeCommandExecutor(String commandName, String description) {
            this.commandName = commandName;
            this.description = description;
    }

    /**
     * @param commandName Name for this subcommand.
     * @param ce Executor for this subcommand.
     * @param tc Tab completer for this subcommand.
     */
    protected void addSubCommand(String commandName, CommandExecutor ce, TabCompleter tc) {
        subCommands.put(commandName.toLowerCase(), ce);
        if (tc != null) subCompleters.put(commandName.toLowerCase(), tc);
    }

    /**
     * Adds a composite subcommand, thus creating a command tree.
     *
     * @param commandName Name for this subcommand.
     * @param cce Both the command executor and tab completer for this subcommand.
     */
    protected void addSubCommand(String commandName, CompositeCommandExecutor cce) {
        addSubCommand(commandName, cce, cce);
    }

    /**
     * Adds a lead command executor with no tab completion.
     *
     * @param commandName Name for this subcommand.
     * @param ce Executor for this subcommand.
     */
    protected void addSubCommand(String commandName, CommandExecutor ce) {
        addSubCommand(commandName, ce, null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Composite commands must have at least one argument - the subcommand name.
        if (args.length < 1) {
            sendError(sender,"Missing sub-command");
            help(sender, false);
            return true;
        }

        // Get the subcommand executor
        CommandExecutor subCommand = subCommands.get(args[0]);
        if (subCommand == null) {
            if (!"help".equalsIgnoreCase(args[0])) {
                sendError(sender, "Unrecognized sub-command: " + args[0]);
                help(sender, false);
            } else {
                help(sender, true);
            }
            return true;
        }

        // Delegate command processing to the subcommand executor.
        // Shifts args by 1.
        return subCommand.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
    }

    protected void help(CommandSender sender, boolean withDescription) {
        if (withDescription) {
            sendHelp(sender, "Description: " + description);
        }
        // This prints all subcommand names on the same line. It may be preferable to split them one-per line
        // at some point.
        sendHelp(sender, "Usage: " + commandName + " <" +
                subCommands.keySet().stream().collect(Collectors.joining(", "))
                + ">");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1) {
            TabCompleter sub = subCompleters.get(args[0].toLowerCase());
            if (sub != null) {
                // Delegate tab completion to the subcommand tab completer.
                // Shifts args by 1.
                return sub.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
            } else {
                return new ArrayList<>(subCommands.keySet());
            }
        }
        return null;
    }
}
