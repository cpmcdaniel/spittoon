package org.kowboy.command

import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.BukkitUtils.glow
import org.kowboy.util.ChatUtils.sendSuccess
import org.kowboy.util.formatPoint
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors

/**
 * Finds all living entities present in the world (except for players) and displays a summary per entity type.
 *
 *
 * The summary format is: `entity-type * count: (nearest-coordinates)`
 *
 *
 *
 * Example Summary: *spider * 12: (-176, 67, 34)*
 *
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class FindEntitiesCommandExecutor(private val plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // Get the entity type blacklist and convert to a set
        val blacklist = plugin.config["finder.entity-blacklist"] as List<String>?
        val blackset = blacklist?.toSet() ?: emptySet()

        sendSuccess(sender, "---- ENTITIES FOUND ----")
        val p = sender as Player
        p.world.livingEntities.asSequence()
                // don't include players
                .filter { le: LivingEntity -> le !is Player }
                .filter { le: LivingEntity -> le.type.toString().toLowerCase() !in blackset }
                // group entities by type
                .groupBy { it.type }
                // convert each group of entities into an EntityGroupSummary
                .map { (et, entities) ->
                    val groupSummary: EntityGroupSummary = EntityGroupSummary(p, et)
                    entities.fold(groupSummary) {
                        entityGroup: EntityGroupSummary, entity: LivingEntity ->
                        glow(entity, 5)
                        entityGroup.acc(entity)
                        entityGroup
                    }
                }.map { groupSummary: EntityGroupSummary -> groupSummary.toString() }
                .forEach {
                   s: String -> sendSuccess(sender, s)
                }
        return true
    }

    class EntityGroupSummary(val player: Player, val type: EntityType) {
        private var count = 0
        private var nearest: Location? = null
        private var nearestDistance = Double.MAX_VALUE

        // Accumulate a new entity into this result container
        fun acc(le: LivingEntity) {
            if (type != le.type) return

            // Increment the entity count
            count++
            // Possibly update the nearest location
            val d = player.location.distance(le.location)
            if (d < nearestDistance) {
                nearest = le.location
                nearestDistance = d
            }
        }

        override fun toString(): String {
            // Example:    spider * 6: (-176, 66, 191)
            return type.toString().toLowerCase() + " * " +
                    count + ": " +
                    (nearest?.formatPoint(0) ?: "()")
        }

    }

}