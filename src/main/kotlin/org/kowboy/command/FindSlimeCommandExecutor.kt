package org.kowboy.command

import org.bukkit.Chunk
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.BukkitUtils.chunkSpiral
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.ChunkPoint
import org.kowboy.util.NumberUtils.isInteger
import org.kowboy.util.formatPoint
import java.util.stream.Stream

/**
 * Finds nearby slime chunks.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class FindSlimeCommandExecutor(plugin: JavaPlugin) : AbstractCommandExecutor(plugin) {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val player = sender as Player
        when {
            args.isEmpty() -> {
                displaySlimeChunks(player, DEFAULT_APOTHEM)
            }
            "help".equals(args[0], ignoreCase = true) -> {
                help(player, true)
            }
            isInteger(args[0]) -> {
                displaySlimeChunks(player, args[0].toInt())
            }
            else -> {
                sendError(player, "Invalid integer param: " + args[0])
                help(player, false)
            }
        }
        return true
    }

    private fun displaySlimeChunks(player: Player, apothem: Int) {
        sendSuccess(player, "---- SLIME CHUNKS ----")
        findSlimeChunks(player, apothem)
                .take(TOP_N)
                .map { it.formatPoint() }
                .forEach { sendSuccess(player, it) }
    }

    private fun findSlimeChunks(player: Player, apothem: Int): Sequence<Chunk> {
        val world = player.world
        return chunkSpiral(player, apothem)
                .map { (x, z) -> world.getChunkAt(x, z) }
                .filter { it.isSlimeChunk }
    }

    override val description: String
        get() = "Finds nearby slime chunks."

    override val usage: String
        get() = "slime [apothem]"

    companion object {
        private const val DEFAULT_APOTHEM = 8
        private const val TOP_N = 8
    }
}