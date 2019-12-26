package org.kowboy.util

import io.mockk.every
import io.mockk.mockk
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.kowboy.util.BukkitUtils.findNearestBlock

class FindBiomeTest {
    @Test
    fun testFindNearest() {
        val seaLevel = 62
        val w = mockk<World>(relaxed = true)
        every { w.seaLevel } returns seaLevel
        every { w.getBiome(any(), seaLevel, any()) } returns Biome.DESERT andThen Biome.JUNGLE
        every { w.isChunkGenerated(any(), any()) } returns true
        val p = mockk<Player>(relaxed = true)
        every { p.world } returns w
        val loc = mockk<Location>(relaxed = true)
        every { p.location } returns loc
        val c = mockk<Chunk>()
        every { c.x } returns 0
        every { c.z } returns 0
        val nearest = findNearestBlock(p, 3) { (x, z) -> Biome.JUNGLE == w.getBiome(x, seaLevel, z) };
        assertNotNull(nearest);
        assertEquals(BlockPoint(8, 0), nearest!!);
    }
}