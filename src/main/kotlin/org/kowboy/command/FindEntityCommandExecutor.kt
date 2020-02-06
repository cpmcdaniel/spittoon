package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.kowboy.util.BukkitUtils.TICKS_PER_MINUTE
import org.kowboy.util.BukkitUtils.getEntityType
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.TabCompletionUtils.partialMatch
import org.kowboy.util.formatPoint
import java.util.*
import java.util.stream.Collectors

/**
 * Finds all of a given LivingEntity type currently in the world, sorts them by distance from the player, and
 * shows the coordinates of the top-N closest mobs of that type. The default for N is 8.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
open class FindEntityCommandExecutor(plugin: JavaPlugin) : AbstractCommandExecutor(plugin), TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            help(sender, true)
            return true
        }
        val entityType = getEntityType(args[0])
        if (null == entityType) {
            sendError(sender, "Invalid entity type: " + args[0])
            help(sender, false)
            return true
        }
        val player = sender as Player
        val playerLoc = player.location
        sendSuccess(sender, "---- ENTITIES FOUND ----")
        player.world.livingEntities.asSequence()
                .filter { le: LivingEntity -> entityType == le.type }
                .map { le: LivingEntity -> EntityDistance(le, playerLoc.distance(le.location)) }
                .sorted()
                .take(TOP_N)
                .map { ed: EntityDistance -> ed.entity }
                .map { le: LivingEntity ->
                    le.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, TICKS_PER_MINUTE * 5, 1))
                    toString(le)
                }
                .forEach { s: String -> sendSuccess(player, s) }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String>? {
        return if (args.size == 1) {
            if (null != getEntityType(args[0])) emptyList() else Arrays.stream(EntityType.values())
                    .filter { et: EntityType -> et.isAlive }
                    .map { et: EntityType -> et.name }
                    .map { obj: String -> obj.toLowerCase() }
                    .filter(partialMatch(args))
                    .collect(Collectors.toList())
        } else emptyList()
    }

    internal inner class EntityDistance(val entity: LivingEntity, private val distance: Double) : Comparable<EntityDistance> {
        override fun compareTo(other: EntityDistance): Int {
            return distance.compareTo(other.distance)
        }

    }

    private fun toString(le: LivingEntity): String {
        return le.type.toString().toLowerCase() + " - " + le.formatPoint()
    }

    override val description: String
        get() = "Find nearby mobs of the given type."

    override val usage: String
        get() = "entity <type>"

    companion object {
        private const val TOP_N: Int = 8
    }
}