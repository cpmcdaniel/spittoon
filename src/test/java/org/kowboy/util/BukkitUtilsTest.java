package org.kowboy.util;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.kowboy.util.BukkitUtils.*;
import static org.kowboy.test.TestUtils.assertPointStreamEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class BukkitUtilsTest {
    static final int SEA_LEVEL = 62;
    @Mock World world;
    @Mock Player player;
    @Mock Location playerLocation;
    @Mock Chunk playerChunk;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(playerLocation);
        when(world.getChunkAt(playerLocation)).thenReturn(playerChunk);
        when(world.getSeaLevel()).thenReturn(SEA_LEVEL);
    }

    class LocationTestCase {
        String expected;
        Location loc;
        int fractionDigits;

        LocationTestCase(double x,
                         double y,
                         double z,
                         String expected,
                         int fractionDigits) {
            this.expected = expected;
            this.fractionDigits = fractionDigits;
            this.loc = new Location(null, x, y, z);
        }

        void assertLocationString() {
            String actual = BukkitUtils.formatLocation(loc, fractionDigits);
            assertEquals(expected, ChatColor.stripColor(actual));
        }
    }

    @Test
    public void testLocationString() {
        LocationTestCase[] testCases = {
                new LocationTestCase(1.0, 1.0, 1.0, "(1, 1, 1)", 0),
                new LocationTestCase(1000.556, 1000, 0.1, "(1000.6, 1000.0, 0.1)", 1),
                new LocationTestCase(1000.556, 1000, 0.1, "(1000.56, 1000.0, 0.1)", 2)
        };

        Arrays.stream(testCases).forEach(LocationTestCase::assertLocationString);
    }

    @Test
    public void testWalkCenter() {
        Optional<int[]> result = walk(Direction.NORTH, 1, 1, 0).findAny();
        assertFalse("Stream is empty!", result.isPresent());
    }

    @Test
    public void testWalkRing2North() {
        Stream<int[]> expected = Stream.of(
                point(-1, 4),
                point(-1, 3),
                point(-1, 2),
                point(-1, 1));
        assertPointStreamEquals(expected, walk(Direction.NORTH, 1, 2, 2));
    }

    @Test
    public void testWalkRing2South() {
        Stream<int[]> expected = Stream.of(
                point(3, 0),
                point(3, 1),
                point(3, 2),
                point(3, 3));
        assertPointStreamEquals(expected, walk(Direction.SOUTH, 1, 2, 2));
    }

    @Test
    public void testWalkRing2East() {
        Stream<int[]> expected = Stream.of(
                point(-1, 0),
                point(0, 0),
                point(1, 0),
                point(2, 0));
        assertPointStreamEquals(expected, walk(Direction.EAST, 1, 2, 2));
    }

    @Test
    public void testWalkRing2West() {
        Stream<int[]> expected = Stream.of(
                point(3, 4),
                point(2, 4),
                point(1, 4),
                point(0, 4));
        assertPointStreamEquals(expected, walk(Direction.WEST, 1, 2, 2));
    }

    @Test
    public void testChunkSpiral1Ring() {
        Stream<int[]> expected = Stream.of(
                // Center
                point(1,2),
                // North Walk
                point(0, 3),
                point(0, 2),
                // East walk
                point(0, 1),
                point(1, 1),
                // South walk
                point(2, 1),
                point(2, 2),
                // West walk
                point(2, 3),
                point(1, 3));
        assertPointStreamEquals(expected, chunkSpiral(1, 2, 1));
    }


    @Test
    public void testSampleFourBlocks() {
        Stream<int[]> expected = Stream.of(
                point(16, 32),
                point(24, 32),
                point(24, 40),
                point(16, 40));
        assertPointStreamEquals(expected, sampleFourBlocks(point(1, 2)));
    }

    @Test
    public void testFindNearestBlock() {
        when(world.isChunkGenerated(anyInt(), anyInt())).thenReturn(true);
        when(playerChunk.getX()).thenReturn(1);
        when(playerChunk.getZ()).thenReturn(2);

        assertFalse(findNearestBlock(player, 3, point -> false).isPresent());

        Optional<int[]> actual = findNearestBlock(player, 3, point -> point[0] == 24 && point[1] == 32);
        assertTrue(actual.isPresent());
        assertArrayEquals(new int[] {24, 32}, actual.get());
    }

    @Test
    public void testFindNearestChunk() {
        when(world.isChunkGenerated(anyInt(), anyInt())).thenReturn(true);
        when(playerChunk.getX()).thenReturn(1);
        when(playerChunk.getZ()).thenReturn(2);

        assertFalse(findNearestChunk(player, 3, point -> false).isPresent());

        Optional<int[]> actual = findNearestChunk(player, 3, point -> point[0] == 0 && point[1] == 3);
        assertTrue(actual.isPresent());
        assertArrayEquals(new int[] {0, 3}, actual.get());
    }
}
