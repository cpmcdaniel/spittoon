package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.BukkitUtils
import org.kowboy.util.ChatUtils
import org.kowboy.util.TabCompletionUtils
import java.util.*
import java.util.stream.Collectors

/**
 * Adds to the set of entity types that are filtered out of `find entities` search results.
 *
 * @author Craig McDaniel
 * @since 1.3
 */
open class ExcludeEntityCommandExecutor(plugin: JavaPlugin) : AbstractCommandExecutor(plugin), TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            help(sender, true)
            return true
        }
        val entityType = BukkitUtils.getEntityType(args[0])
        if (null == entityType) {
            ChatUtils.sendError(sender, "Invalid entity type: " + args[0])
            help(sender, false)
            return true
        }

        val blacklist = plugin.config.getStringList("finder.entity-blacklist")
        val blackset = blacklist.toMutableSet()
        blackset.add(entityType.toString().toLowerCase())
        plugin.config.set("finder.entity-blacklist", blackset.toSortedSet().toList())
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        return if (args.size == 1) {
            if (null != BukkitUtils.getEntityType(args[0])) emptyList() else Arrays.stream(EntityType.values())
                    .filter { et: EntityType -> et.isAlive }
                    .map { et: EntityType -> et.name.toLowerCase() }
                    .filter(TabCompletionUtils.partialMatch(args))
                    .collect(Collectors.toList())
        } else emptyList()
    }

    override val description: String
        get() = "Blacklist the given entity type from search results."

    override val usage: String
        get() = "exclude-entity <type>"
}