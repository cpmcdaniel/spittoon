package org.kowboy.util

import kotlin.math.abs

enum class Direction(
        private val dx: Int,             // X direction [-1, 0, 1] = [West, North/South, East]
        private val dz: Int,             // Z direction [-1, 0, 1] = [North, East/West, South]
        private val ox: Int,             // X offset sign (see offsetX below)
        private val oz: Int) {           // Z offset sign (see offsetZ below)
    NORTH(0, -1, -1, 1),
    EAST(1, 0, -1, -1),
    SOUTH(0, 1, 1, -1),
    WEST(-1, 0, 1, 1);

    /**
     * For walking concentric squares clockwise around the compass. Modifies the offset used to calculate the X-coordinate
     * of the first step in the walk.
     *
     * Walking North offsets the corner to the West (negative X).
     * Walking South offsets the corner to the East (positive X).
     * Walking East offsets the corner to the West (negative X).
     * Walking West offsets the corner to the East (positive X).
     *
     * @param offset The magnitude of the offset.
     * @return The offset to add to the X coordinate for the first step in the walk. Sign is determined by `this.ox`.
     */
    fun offsetX(offset: Int): Int {
        return ox * abs(offset)
    }

    /**
     * For walking concentric squares clockwise around the compass. Modifies the offset used to calculate the Z-coordinate
     * of the first step in the walk.
     * Walking North offsets the corner to the South (positive Z).
     * Walking South offsets the corner to the North (negative Z).
     * Walking East offsets the corner to the North (negative Z).
     * Walking West offsets the corner to the South (positive Z).
     *
     * @param offset The magnitude of the offset.
     * @return The offset to add to the Z coordinate for the first step in the walk. Sign is determined by `this.oz`.
     */
    fun offsetZ(offset: Int): Int {
        return oz * abs(offset)
    }

    /**
     * For walking along a path in a certain direction.
     *
     * @param step The step number along this direction.
     * @return Returns 0 if moving North or South, -step if moving West and step if moving East.
     */
    fun stepX(step: Int): Int {
        return dx * abs(step)
    }

    /**
     * For walking along a path in a certain direction.
     *
     * @param step The step number along this direction.
     * @return Returns 0 if moving East or West, -step if moving North and step if moving South.
     */
    fun stepZ(step: Int): Int {
        return dz * abs(step)
    }
}