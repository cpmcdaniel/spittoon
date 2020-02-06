package org.kowboy.util

import org.bukkit.*
import org.bukkit.block.Biome
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.kowboy.util.ChatUtils.sendDebug
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.math.min


/**
 * Utility methods for messing with Bukkit objects.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
object BukkitUtils {
    const val BLOCKS_PER_CHUNK: Byte = 16
    const val MAX_CHUNK_APOTHEM = 1024 // subject to change
    const val MAX_BLOCK_APOTHEM = MAX_CHUNK_APOTHEM * BLOCKS_PER_CHUNK
    const val TICKS_PER_SECOND = 20
    const val TICKS_PER_MINUTE = TICKS_PER_SECOND * 60

    /**
     * Converts a String value to the appropriate Biome enum. Swallows [IllegalArgumentException] execption.
     *
     * @param s The string to convert.
     * @return The Biome enum value that matches the input, otherwise null.
     */
    fun getBiome(s: String?): Biome? {
        if (null != s) {
            try {
                return Biome.valueOf(s.toUpperCase())
            } catch (e: IllegalArgumentException) {
            }
        }
        return null
    }

    /**
     * Converts a String value to the appropriate EntityType enum. Swallows [IllegalArgumentException] execption.
     *
     * @param s The string to convert.
     * @return The EntityType enum value that matches the input, otherwise null.
     */
    fun getEntityType(s: String?): EntityType? {
        if (null != s) {
            try {
                return EntityType.valueOf(s.toUpperCase())
            } catch (e: IllegalArgumentException) {
            }
        }
        return null
    }

    /**
     * Converts a String value to the appropriate Material enum. Swallows [IllegalArgumentException] execption.
     *
     * @param s The string to convert.
     * @return The Material enum value that matches the input, otherwise null.
     */
    fun getMaterial(s: String): Material? {
        return Material.getMaterial(s.toUpperCase())
    }

    /**
     * Finds the (x, z) coordinates of the nearest chunk that matches the given predicate. Does not actually load
     * chunk objects, as this could force generation/loading of chunks. Subject to limitations for the sake of
     * performance.
     *
     * @param player The search is centered around the player.
     * @param r The maximum number of concentric squares (or "rings") to search.
     * @param chunkPointFilter The predicate for finding a match. Stops as soon as it finds a match.
     * @return The (x, z) chunk coordinates of the nearest match to the Player.
     */
    fun findNearestChunk(player: Player, r: Int, chunkPointFilter: ChunkPointFilter): ChunkPoint? {
        return chunkSpiral(player, r)
                .takeWhile { (x, z) -> player.world.isChunkGenerated(x, z) }
                .filter { chunkPointFilter(it) }
                .firstOrNull()
    }

    /**
     * Finds the (x, z) coordinates of the nearest block that matches the given predicate. Does not actually load
     * block objects, as this could force generation/loading of chunks. Subject to limitations for the sake of
     * performance.
     *
     * @param player The search is centered around the player.
     * @param r The maximum number of concentric squares (or "rings") to search.
     * @param blockPointFilter The predicate for finding a match. Stops as soon as it finds a match.
     * @return The (x, z) block coordinates of the nearest match to the Player.
     */
    fun findNearestBlock(player: Player, r: Int, blockPointFilter: BlockPointFilter): BlockPoint? {
        return chunkSpiral(player, r)
                // Limiting to generated chunks only may or may not be desirable?
                .takeWhile { (x, z) -> player.world.isChunkGenerated(x, z) }
                .flatMap { sampleFourBlocks(it) }
                .filter { blockPointFilter(it) }
                .firstOrNull()
    }

    /**
     * We will spiral outward by chunk, starting at the player's location. The spiral will move clockwise around
     * the compass, starting with the first move to the adjacent chunk Southwest of the center, then walking North to
     * the Northest corner. At this point we are walking the first square "ring" around the center chunk in a clockwise
     * direction. We then repeat by moving to the Southwest corner of the next ring of chunks.
     *
     * @param p The player used to center the spiral.
     * @param rings The maximum numer of spiral "rings".
     * @return A Sequence of Chunk coordinates (x, z) in spiral order.
     */
    fun chunkSpiral(player: Player, rings: Int): Sequence<ChunkPoint> {
        // It may be easier to think of this as concentric squares, where the player chunk will be the very center 1x1 square.
        // The center of the spiral is really the very center block of the chunk, but it helps if you think of chunks as
        // points on a piece of graph paper with the center chunk being the origin of the coordinate system for the
        // spiral.
        //
        // Note, the following algorithm uses chunk (not block) coordinates exclusively.
        return chunkSpiral(player.toChunkPoint(), rings)
    }

    fun chunkSpiral(center: ChunkPoint, rings: Int): Sequence<ChunkPoint> {
        // Constrain the apothem to something reasonable
        val apothem = min(rings, MAX_CHUNK_APOTHEM)
        // Start with just the center chunk
        return (sequenceOf(center) + (0..apothem).asSequence()
                .flatMap { r: Int ->
                    Direction.values().asSequence().flatMap { dir -> walk(dir, center, r) } })
    }

    /**
     * Returns a sequence of chunk coordinates by walking in the given direction along the given ring number.
     *
     * @param d The direction to walk.
     * @param center The coordinates of the center chunk.
     * @param r The number of rings from center.
     * @return A Sequence of chunk coordinates for one side of the ring.
     */
    fun walk(d: Direction, center: ChunkPoint, r: Int): Sequence<ChunkPoint> {
        // Constrain the apothem to something reasonable.
        val apothem = min(r, MAX_BLOCK_APOTHEM)
        return if (apothem == 0) {
            sequenceOf() // we don't walk ring 0.
        } else (0 until (2 * apothem)).asSequence().map {
            // Consider the first ring (r=1). The North walk starts at the SE corner chunk of the ring and stops just
            // shy of the NW corner chunk (because it will be the start of the East walk). The size of the first ring
            // square is 3x3, so the length of the walk is 2. Thus, for any r, the walk length is 2r.
            val (x, z) = center
            ChunkPoint(x + d.offsetX(apothem) + d.stepX(it),
                    z + d.offsetZ(apothem) + d.stepZ(it))
        }
    }

    /**
     * Converts the given chunk coordinates to a stream of 2D block coordinates (x, z). The sampled blocks will form
     * a repeating pattern when combined with the samples adjacent chunks. To avoid performance issues, we err on the
     * side of under-sampling. The default behavior is to split the chunk into quadrants and sample one block from each.
     * Thus, we are sampling 4 blocks.
     *
     * Note that this may miss catching small biomes that have sections smaller than 8 blocks in X or Z direction. The
     * type that comes to mind is the RIVER biome, which could potentially snake it's way through a chunk without
     * intersecting one of the sampled chunks. The probability of this happening is relatively low (unless the river only
     * appears at the very edge of the search area, the very outer ring).
     *
     * @param cp The (x, z) chunk coordinates.
     * @return A sequence of (x, z) block coordinates.
     */
    fun sampleFourBlocks(cp: ChunkPoint): Sequence<BlockPoint> {
        // Sample the NW corner of each quadrant in the chunk.
        // First block is (0, 0) within the chunk (NW corner). All other samples will be offset from this.
        val nw = cp.toBlockPoint()     // First sample NW quadrant (+0, +0).
        val ne = nw.copy(x = nw.x + 8) // NE = NW + 8 blocks East (+8, +0).
        val se = ne.copy(z = ne.z + 8) // SE = NE + 8 blocks South (+8, +8).
        val sw = nw.copy(z = ne.z + 8) // SW = NW + 8 blocks South (+0, +8).
        return sequenceOf(nw, ne, se, sw)
    }

    /**
     * Creates a sequence of all the blocks in the given chunk matching the given block type.
     *
     * @param chunk The chunk to stream blocks from.
     * @param blockType The Material used to filter blocks through the sequence.
     * @return A sequence of blocks matching the given material.
     */
    fun chunkBlocks(chunk: Chunk, blockType: Material): Sequence<Block> {
        return chunkBlocks(chunk) { cs, x, y, z -> blockType == cs.getBlockType(x, y, z) }
    }

    fun chunkBlocks(world: World, x: Int, z: Int, blockType: Material): Sequence<Block> {
        return chunkBlocks(world.getChunkAt(x, z), blockType)
    }


    /**
     * Creates a sequence of all the blocks in the given chunk matching the given block type.
     *
     * @param chunk The chunk to stream blocks from.
     * @param blockFilter The filter to use for selecting blocks from the chunk.
     * @return A sequence of blocks matching the given filter.
     */
    fun chunkBlocks(chunk: Chunk, blockFilter: BlockFilter): Sequence<Block> {
        // Take a snapshot. Do not include biome data.
        val cs = chunk.getChunkSnapshot(true, false, false)
        val highestBlockY = highestBlockYFn(chunk.world, cs)
        return (0 until BLOCKS_PER_CHUNK).asSequence().flatMap {
            x: Int ->
            (0 until BLOCKS_PER_CHUNK).asSequence().flatMap {
                z: Int ->
                (1..highestBlockY(x, z)).asSequence()
                        .filter { blockFilter(cs, x, it, z) }
                        .map { chunk.getBlock(x, it, z) }
            }
        }
    }

    fun chunkBlocks(world: World, x: Int, z: Int, blockFilter: BlockFilter): Sequence<Block> {
        return chunkBlocks(world.getChunkAt(x, z), blockFilter)
    }

    private fun highestBlockYFn(world: World, cs: ChunkSnapshot): (Int, Int) -> Int {
        return if (world.environment == World.Environment.NORMAL) {
            { x: Int, z: Int -> cs.getHighestBlockYAt(x, z) }
        } else { x: Int, z: Int -> 127 }
    }

    /**
     * Sends a formatted message to the sender using the subject and then returning the subject to the caller.
     *
     * @param sender The command sender (either admin or player).
     * @param subject The object to format as string.
     * @param format A transform function that takes the subject and returns a formatted string. The function should
     * not mutate the state of the subject in any way, though there is no way to enforce this.
     * @return The subject parameter, unaltered.
     */
    fun <T> spy(sender: CommandSender, subject: T, format: (T) -> String): T {
        sendDebug(sender, format(subject))
        return subject
    }
}
