package org.kowboy.command

import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.BlockFilter
import org.kowboy.util.BukkitUtils.chunkBlocks
import org.kowboy.util.BukkitUtils.chunkSpiral
import org.kowboy.util.BukkitUtils.getMaterial
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.NumberUtils.isBetween
import org.kowboy.util.Vein
import org.kowboy.util.addBlock
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Finds all veins for a configured set of block types within an apothem around the player.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
internal class FindBlocksCommandExecutor(plugin: JavaPlugin) : AbstractCommandExecutor(plugin) {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val player = sender as Player
        if (args.isNotEmpty()) {
            when {
                "help".equals(args[0], ignoreCase = true) -> {
                    help(sender, true)
                }
                isBetween(args[0], 0, MAX_APOTHEM) -> {
                    val apothem = args[0].toInt()
                    displayVeinSummary(player, apothem)
                }
                else -> {
                    sendError(sender, "Apothem must be between 0 and $MAX_APOTHEM.")
                    help(sender, false)
                }
            }
        } else { // no apothem param, use default
            displayVeinSummary(player)
        }
        return true
    }

    private fun displayVeinSummary(player: Player, apothem: Int = DEFAULT_APOTHEM) {
        sendSuccess(player, "---- BLOCKS FOUND ----")
        findVeins(player, apothem)
                .groupBy { it.type }
                .values
                .map { veins -> veins.toSortedSet() }
                .map { it.first() }
                .map { it.toString() }
                .forEach { s: String -> sendSuccess(player, s) }
    }

    private fun findVeins(player: Player, apothem: Int): Sequence<Vein> {
        return chunkSpiral(player, apothem)
                .flatMap { (x, z) -> chunkBlocks(player.world, x, z, makeBlockFilter()) }
                .fold(setOf<Vein>()) {
                    veins, block -> veins.addBlock(block, player)
                }
                .asSequence()
    }

    private fun makeBlockFilter(): BlockFilter {
        val materialStrings = plugin.config.getStringList("finder.block-filter") ?: emptyList()
        val (knownMaterials, unknownMaterials) = materialStrings.partition { getMaterial(it) != null }
        unknownMaterials.forEach {
            plugin.logger.severe("Unrecognized block type in finder.block-finder config: " + it!!)
        }
        val searchSet = knownMaterials.map { getMaterial(it) }.filterNotNull().toSet()
        return { cs: ChunkSnapshot, x: Int, y: Int, z: Int ->
            cs.getBlockType(x, y, z) in searchSet
        }
    }

    override val description: String
        get() = "Finds mineral veins within an apothem around the player"

    override val usage: String
        get() = "blocks [apothem]"

    companion object {
        private const val DEFAULT_APOTHEM = 0
        private const val MAX_APOTHEM = 2
    }
}