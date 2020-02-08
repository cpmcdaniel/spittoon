package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.ChatUtils.notPlayerError
import org.kowboy.util.TabCompletionUtils.stopCompletion

/**
 * A command executor for finding blocks, entities, and slime chunks near the player.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class FindCommandExecutor internal constructor(plugin: JavaPlugin) : CompositeCommandExecutor("find", "Find nearby blocks, enities, biomes, and slime chunks.") {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender is Player) {
            return super.onCommand(sender, command, label, args)
        }
        notPlayerError(sender)
        return true // don't print usage text
    }

    init {
        addSubCommand("entities",
                SenderPermissionFilter(FindEntitiesCommandExecutor(plugin), "spittoon.find.entity"),
                stopCompletion())
        val findEntity = FindEntityCommandExecutor(plugin)
        addSubCommand("entity",
                SenderPermissionFilter(findEntity, "spittoon.find.entity"),
                findEntity)
        val excludeEntity = ExcludeEntityCommandExecutor(plugin)
        addSubCommand("exclude-entity",
                SenderPermissionFilter(excludeEntity, "spittoon.find.exclude-entity"),
                excludeEntity)
        val findBlock = FindBlockCommandExecutor(plugin)
        addSubCommand("block",
                SenderPermissionFilter(findBlock, "spittoon.find.block"),
                findBlock)
        addSubCommand("blocks",
                SenderPermissionFilter(FindBlocksCommandExecutor(plugin), "spittoon.find.block"),
                stopCompletion())
        addSubCommand("slime",
                SenderPermissionFilter(FindSlimeCommandExecutor(plugin), "spittoon.find.slime"),
                stopCompletion())
        val findBiome = FindBiomeCommandExecutor(plugin)
        addSubCommand("biome",
                SenderPermissionFilter(findBiome, "spittoon.find.biome"),
                findBiome)
    }
}