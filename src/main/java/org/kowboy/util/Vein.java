package org.kowboy.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.Optional;
import java.util.Set;

import static org.kowboy.util.BukkitUtils.formatLocation;

/**
 * A representation of a mineral vein.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class Vein implements Comparable<Vein> {
    // We will keep a +1 sized bounding box around the vein blocks.
    private BoundingBox box;

    // Approximate distance from the player. Only really used for sorting veins.
    private double distance;

    // How many blocks are in this vein.
    private int count;

    // All blocks in a vein are of the same type of material.
    private final Material type;

    // Note, we could keep a collection of all the blocks in the vein, but we don't have a use for that at this point.
    private Location location;

    public Vein(final Block block, final double distance) {
        this.distance = distance;
        location = block.getLocation();
        type = block.getType();
        box = BoundingBox.of(block).expand(1);
        count = 1;
    }

    public Vein(final Block block, final Player player) {
        this(block, player.getLocation().distance(block.getLocation()));
    }

    public int getCount() {
        return count;
    }

    public Location getLocation() {
        return location;
    }

    public Material getType() {
        return type;
    }

    /**
     * Does this vein contain the given block within it's bounding box? Is it the same material?
     *
     * @param block The block to test.
     * @return True if the block is part of the vein, false otherwise.
     */
    public boolean contains(final Block block) {
        return type.equals(block.getType()) && box.contains(BoundingBox.of(block));
    }

    /**
     * If this block belongs in the vein, add it.
     *
     * @param block The block to possibly add to this vein.
     * @return True if the block was added, false otherwise.
     */
    public boolean addBlock(final Block block) {
        if (!contains(block)) return false;
        count++;
        box = box.union(BoundingBox.of(block).expand(1));
        return true;
    }

    /**
     * Detects if two veins overlap.
     *
     * @param v The vein to test against this one.
     * @return True if the two veins overlap, false otherwise.
     */
    public boolean overlaps(Vein v) {
        // Keep in mind that the bounding box is a +1 block buffer around each vein. We therefore need to shrink
        // one of them by 1 or we risk a false positive (where blocks that are 1 block apart are considered overlapping).
        return this.box.overlaps(v.box.clone().expand(-1));
    }

    /**
     * Combines two veins together iff they overlap.
     *
     * @param v The vein to combine with this one.
     * @return The new combined vein iff they overlap, null otherwise.
     */
    public Vein combine(Vein v) {
        if (type.equals(v.type) && overlaps(v)) {
            this.count += v.count;
            this.distance = Math.min(this.distance, v.distance);
            this.box = this.box.union(v.box);
            return this;
        }
        return null;
    }

    /**
     * Finds a vein from the set that the given block should belong to and adds it to the vein.
     *
     * @param veins The set of veins to check.
     * @param block The block.
     * @return An Optional containing the vein that the given block was added to, otherwise the Optional will be empty.
     */
    public static final Optional<Vein> addToVein(final Set<Vein> veins, final Block block) {
        return veins.stream().filter(v -> v.addBlock(block)).findFirst();
    }

    public static final Set<Vein> addBlock(final Player player, final Set<Vein> veins, final Block block) {
        Optional<Vein> found = addToVein(veins, block);
        if (!found.isPresent()) {
            // Create a new vein and initialize it with this block.
            veins.add(new Vein(block, player.getLocation().distance(block.getLocation())));
        }
        return veins;
    }

    /**
     * Iterates through the second set of veins and for each vein, combines it with an existing vein in the first set if
     * there is an overlap, otherwise adds the vein to the first set.
     *
     * @param vs1 The first set of veins.
     * @param vs2 The second set of veins.
     * @return The combined set of veins.
     */
    public static final Set<Vein> combine(final Set<Vein> vs1, final Set<Vein> vs2) {
        for (Vein v2 : vs2) {
            Optional<Vein> found = vs1.stream()
                    .map(v1 -> v1.combine(v2))
                    .filter(v -> v != null)
                    .findFirst();
            if (!found.isPresent()) {
                // Add v2 to the first set
                vs1.add(v2);
            }
        }
        return vs1;
    }

    @Override
    public int compareTo(Vein v) {
        return Double.compare(this.distance, v.distance);
    }

    @Override
    public String toString() {
        // Example: diamond_ore * 4: (-13, 12, 72)
        return type.name().toLowerCase() + " * " + this.count + ": " + formatLocation(this.location);
    }
}
