package org.kowboy.util

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.kowboy.test.MockBlock

class VeinTest {
    @MockK lateinit var player: Player
    @MockK lateinit var world: World

    private lateinit var blockLocation: Location
    private lateinit var playerLocation: Location
    private lateinit var block: Block
    private lateinit var adjacentBlock: Block
    private lateinit var waterBlock: Block
    private lateinit var farBlock: Block

    private lateinit var vein: Vein

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        blockLocation = Location(world, 25.0, 11.0, 17.0)
        playerLocation = Location(world, 1.0, 62.0, 1.0)
        block = MockBlock(blockLocation.clone(), Material.DIAMOND_ORE)
        adjacentBlock = MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE)
        waterBlock = MockBlock(blockLocation.clone().add(0.0, 1.0, 0.0), Material.WATER)
        farBlock = MockBlock(blockLocation.clone().add(22.0, 0.0, 0.0), Material.DIAMOND_ORE)
        every { player.location } returns playerLocation
        every { player.world } returns world

        vein = Vein(block, player)
    }

    @Test
    fun testVeinConstructor() {
        assertEquals(blockLocation, vein.location)
        assertEquals(1, vein.count)
        assertEquals(Material.DIAMOND_ORE, vein.type)
    }

    @Test
    fun testContains() {
        vein.addBlock(adjacentBlock)
        vein.addBlock(waterBlock)
        vein.addBlock(farBlock)
        assertTrue(vein.contains(block))
        assertTrue(vein.contains(adjacentBlock))
        assertFalse(vein.contains(waterBlock))
        assertFalse(vein.contains(farBlock))
        assertEquals(2, vein.count)
    }

    @Test
    fun testAddBlock() {
        // Block of same type that is just outside the bounding box
        val justOutside: Block = MockBlock(blockLocation.clone().add(2.0, 0.0, 0.0), Material.DIAMOND_ORE)
        assertFalse(vein.addBlock(justOutside))
        assertEquals(1, vein.count)
        // Adjacent block of same type.
        assertTrue(vein.addBlock(adjacentBlock))
        assertEquals(2, vein.count)
        // Try adding that first block again. Should work this time because the bounding box just expanded.
        assertTrue(vein.addBlock(justOutside))
        assertEquals(3, vein.count)
        // Block of different type is within the bounding box.
        assertFalse(vein.addBlock(waterBlock))
        assertEquals(3, vein.count)
    }

    @Test
    fun testOverlaps() {
        // First vein
        vein.addBlock(MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE))
        // Second vein, overlaps
        val v2 = Vein(MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE), player)
        assertTrue(vein.overlaps(v2))
        assertTrue(v2.overlaps(vein))
        // Third vein, just outside bounding box.
        val v3 = Vein(MockBlock(blockLocation.clone().add(6.0, 2.0, 3.0), Material.DIAMOND_ORE), player)
        assertFalse(vein.overlaps(v3))
        assertFalse(v3.overlaps(vein))
    }

    @Test
    fun testBoundingBox() {
        val bb1 = BoundingBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        bb1.expand(1.0)
        val bb2 = BoundingBox(1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
        assertTrue(bb1.contains(bb2))
        assertTrue(bb1.overlaps(bb2))
        bb2.expand(1.0)
        assertTrue(bb1.overlaps(bb2))
    }

    @Test
    fun testCombine() { // First vein
        vein.addBlock(MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE))
        // Second vein, overlaps
        val v2 = Vein(MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE), player)
        val combined = vein.combine(v2)
        assertNotNull(combined)
        assertSame(vein, combined)
        assertEquals(6, vein.count.toLong())
        // Third vein, just outside bounding box.
        val v3 = Vein(MockBlock(blockLocation.clone().add(7.0, 2.0, 3.0), Material.DIAMOND_ORE), player)
        assertNull(vein.combine(v3))
        assertEquals(6, vein.count.toLong())
        // A vein of a different type.
        val v4 = Vein(waterBlock, player)
        assertNull(vein.combine(v4))
        assertEquals(6, vein.count.toLong())
    }

    @Test
    fun testSetAddToVein() { // No veins yet.
        val veins: Set<Vein> = setOf()
        var actual = veins.addToVein(block)
        assertEquals(0, veins.size.toLong())
        assertNull(actual)
        // Manually place the setup vein in the set.
        val veins2 = veins + vein
        // Adjacent block of same type.
        actual = veins2.addToVein(adjacentBlock)
        assertEquals(1, veins2.size)
        assertNotNull(actual)
        assertEquals(2, actual?.count)
    }

    @Test
    fun testSetAddBlock() { // No veins yet.
        val veins: Set<Vein> = setOf()
        var actual: Set<Vein> = veins.addBlock(block, player)
        assertNotSame(veins, actual)
        assertEquals(1, actual.size)
        assertEquals(1, actual.first().count)
        // Adjacent block of same type
        actual = actual.addBlock(adjacentBlock, player)
        assertEquals(1, actual.size)
        assertEquals(2, actual.first().count)
        // Adjacent block of different type
        actual = actual.addBlock(waterBlock, player)
        assertEquals(2, actual.size)
        // Far away block of same type
        actual = actual.addBlock(farBlock, player)
        assertEquals(3, actual.size)
        assertEquals(4, actual.sumBy { it.count })

        assertEquals(0, veins.size)
    }

    @Test
    fun testSetCombine() {
        var veins1 = setOf(vein)
        var veins2 = setOf<Vein>()
        vein.addBlock(MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE))
        vein.addBlock(MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE))
        // This vein will combine with the vein in the first set.
        val v2 = Vein(MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE), player)
        v2.addBlock(MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE))
        v2.addBlock(MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE))
        veins2 = veins2 + v2
        assertEquals(1, veins1.size)
        assertEquals(1, veins2.size)
        // Put a far away vein in the first set (will not combine with any other vein).
        val v3 = Vein(farBlock, player)
        veins1 = veins1 + v3
        assertEquals(2, veins1.size)
        // Put a water vein in the second set (will not combine with any other veins).
        val v4 = Vein(waterBlock, player)
        veins2 = veins2 + v4
        assertEquals(2, veins2.size)
        val actual = veins1.combine(veins2)
        assertEquals(3, actual.size)
    }
}