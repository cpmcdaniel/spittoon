package org.kowboy.command

import org.bukkit.block.Biome
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.kowboy.util.BukkitUtils.findNearestBlock
import org.kowboy.util.BukkitUtils.getBiome
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendInfo
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.TabCompletionUtils.partialMatch
import java.util.*
import java.util.stream.Collectors

/**
 * Finds the nearest chunk with the given biome and print the chunk coordinates.
 *
 *
 * This is not an exhaustive search and likely to miss small biomes, as biome designation is store per block since
 * minecraft 1.15. That's right, it's not per chunk or even per (x, z) coordinate - it's in 3D!
 *
 *
 * Obviously an exhaustive search, even in 2 dimensions, would be very expensive and cause major server lag. As a
 * result, the algorithm will hold the y-value constant (sea level) and sample a few blocks from each chunk. There are
 * also range limits to consider and unexplored chunks will have to be generated. Both of these issues have a big
 * impact on performance. As such, both of these things are configurable (biome-search-apothem and
 * search-ungenerated-chunks.
 *
 * TODO: Implement config for search-ungenerated-chunks and optional parameter for search apothem.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
internal class FindBiomeCommandExecutor(plugin: JavaPlugin?) : AbstractCommandExecutor(plugin!!), TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            help(sender, true)
            return true
        }
        val biome = getBiome(args[0])
        if (null == biome) {
            sendError(sender, "Invalid biome: " + args[0])
            help(sender, false)
            return true
        }
        // Start with chunk at player location
        val player = sender as Player
        val world = player.world
        val seaLevel = world.seaLevel
        object : BukkitRunnable() {
            override fun run() {
                val nearestBlock = findNearestBlock(player, 1024) { (x, z) -> biome == world.getBiome(x, seaLevel, z) }
                if (nearestBlock != null) {
                    sendSuccess(sender, "Nearest " + biome.toString().toLowerCase() + ": " +
                            nearestBlock)
                } else {
                    sendInfo(sender, "Not found")
                }
            }
        }.runTaskLater(plugin, 10)
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        return if (args.size == 1) {
            if (null != getBiome(args[0])) emptyList() else Biome.values().asSequence()
                    .map { biome: Biome -> biome.name }
                    .map { it.toLowerCase() }
                    .filter(partialMatch(args))
                    .sorted()
                    .toList()
        } else emptyList()
    }

    override val description: String
        protected get() = "Finds the nearest chunk with the given biome and prints it's coordinates"

    override val usage: String
        protected get() = "biome <biome-type>"

    companion object {
        private const val DEFAULT_SEARCH_APOTHEM = 8
    }
}