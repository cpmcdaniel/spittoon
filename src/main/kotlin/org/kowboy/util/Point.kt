package org.kowboy.util

import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.ChunkSnapshot
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.kowboy.util.BukkitUtils.BLOCKS_PER_CHUNK
import java.text.DecimalFormat
import java.text.NumberFormat

/**
 * Utility functions and data classes for working with 2D and 3D coordinates.
 *
 * @author Craig McDaniel
 * @since 1.1
 */

fun formatPoint(x: Int, y: Int?, z: Int): String {
    val builder = StringBuilder()
            .append(ChatColor.RESET.toString() + "(" +
                    ChatColor.RED + x +
                    ChatColor.RESET + ", " )
    if (y != null) {
        builder.append(ChatColor.GREEN.toString() + y +
                       ChatColor.RESET + ", ")
    }
    builder.append(ChatColor.BLUE.toString() + z +
                   ChatColor.RESET + ")")
    return builder.toString()
}

fun formatPoint(x: Int, z: Int) = formatPoint(x, null, z)

fun formatPoint(x: Double, y: Double?, z: Double, fractionDigits: Int): String {
    val fmt = NumberFormat.getInstance() as DecimalFormat
    fmt.applyPattern("###0.0##")
    fmt.maximumFractionDigits = fractionDigits

    val builder = StringBuilder()
            .append(ChatColor.RESET.toString() + "(" +
                    ChatColor.RED + fmt.format(x) +
                    ChatColor.RESET + ", ")
    if (y != null) {
        builder.append(ChatColor.GREEN.toString() + fmt.format(y) +
                       ChatColor.RESET + ", ")
    }
    builder.append(ChatColor.BLUE.toString() + fmt.format(z) +
                   ChatColor.RESET + ")")
    return builder.toString()
}

fun formatPoint(x: Double, y: Double?, z: Double) = formatPoint(x, y, z, 0)
fun formatPoint(x: Double, z: Double, fractionDigits: Int) = formatPoint(x, null, z, fractionDigits)
fun formatPoint(x: Double, z: Double) = formatPoint(x, z, 0)

data class Point(val x: Double, val z: Double) {
    fun toBlockPoint() = BlockPoint(x.toInt(), z.toInt())
    fun toChunkPoint() = toBlockPoint().toChunkPoint()
    override fun toString() = formatPoint(x, z, 1) // default to one decimal place
}
data class BlockPoint(val x: Int, val z: Int) {
    fun toPoint() = Point(x.toDouble(), z.toDouble())
    fun toChunkPoint() = ChunkPoint(x / BLOCKS_PER_CHUNK, z / BLOCKS_PER_CHUNK)
    override fun toString() = formatPoint(x, z)
}
data class ChunkPoint(val x: Int, val z: Int) {
    fun toPoint() = toBlockPoint().toPoint()
    fun toBlockPoint() = BlockPoint(x * BLOCKS_PER_CHUNK, z * BLOCKS_PER_CHUNK)
    override fun toString() = formatPoint(x, z)
}

// Conversion from Bukkit types to Point
fun Location.toPoint() = Point(x, z)
fun Entity.toPoint() = location.toPoint()
fun Block.toPoint() = Point(x.toDouble(), z.toDouble())
fun Chunk.toPoint() = toBlockPoint().toPoint()

// Conversion from Bukkit types to BlockPoint
fun Location.toBlockPoint() = BlockPoint(blockX, blockZ)
fun Entity.toBlockPiont() = location.toBlockPoint()
fun Block.toBlockPoint() = BlockPoint(x, z)
fun Chunk.toBlockPoint() = toChunkPoint().toBlockPoint()

// Conversion from Bukkit types to ChunkPoint
fun Location.toChunkPoint() = toBlockPoint().toChunkPoint()
fun Entity.toChunkPoint() = location.toChunkPoint()
fun Block.toChunkPoint() = toBlockPoint().toChunkPoint()
fun Chunk.toChunkPoint() = ChunkPoint(x, z)

// Format functions for Bukkit types
fun Location.formatPoint() = toPoint().toString()
fun Location.formatPoint(fractionDigits: Int) = formatPoint(x, y, z, fractionDigits)
fun Entity.formatPoint() = toBlockPiont().toString()
fun Entity.formatPoint(fractionDigits: Int) = formatPoint(location.x, location.y, location.z, fractionDigits)
fun Block.formatPoint() = toBlockPoint().toString()
fun Chunk.formatPoint() = toChunkPoint().toString()

// Aliases for filter types
typealias BlockFilter = (ChunkSnapshot, Int, Int, Int) -> Boolean
typealias BlockPointFilter = (BlockPoint) -> Boolean
typealias ChunkPointFilter = (ChunkPoint) -> Boolean