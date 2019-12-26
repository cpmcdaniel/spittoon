package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendHelp
import org.kowboy.util.TabCompletionUtils.partialMatch
import java.util.*
import java.util.stream.Collectors

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
 * Whenever [CommandExecutor.onCommand] is called and the first argument is equal to one of the subcommand names,
 * that subcommand's `onCommand` method is called with the args array shifted by one. Thus, subcommand
 * executors can safely assume they are called with only the args that come after the subcommand name in the original
 * args array.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
abstract class CompositeCommandExecutor protected constructor(private val commandName: String, private val description: String) : CommandExecutor, TabCompleter {
    private val subCommands: MutableMap<String, CommandExecutor> = LinkedHashMap()
    private val subCompleters: MutableMap<String, TabCompleter> = LinkedHashMap()

    /**
     * @param commandName Name for this subcommand.
     * @param ce Executor for this subcommand.
     * @param tc Tab completer for this subcommand.
     */
    protected fun addSubCommand(commandName: String, ce: CommandExecutor, tc: TabCompleter? = null) {
        subCommands[commandName.toLowerCase()] = ce
        if (tc != null) subCompleters[commandName.toLowerCase()] = tc
    }

    /**
     * Adds a composite subcommand, thus creating a command tree.
     *
     * @param commandName Name for this subcommand.
     * @param cce Both the command executor and tab completer for this subcommand.
     */
    protected fun addSubCommand(commandName: String, cce: CompositeCommandExecutor) {
        addSubCommand(commandName, cce, cce)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // Composite commands must have at least one argument - the subcommand name.
        if (args.isEmpty()) {
            sendError(sender, "Missing sub-command")
            help(sender, false)
            return true
        }
        // Get the subcommand executor
        val subCommand = subCommands[args[0]]
        if (subCommand == null) {
            if (!"help".equals(args[0], ignoreCase = true)) {
                sendError(sender, "Unrecognized sub-command: " + args[0])
                help(sender, false)
            } else {
                help(sender, true)
            }
            return true
        }
        // Delegate command processing to the subcommand executor.
        // Shifts args by 1.
        return subCommand.onCommand(sender, command, label, args.copyOfRange(1, args.size))
    }

    private fun help(sender: CommandSender?, withDescription: Boolean) {
        if (withDescription) {
            sendHelp(sender!!, "Description: $description")
        }
        // This prints all subcommand names on the same line. It may be preferable to split them one-per line
        // at some point.
        sendHelp(sender!!, "Usage: " + commandName + " <" +
                subCommands.keys.stream().collect(Collectors.joining(", "))
                + ">")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        if (args.isNotEmpty()) {
            val sub = subCompleters[args[0].toLowerCase()]
            return if (sub != null) {
                // Delegate tab completion to the subcommand tab completer.
                // Shifts args by 1.
                sub.onTabComplete(sender, command, alias, args.copyOfRange(1, args.size))
            } else {
                subCommands.keys.stream()
                        .filter(partialMatch(args))
                        .collect(Collectors.toList())
            }
        }
        return emptyList()
    }

}