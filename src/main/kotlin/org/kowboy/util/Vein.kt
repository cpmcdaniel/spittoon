package org.kowboy.util

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.kowboy.util.ChatUtils.getColor
import java.util.*
import java.util.function.Predicate

/**
 * A representation of a mineral vein.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class Vein(block: Block,
    // Approximate distance from the player. Only really used for sorting veins.
    private var distance: Double) : Comparable<Vein> {

    // We will keep a +1 sized bounding box around the vein blocks.
    private var box: BoundingBox

    // How many blocks are in this vein.
    var count: Int
        private set

    // All blocks in a vein are of the same type of material.
    val type: Material = block.type

    // Note, we could keep a collection of all the blocks in the vein, but we don't have a use for that at this point.
    val location: Location = block.location

    constructor(block: Block, player: Player) : this(block, player.location.distance(block.location)) {}

    init {
        box = BoundingBox.of(block).expand(1.0)
        count = 1
    }

    /**
     * Does this vein contain the given block within it's bounding box? Is it the same material?
     *
     * @param block The block to test.
     * @return True if the block is part of the vein, false otherwise.
     */
    operator fun contains(block: Block): Boolean {
        return type == block.type && box.contains(BoundingBox.of(block))
    }

    /**
     * If this block belongs in the vein, add it.
     *
     * @param block The block to possibly add to this vein.
     * @return True if the block was added, false otherwise.
     */
    fun addBlock(block: Block): Boolean {
        if (!contains(block)) return false
        count++
        box = box.union(BoundingBox.of(block).expand(1.0))
        return true
    }

    /**
     * Detects if two veins overlap.
     *
     * @param v The vein to test against this one.
     * @return True if the two veins overlap, false otherwise.
     */
    fun overlaps(v: Vein): Boolean { // Keep in mind that the bounding box is a +1 block buffer around each vein. We therefore need to shrink
        // one of them by 1 or we risk a false positive (where blocks that are 1 block apart are considered overlapping).
        return box.overlaps(v.box.clone().expand(-1.0))
    }

    /**
     * Combines two veins together iff they overlap.
     *
     * @param v The vein to combine with this one.
     * @return The new combined vein iff they overlap, null otherwise.
     */
    fun combine(v: Vein): Vein? {
        if (type == v.type && overlaps(v)) {
            count += v.count
            distance = Math.min(distance, v.distance)
            box = box.union(v.box)
            return this
        }
        return null
    }

    override fun compareTo(v: Vein): Int {
        return distance.compareTo(v.distance)
    }

    override fun toString(): String { // Example: diamond_ore * 4: (-13, 12, 72)
        return getColor(type).toString() +
                type.name.toLowerCase() +
                ChatColor.GRAY +
                " * " +
                count +
                ": " +
                location.toBlockPoint()
    }
}

/**
 * Finds a vein from the set that the given block should belong to and adds it to the vein.
 *
 * @param block The block.
 * @return An Optional containing the vein that the given block was added to, otherwise the Optional will be empty.
 */
fun Set<Vein>.addToVein(block: Block): Vein? {
    return this.firstOrNull { vein -> vein.addBlock(block) }
}

/**
 * Adds a block to an existing vein if it can (if it belongs to one), or else creates a new vein and adds it
 * to the set.
 *
 * @param block The block to add.
 * @param player The player - used to calculate distance to the block.
 * @return The updated set of veins.
 */
fun Set<Vein>.addBlock(block: Block, player: Player): Set<Vein> {
    if (null == addToVein(block)) {
        return this + Vein(block, player.location.distance(block.location))
    }
    return this
}

/**
 * Iterates through the second set of veins and for each vein, combines it with an existing vein in the first set if
 * there is an overlap, otherwise adds the vein to the first set.
 *
 * @param vs2 The second set of veins.
 * @return The combined set of veins.
 */
fun Set<Vein>.combine(vs2: Set<Vein>): Set<Vein> {
    return vs2.fold(this) { acc, v2 ->
        val found = acc.asSequence().mapNotNull { v1 -> v1.combine(v2) }
                .firstOrNull()
        if (null == found) {
            acc + v2
        } else {
            acc
        }
    }
}