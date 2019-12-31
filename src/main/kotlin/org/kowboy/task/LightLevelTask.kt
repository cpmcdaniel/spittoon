package org.kowboy.task

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.stream.IntStream

/**
 *
 *
 * Displays visual indicators of light-level in a apothem around the player. Uses colored particle effects to
 * indicate light level according to the following table.
 *
 * <table>
 * <thead>
 * <th>Light Level</th><th>Color</th>
</thead> *
 * <tbody>
 * <tr><td>12+</td><td>Bright Green</td></tr>
 * <tr><td>10,11</td><td>Green</td></tr>
 * <tr><td>8,9</td><td>Yellow</td></tr>
 * <tr><td>0-7</td><td>Dark Red*</td></tr>
</tbody> *
</table> *
 *
 *
 *
 * *The dark red color indicates that mobs can spawn on that block.
 *
 *
 *
 *
 * This task is repeated as long as the player is online and sneaking. If either of these conditions is false, the
 * task will cancel itself.
 *
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class LightLevelTask(private val apothem: Int, private val player: Player, private val onCancel: Runnable) : BukkitRunnable() {
    override fun run() {
        if (player.isOnline && player.isSneaking) {
            (-apothem until apothem).forEach { x ->
                (-apothem until apothem).forEach { z ->
                    (-apothem until 3).forEach { y ->
                        process(x, y, z)
                    }
                }
            }
        } else {
            cancel()
        }
    }

    @Synchronized
    @Throws(IllegalStateException::class)
    override fun cancel() {
        onCancel.run()
        super.cancel()
    }

    private fun process(x: Int, y: Int, z: Int) {
        val blockLocation = player.location.add(x.toDouble(), y.toDouble(), z.toDouble())
        val block = blockLocation.block
        // We only care about solid blocks that have Water or Air above.
        if (!block.type.isSolid) return
        if (!block.getRelative(BlockFace.UP).type.isSolid) {
            // Get the location of the center of the top face of the block
            val particleLoc = block.location.add(0.5, 1.0, 0.5)
            // getLightFromBlocks ignores light from the sun
            val lightLevel = particleLoc.block.lightFromBlocks
            val color = getColor(lightLevel)
            val data = DustOptions(color, 1.0f)
            player.spawnParticle(Particle.REDSTONE, particleLoc, 3, data)
        }
    }

    companion object {
        fun getColor(lightLevel: Byte): Color {
            if (lightLevel >= 12) return Color.LIME
            if (lightLevel >= 10) return Color.GREEN
            return if (lightLevel >= 8) Color.YELLOW else Color.MAROON
        }
    }

}