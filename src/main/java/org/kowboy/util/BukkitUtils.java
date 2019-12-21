package org.kowboy.util;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.kowboy.util.ChatUtils.sendDebug;

/**
 * Utility methods for messing with Bukkit objects.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class BukkitUtils {
    public static final byte BLOCKS_PER_CHUNK = 16;
    public static final int MAX_CHUNK_APOTHEM = 1024; // subject to change
    public static final int MAX_BLOCK_APOTHEM = MAX_CHUNK_APOTHEM * BLOCKS_PER_CHUNK;

    public static final int[] point(int x, int z) {
        return new int[] {x, z};
    }

    /**
     * Formats a location for display.
     *
     * @param loc The location to format
     * @param fractionDigits How many digits to display to the right of the decimal point. A value of 0 will display
     *                       each coordinate as an integer.
     * @return The formatted String. Example: <em>(1.0, -1.4, 2.0)</em>
     */
    public static final String formatLocation(Location loc, int fractionDigits) {
        if (fractionDigits == 0) {
            return formatLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
        return formatLocation(loc.getX(), loc.getY(), loc.getZ(), fractionDigits);
    }

    public static final String formatLocation(Location loc) {
        return formatLocation(loc, 0);
    }

    public static final String formatLocation(int x, int y, int z) {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    public static final String formatLocation(int[] coords) {
        if (coords.length == 3) return formatLocation(coords[0], coords[1], coords[2]);
        else if (coords.length == 2) return formatLocation(coords[0], coords[1]);
        else throw new IllegalArgumentException("Invalid length for coordinate array: " + coords.length);
    }

    public static final String formatLocation(double x, double y, double z, int fractionDigits) {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance();
        fmt.applyPattern("###0.0##");
        fmt.setMaximumFractionDigits(fractionDigits);

        return "(" + fmt.format(x) + ", " + fmt.format(y) + ", " + fmt.format(z) + ")";
    }

    public static final String formatLocation(int x, int z) {
        return "(" + x + ", " + z + ")";
    }

    public static final String formatLocation(Chunk chunk) {
        return formatLocation(chunk.getX(), chunk.getZ());
    }

    /**
     * Converts a String value to the appropriate Biome enum. Swallows {@link IllegalArgumentException} execption.
     *
     * @param s The string to convert.
     * @return The Biome enum value that matches the input, otherwise null.
     */
    public static final Biome getBiome(String s) {
        if (null != s) {
            try {
                return Biome.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return null;
    }

    /**
     * Converts a String value to the appropriate EntityType enum. Swallows {@link IllegalArgumentException} execption.
     *
     * @param s The string to convert.
     * @return The EntityType enum value that matches the input, otherwise null.
     */
    public static final EntityType getEntityType(String s) {
        if (null != s) {
            try {
                return EntityType.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }
        return null;
    }

    /**
     * Converts a String value to the appropriate Material enum. Swallows {@link IllegalArgumentException} execption.
     *
     * @param s The string to convert.
     * @return The Material enum value that matches the input, otherwise null.
     */
    public static final Material getMaterial(String s) {
        return Material.getMaterial(s.toUpperCase());
    }

    /**
     * Finds the (x, z) coordinates of the nearest chunk that matches the given predicate. Does not actually load
     * chunk objects, as this could force generation/loading of chunks. Subject to limitations for the sake of
     * performance.
     *
     * @param player The search is centered around the player.
     * @param r The maximum number of concentric squares (or "rings") to search.
     * @param pred The predicate for finding a match. Stops as soon as it finds a match.
     * @return The (x, z) chunk coordinates of the nearest match to the Player.
     */
    public static final Optional<int[]> findNearestChunk(final Player player, final int r, final Predicate<int[]> pred) {
        return chunkSpiral(player, r)
                .takeWhile(chunkPoint -> player.getWorld().isChunkGenerated(chunkPoint[0], chunkPoint[1]))
                .filter(pred)
                .findFirst();
    }

    /**
     * Finds the (x, z) coordinates of the nearest block that matches the given predicate. Does not actually load
     * block objects, as this could force generation/loading of chunks. Subject to limitations for the sake of
     * performance.
     *
     * @param player The search is centered around the player.
     * @param r The maximum number of concentric squares (or "rings") to search.
     * @param pred The predicate for finding a match. Stops as soon as it finds a match.
     * @return The (x, z) block coordinates of the nearest match to the Player.
     */
    public static final Optional<int[]> findNearestBlock(final Player player, final int r, final Predicate<int[]> pred) {
        return chunkSpiral(player, r)
                // Limiting to generated chunks only may or may not be desirable?
                .takeWhile(chunkPoint -> player.getWorld().isChunkGenerated(chunkPoint[0], chunkPoint[1]))
                .flatMap(BukkitUtils::sampleFourBlocks)
                .filter(pred)
                .findFirst();
    }

    /**
     * We will spiral outward by chunk, starting at the player's location. The spiral will move clockwise around
     * the compass, starting with the first move to the adjacent chunk Southwest of the center, then walking North to
     * the Northest corner. At this point we are walking the first square "ring" around the center chunk in a clockwise
     * direction. We then repeat by moving to the Southwest corner of the next ring of chunks.
     *
     * @param p The player used to center the spiral.
     * @param rings The maximum numer of spiral "rings".
     * @return A Stream of Chunk coordinates (x, z) in spiral order.
     */
    public static final Stream<int[]> chunkSpiral(final Player p, final int rings) {
        // It may be easier to think of this as concentric squares, where the player chunk will be the very center
        // 1x1 square.
        World world = p.getWorld();

        // The center of the spiral is really the very center block of the chunk, but it helps if you think of chunks as
        // points on a piece of graph paper with the center chunk being the origin of the coordinate system for the
        // spiral.
        //
        // Note, the following algorithm uses chunk (not block) coordinates exclusively.
        Chunk center = world.getChunkAt(p.getLocation());
        final int centerX = center.getX();
        final int centerZ = center.getZ();

        return chunkSpiral(centerX, centerZ, rings);
    }

    public static final Stream<int[]> chunkSpiral(final int centerX, final int centerZ, final int rings) {
        // Constrain the apothem to something reasonable
        int apothem = Math.min(rings, MAX_CHUNK_APOTHEM);

        return Stream.concat(Stream.of(new int[] {centerX, centerZ}),  // Start with just the center chunk
                IntStream.rangeClosed(0, apothem).boxed()
                        .flatMap(r -> Arrays.stream(Direction.values()).flatMap(
                                // This gives us a direction to walk for the current ring. Order is: N, E, S, W.
                                direction -> walk(direction, centerX, centerZ, r))));
    }

    /**
     * Returns a stream of chunk coordinates by walking in the given direction along the given ring number.
     *
     * @param d The direction to walk.
     * @param centerX The X coordinate of the center chunk.
     * @param centerZ  The Z coordinate of the center chunk.
     * @param r The number of rings from center.
     * @return A Stream of chunk coordinates for one side of the ring.
     */
    public static final Stream<int[]> walk(Direction d, int centerX, int centerZ, int r) {
        // Constrain the apothem to something reasonable.
        int apothem = Math.min(r, MAX_BLOCK_APOTHEM);
        if (apothem == 0) {
            return Stream.of(); // we don't walk ring 0.
        }

        // Consider the first ring (r=1). The North walk starts at the SE corner chunk of the ring and stops just
        // shy of the NW corner chunk (because it will be the start of the East walk). The size of the first ring
        // square is 3x3, so the length of the walk is 2. Thus, for any r, the walk length is 2r.
        return IntStream.range(0, (2 * apothem)).mapToObj(
                step -> new int[] {
                        centerX + d.offsetX(apothem) + d.stepX(step),
                        centerZ + d.offsetZ(apothem) + d.stepZ(step)
                }
        );
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
     * @param point The (x, z) chunk coordinates.
     * @return A stream of (x, z) block coordinates.
     */
    public static final Stream<int[]> sampleFourBlocks(int[] point) {
        // Sample the NW corner of each quadrant in the chunk.
        // First block is (0, 0) within the chunk (NW corner). All other samples will be offset from this.
        int originX = point[0] * BLOCKS_PER_CHUNK;
        int originZ = point[1] * BLOCKS_PER_CHUNK;
        int[] originBlock =  new int[] {originX, originZ};

        return Stream.of(
                // First sample (0, 0). NW quadrant.
                originBlock,

                // Second sample (8, 0). NE quadrant.
                new int[] {originX + 8, originZ},

                // Third sample (8, 8). SE quadrant.
                new int[] {originX + 8, originZ + 8},

                // Fourth sample (0, 8). SW quadrant.
                new int[] {originX, originZ + 8});
    }

    /**
     * Creates a Stream of all the blocks in the given chunk matching the given block type.
     *
     * @param chunk The chunk to stream blocks from.
     * @param blockType The Material used to filter blocks through the stream.
     * @return A stream of blocks matching the given material.
     */
    public static final Stream<Block> chunkBlocks(Chunk chunk, Material blockType) {
        return chunkBlocks(chunk, (cs, x, y, z) -> blockType.equals(cs.getBlockType(x, y, z)));
    }

    public static final Stream<Block> chunkBlocks(World world, int x, int z, Material blockType) {
        return chunkBlocks(world.getChunkAt(x, z), blockType);
    }

    /**
     * Creates a Stream of all the blocks in the given chunk matching the given block type.
     *
     * @param chunk The chunk to stream blocks from.
     * @param blockFilter The filter to use for selecting blocks from the chunk.
     * @return A stream of blocks matching the given filter.
     */
    public static final Stream<Block> chunkBlocks(Chunk chunk, BlockFilter blockFilter) {
        // Take a snapshot. Do not include biome data.
        final ChunkSnapshot cs = chunk.getChunkSnapshot(true, false, false);
        final BiFunction<Integer, Integer, Integer> highestBlockY = highestBlockYFn(chunk.getWorld(), cs);

        return IntStream.range(0, 16).boxed().flatMap(
                x -> IntStream.range(0, 16).boxed().flatMap(
                        z -> IntStream.range(1, highestBlockY.apply(x, z)).boxed()
                                .filter(y -> blockFilter.test(cs, x, y, z))
                                .map(y -> chunk.getBlock(x, y, z))));
    }

    public static final Stream<Block> chunkBlocks(World world, int x, int z, BlockFilter blockFilter) {
        return chunkBlocks(world.getChunkAt(x, z), blockFilter);
    }

    private static BiFunction<Integer, Integer, Integer> highestBlockYFn(World world, ChunkSnapshot cs) {
        if (world.getEnvironment().equals(World.Environment.NORMAL)) {
            return (x, z) -> cs.getHighestBlockYAt(x, z);
        }
        return (x, z) -> 127;
    }

    /**
     * Sends a formatted message to the sender using the subject and then returning the subject to the caller.
     *
     * @param sender The command sender (either admin or player).
     * @param subject The object to format as string.
     * @param format A transform function that takes the subject and returns a formatted string. The function should
     *               not mutate the state of the subject in any way, though there is no way to enforce this.
     * @return The subject parameter, unaltered.
     */
    public final static <T> T spy(CommandSender sender, T subject, Function<T, String> format) {
        sendDebug(sender, format.apply(subject));
        return subject;
    }
}
