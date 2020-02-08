package org.kowboy.listener

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.kowboy.SpittoonPlugin
import org.kowboy.task.LightLevelTask
import java.util.*

/**
 *
 * When a player sneaks, this listener will schedule a task to show light levels on blocks within a configured apothem.
 * If such a task is already running for the given [org.bukkit.entity.Player], the event handler does nothing.
 * This prevents spawning many concurrent tasks for the same Player that would all be doing the same thing anyway.
 *
 *
 * This handler does nothing if the "light-level.on" config setting is `false`.
 *
 *
 * Additionally, the task is only scheduled if the player is also holding a torch in either the main hand
 * or offhand.
 *
 * @author  Craig McDaniel
 * @since   1.0
 * @see LightLevelTask
 */
class LightLevelListener(plugin: SpittoonPlugin) : Listener {
    private val plugin: JavaPlugin
    // Keeps track of which players have an active light level task.
    // This is used to prevent spawning additional tasks on every move event.
    private val locks: MutableSet<UUID> = HashSet()

    private val lightSources = setOf(
            Material.TORCH,
            Material.BEACON,
            Material.GLOWSTONE,
            Material.JACK_O_LANTERN,
            Material.REDSTONE_LAMP,
            Material.SEA_LANTERN,
            Material.SEA_PICKLE,
            Material.LANTERN,
            Material.CONDUIT,
            Material.CAMPFIRE,
            Material.END_ROD,
            Material.ENDER_CHEST,
            Material.REDSTONE_TORCH,
            Material.MAGMA_BLOCK)

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        val player = event.player ?: return
        // Do nothing if the feature is not enabled/
        if (!plugin.config.getBoolean("light-level.on", false)) return
        // Do nothing if there is already a task for this player
        if (locks.contains(player.uniqueId)) return
        // Only create the task if the player is sneaking...
        if (!event.isSneaking) return
        // ...and holding a torch
        if (!isHoldingLight(player)) return
        scheduleTask(player)
    }

    private fun isHoldingLight(player: Player): Boolean {
        val mainHand = player.inventory.itemInMainHand.type
        val offHand = player.inventory.itemInOffHand.type
        return mainHand in lightSources || offHand in lightSources
    }

    private fun scheduleTask(player: Player) {
        if (lock(player)) {
            val apothem = plugin.config.getInt("light-level.apothem", DEFAULT_APOTHEM.toInt())
            val task: BukkitRunnable = LightLevelTask(apothem, player, Runnable { release(player) })
            task.runTaskTimer(plugin, 10, 10)
        } else {
            // Theoretically, this should never happen. I believe event handlers are all processed in a
            // single thread, so there should be no lock contention.
            plugin.logger.warning("Player " + player.name + " already has a lock for light-level task")
        }
    }

    private fun lock(p: Player): Boolean {
        synchronized(this) {
            if (locks.contains(p.uniqueId)) return false
            locks.add(p.uniqueId)
            return true
        }
    }

    private fun release(p: Player) {
        synchronized(this) {
            if (locks.contains(p.uniqueId)) {
                locks.remove(p.uniqueId)
            }
        }
    }

    companion object {
        private const val DEFAULT_APOTHEM: Byte = 8
    }

    init {
        plugin.registerEvents(this)
        this.plugin = plugin
    }
}