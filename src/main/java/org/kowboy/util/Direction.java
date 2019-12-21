package org.kowboy.util;

public enum Direction {
    NORTH(0, -1, -1, 1),
    EAST(1, 0, -1, -1),
    SOUTH(0, 1, 1, -1),
    WEST(-1, 0, 1, 1);


    private final int dx; // X direction [-1, 0, 1] = [West, North/South, East]
    private final int dz; // Z direction [-1, 0, 1] = [North, East/West, South]
    private final int ox; // X offset sign (see offsetX below)
    private final int oz; // Z offset sign (see offsetZ below)

    Direction(int dx, int dz, int ox, int oz) {
        this.dx = dx;
        this.dz = dz;
        this.ox = ox;
        this.oz = oz;
    }

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
     * @return The offset to add to the X coordinate for the first step in the walk. Sign is determined by <code>this.ox</code>.
     */
    public int offsetX(int offset) {
        return this.ox * Math.abs(offset);
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
     * @return The offset to add to the Z coordinate for the first step in the walk. Sign is determined by <code>this.oz</code>.
     */
    public int offsetZ(int offset) {
        return this.oz * Math.abs(offset);
    }

    /**
     * For walking along a path in a certain direction.
     *
     * @param step The step number along this direction.
     * @return Returns 0 if moving North or South, -step if moving West and step if moving East.
     */
    public int stepX(int step) {
        return this.dx * Math.abs(step);
    }

    /**
     * For walking along a path in a certain direction.
     *
     * @param step The step number along this direction.
     * @return Returns 0 if moving East or West, -step if moving North and step if moving South.
     */
    public int stepZ(int step) {
        return this.dz * Math.abs(step);
    }
}
