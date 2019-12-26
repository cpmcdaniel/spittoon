package org.kowboy.util

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.bukkit.ChatColor
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.kowboy.test.TestUtils.assertBlockPointSeqs
import org.kowboy.test.TestUtils.assertChunkPointSeqs
import org.kowboy.util.BukkitUtils.BLOCKS_PER_CHUNK
import org.kowboy.util.BukkitUtils.chunkSpiral
import org.kowboy.util.BukkitUtils.findNearestBlock
import org.kowboy.util.BukkitUtils.findNearestChunk
import org.kowboy.util.BukkitUtils.sampleFourBlocks
import org.kowboy.util.BukkitUtils.walk

class BukkitUtilsTest {
    @MockK
    lateinit var world: World
    @MockK
    lateinit var player: Player
    @MockK
    lateinit var playerLocation: Location
    @MockK
    lateinit var playerChunk: Chunk

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { player.world } returns world
        every { player.location } returns playerLocation
        every { world.getChunkAt(playerLocation) } returns playerChunk
        every { world.seaLevel } returns SEA_LEVEL
    }

    internal inner class LocationTestCase(x: Double,
                                          y: Double,
                                          z: Double,
                                          private var expected: String,
                                          private var fractionDigits: Int) {
        private var loc = Location(null, x, y, z)
        fun assertLocationString() {
            val actual = loc.formatPoint(fractionDigits)
            Assert.assertEquals(expected, ChatColor.stripColor(actual))
        }

    }

    @Test
    fun testLocationString() {
        val testCases = listOf(
                LocationTestCase(1.0, 1.0, 1.0, "(1, 1, 1)", 0),
                LocationTestCase(1000.556, 1000.0, 0.1, "(1000.6, 1000.0, 0.1)", 1),
                LocationTestCase(1000.556, 1000.0, 0.1, "(1000.56, 1000.0, 0.1)", 2)
        )
        testCases.forEach { it.assertLocationString() }
    }

    @Test
    fun testWalkCenter() {
        val result = walk(Direction.NORTH, ChunkPoint(1, 1), 0)
        assertFalse("Sequence should be empty!", result.any());
    }

    @Test
    fun testWalkRing2North() {
        val expected = sequenceOf(
                ChunkPoint(-1, 4),
                ChunkPoint(-1, 3),
                ChunkPoint(-1, 2),
                ChunkPoint(-1, 1))
        assertChunkPointSeqs(expected, walk(Direction.NORTH, ChunkPoint(1, 2), 2))
    }

    @Test
    fun testWalkRing2South() {
        val expected = sequenceOf(
                ChunkPoint(3, 0),
                ChunkPoint(3, 1),
                ChunkPoint(3, 2),
                ChunkPoint(3, 3))
        assertChunkPointSeqs(expected, walk(Direction.SOUTH, ChunkPoint(1, 2), 2))
    }

    @Test
    fun testWalkRing2East() {
        val expected = sequenceOf(
                ChunkPoint(-1, 0),
                ChunkPoint(0, 0),
                ChunkPoint(1, 0),
                ChunkPoint(2, 0))
        assertChunkPointSeqs(expected, walk(Direction.EAST, ChunkPoint(1, 2), 2));
    }

    @Test
    fun testWalkRing2West() {
        val expected = sequenceOf(
                ChunkPoint(3, 4),
                ChunkPoint(2, 4),
                ChunkPoint(1, 4),
                ChunkPoint(0, 4))
        assertChunkPointSeqs(expected, walk(Direction.WEST, ChunkPoint(1, 2), 2));
    }

    @Test
    fun testChunkSpiral1Ring() {
        val expected = sequenceOf(
                // Center
                ChunkPoint(1,2),
                // North Walk
                ChunkPoint(0, 3),
                ChunkPoint(0, 2),
                // East walk
                ChunkPoint(0, 1),
                ChunkPoint(1, 1),
                // South walk
                ChunkPoint(2, 1),
                ChunkPoint(2, 2),
                // West walk
                ChunkPoint(2, 3),
                ChunkPoint(1, 3))
        assertChunkPointSeqs(expected, chunkSpiral(ChunkPoint(1, 2), 1));
    }

    @Test
    fun testSampleFourBlocks() {
        val expected = sequenceOf(
                BlockPoint(16, 32),
                BlockPoint(24, 32),
                BlockPoint(24, 40),
                BlockPoint(16, 40));
        assertBlockPointSeqs(expected, sampleFourBlocks(ChunkPoint(1, 2)));
    }

    @Test
    fun testFindNearestBlock() {
        every { world.isChunkGenerated(any(), any()) } returns true
        every { playerChunk.x } returns 1
        every { playerChunk.z } returns 2
        every { playerLocation.blockX } returns 1 * BLOCKS_PER_CHUNK
        every { playerLocation.blockZ } returns 2 * BLOCKS_PER_CHUNK
        assertNull(findNearestBlock(player, 3) { false });

        val found = findNearestBlock(player, 3) { (x, z) -> x ==24 && z == 32 }
        assertNotNull(found)
        assertEquals(BlockPoint(24, 32), found)
    }

    @Test
    fun testFindNearestChunk() {
        every { world.isChunkGenerated(any(), any()) } returns true
        every { playerChunk.x } returns 1
        every { playerChunk.z } returns 2
        every { playerLocation.blockX } returns 1 * BLOCKS_PER_CHUNK
        every { playerLocation.blockZ } returns 2 * BLOCKS_PER_CHUNK
        assertNull(findNearestChunk(player, 3) { _ -> false});

        val nearest = findNearestChunk(player, 3) { (x, z) -> x == 0 && z == 3 }
        assertNotNull(nearest)
        assertEquals(ChunkPoint(0, 3), nearest)
    }

    companion object {
        const val SEA_LEVEL = 62
    }
}