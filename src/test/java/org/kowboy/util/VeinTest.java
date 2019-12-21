package org.kowboy.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.junit.Before;
import org.junit.Test;
import org.kowboy.test.MockBlock;
import org.kowboy.util.Vein;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VeinTest {
    @Mock Player player;
    @Mock World world;

    Block block;
    Block adjacent;
    Block waterBlock;
    Block far;
    Location blockLocation;
    Location playerLocation;
    Vein vein;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        playerLocation = new Location(world, 1, 62, 1);
        blockLocation = new Location(world, 25, 11, 17);

        block = new MockBlock(blockLocation.clone(), Material.DIAMOND_ORE);

        // Adjacent block of same type.
        adjacent = new MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE);

        // Adjacent block of different type.
        waterBlock = new MockBlock(blockLocation.clone().add(0.0, 1.0, 0.0), Material.WATER);

        // Far away block of same type
        far = new MockBlock(blockLocation.clone().add(22.0, 0.0, 0.0), Material.DIAMOND_ORE);

        when(player.getLocation()).thenReturn(playerLocation);

        vein = new Vein(block, player);
    }

    @Test
    public void testVeinConstructor() {
        assertEquals(blockLocation, vein.getLocation());
        assertEquals(1, vein.getCount());
        assertEquals(Material.DIAMOND_ORE, vein.getType());
    }

    @Test
    public void testContains() {
        // Identity test
        assertTrue(vein.contains(block));

        assertTrue(vein.contains(adjacent));
        assertFalse(vein.contains(waterBlock));
        assertFalse(vein.contains(far));
    }

    @Test
    public void testAddBlock() {
        // Block of same type that is just outside the bounding box
        Block justOutside = new MockBlock(blockLocation.clone().add(2.0, 0.0, 0.0), Material.DIAMOND_ORE);
        assertFalse(vein.addBlock(justOutside));
        assertEquals(1, vein.getCount());

        // Adjacent block of same type.
        assertTrue(vein.addBlock(adjacent));
        assertEquals(2, vein.getCount());

        // Try adding that first block again. Should work this time because the bounding box just expanded.
        assertTrue(vein.addBlock(justOutside));
        assertEquals(3, vein.getCount());

        // Block of different type is within the bounding box.
        assertFalse(vein.addBlock(waterBlock));
        assertEquals(3, vein.getCount());
    }

    @Test
    public void testOverlaps() {
        // First vein
        vein.addBlock(new MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE));

        // Second vein, overlaps
        Vein v2 = new Vein(new MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE), player);
        assertTrue(vein.overlaps(v2));
        assertTrue(v2.overlaps(vein));

        // Third vein, just outside bounding box.
        Vein v3 = new Vein(new MockBlock(blockLocation.clone().add(6.0, 2.0, 3.0), Material.DIAMOND_ORE), player);
        assertFalse(vein.overlaps(v3));
        assertFalse(v3.overlaps(vein));
    }

    @Test
    public void testBoundingBox() {
        BoundingBox bb1 = new BoundingBox(0, 0, 0, 1, 1, 1);
        bb1.expand(1);

        BoundingBox bb2 = new BoundingBox(1, 1, 1, 1, 1, 1);
        assertTrue(bb1.contains(bb2));

        assertTrue(bb1.overlaps(bb2));
        bb2.expand(1);
        assertTrue(bb1.overlaps(bb2));
    }

    @Test
    public void testCombine() {
        // First vein
        vein.addBlock(new MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE));

        // Second vein, overlaps
        Vein v2 = new Vein(new MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE), player);
        Vein combined = vein.combine(v2);
        assertNotNull(combined);
        assertSame(vein, combined);
        assertEquals(6, vein.getCount());

        // Third vein, just outside bounding box.
        Vein v3 = new Vein(new MockBlock(blockLocation.clone().add(7.0, 2.0, 3.0), Material.DIAMOND_ORE), player);
        assertNull(vein.combine(v3));
        assertEquals(6, vein.getCount());

        // A vein of a different type.
        Vein v4 = new Vein(waterBlock, player);
        assertNull(vein.combine(v4));
        assertEquals(6, vein.getCount());
    }

    @Test
    public void testAddToVein() {
        // No veins yet.
        Set<Vein> veins = new TreeSet<>();
        Optional<Vein> actual = Vein.addToVein(veins, block);
        assertEquals(0, veins.size());
        assertFalse(actual.isPresent());

        // Manually place the setup vein in the set.
        veins.add(vein);

        // Adjacent block of same type.
        actual = Vein.addToVein(veins, adjacent);
        assertEquals(1, veins.size());
        assertTrue(actual.isPresent());
        assertEquals(2, actual.get().getCount());
    }

    @Test
    public void testStaticAddBlock() {
        // No veins yet.
        Set<Vein> veins = new TreeSet<>();
        Set<Vein> actual = Vein.addBlock(player, veins, block);
        assertSame(veins, actual);
        assertEquals(1, veins.size());
        assertEquals(1, veins.iterator().next().getCount());

        // Adjacent block of same type
        actual = Vein.addBlock(player, veins, adjacent);
        assertEquals(1, veins.size());
        assertEquals(2, veins.iterator().next().getCount());

        // Adjacent block of different type
        actual = Vein.addBlock(player, veins, waterBlock);
        assertEquals(2, veins.size());

        // Far away block of same type
        actual = Vein.addBlock(player, veins, far);
        assertEquals(3, veins.size());
    }

    @Test
    public void testStaticCombine() {
        Set<Vein> veins1 = new TreeSet<>();
        Set<Vein> veins2 = new TreeSet<>();

        veins1.add(vein);
        vein.addBlock(new MockBlock(blockLocation.clone().add(1.0, 0.0, 0.0), Material.DIAMOND_ORE));
        vein.addBlock(new MockBlock(blockLocation.clone().add(2.0, 1.0, 1.0), Material.DIAMOND_ORE));

        // This vein will combine with the vein in the first set.
        Vein v2 = new Vein(new MockBlock(blockLocation.clone().add(3.0, 2.0, 2.0), Material.DIAMOND_ORE), player);
        v2.addBlock(new MockBlock(blockLocation.clone().add(4.0, 3.0, 3.0), Material.DIAMOND_ORE));
        v2.addBlock(new MockBlock(blockLocation.clone().add(5.0, 2.0, 3.0), Material.DIAMOND_ORE));
        veins2.add(v2);

        // Put a far away vein in the first set (will not combine with any other vein).
        Vein v3 = new Vein(far, player);
        veins1.add(v3);

        // Put a water vein in the second set (will not combine with any other veins).
        Vein v4 = new Vein(waterBlock, player);
        veins2.add(v4);

        Set<Vein> actual = Vein.combine(veins1, veins2);
        assertSame(actual, veins1);
        assertEquals(3, actual.size());
    }
}
