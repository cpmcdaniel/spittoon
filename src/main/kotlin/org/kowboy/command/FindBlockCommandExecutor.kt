package org.kowboy.command

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.BukkitUtils.chunkBlocks
import org.kowboy.util.BukkitUtils.chunkSpiral
import org.kowboy.util.BukkitUtils.getMaterial
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.NumberUtils.isBetween
import org.kowboy.util.TabCompletionUtils.partialMatch
import org.kowboy.util.Vein
import org.kowboy.util.addBlock
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Locates veins of blocks of a specified type nearby and displays summary information to the player (location, how
 * many blocks in the vein, etc).
 *
 * @author Craig McDaniel
 * @since 1.0
 */
internal class FindBlockCommandExecutor(plugin: JavaPlugin) : AbstractCommandExecutor(plugin), TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val player = sender as Player
        if (args.isNotEmpty()) {
            val mat = getMaterial(args[0])
            if (null == mat) {
                sendError(sender, "Invalid block material: " + args[0])
                help(sender, false)
            } else if (args.size >= 2) {
                if (isBetween(args[1], 0, MAX_APOTHEM)) {
                    val apothem = args[1].toInt()
                    displayVeinSummary(player, mat, apothem)
                } else {
                    sendError(sender, "Apothem must be between 0 and $MAX_APOTHEM.")
                    help(sender, false)
                }
            } else { // no apothem param, use default.
                displayVeinSummary(player, mat)
            }
        } else {
            help(sender, true)
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        return if (args.isNotEmpty()) {
            if (null != getMaterial(args[0])) emptyList() else Arrays.stream(Material.values())
                    .filter { obj: Material -> obj.isBlock }
                    .map { obj: Material -> obj.name }
                    .filter(partialMatch(args))
                    .map { obj: String -> obj.toLowerCase() }
                    .collect(Collectors.toList())
        } else emptyList()
    }

    override val description: String
        get() = "Finds nearby veins of blocks of the given type"

    override val usage: String
        get() = "block <block-type> [apothem]"

    companion object {
        private const val DEFAULT_APOTHEM = 0 // only searches the Player's chunk
        private const val MAX_APOTHEM = 2
        private const val CHUNK_BLOCK_LIMIT = 1000
        private const val TOP_N = 5 // only show 5 closest veins.
        @JvmOverloads
        fun displayVeinSummary(player: Player, mat: Material, apothem: Int = DEFAULT_APOTHEM) {
            sendSuccess(player, "---- VEINS FOUND ----")
            findVeins(player, mat, apothem)
                    .take(TOP_N)
                    .map { obj: Vein -> obj.toString() }
                    .forEach { s: String -> sendSuccess(player, s) }
        }

        private fun findVeins(player: Player, mat: Material, apothem: Int): Set<Vein> {
            return chunkSpiral(player, apothem)
                    .flatMap { (x, z) -> chunkBlocks(player.world, x, z, mat) }
                    // We are only going to show the top 5 closes veins, so even with a large apothem, we probably don't
                    // need a huge number of blocks. Also, the next step is an aggregate operation, so we need to realize
                    // all of the stream values. This avoids realizing way more than we need.
                    .take(CHUNK_BLOCK_LIMIT)
                    .fold(sortedSetOf<Vein>()) { vs: Set<Vein>, block: Block -> vs.addBlock(block, player) }
        }
    }
}