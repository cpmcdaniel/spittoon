package org.kowboy.util;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.kowboy.util.BukkitUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindBiomeTest {

    @Test
    public void testFindNearest() {
        int seaLevel = 62;
        World w = mock(World.class);
        when(w.getSeaLevel()).thenReturn(seaLevel);
        when(w.getBiome(anyInt(), anyInt(), anyInt())).thenReturn(Biome.DESERT, Biome.JUNGLE);
        when(w.isChunkGenerated(anyInt(), anyInt())).thenReturn(true);

        Player p = mock(Player.class);
        when(p.getWorld()).thenReturn(w);

        Location loc = mock(Location.class);
        when(p.getLocation()).thenReturn(loc);

        Chunk c = mock(Chunk.class);
        when(c.getX()).thenReturn(0);
        when(c.getZ()).thenReturn(0);
        when(w.getChunkAt(loc)).thenReturn(c);

        Optional<int[]> nearest = findNearestBlock(p, 3, point -> Biome.JUNGLE.equals(w.getBiome(point[0], seaLevel, point[1])));
        assertTrue(nearest.isPresent());
        assertArrayEquals(point(8, 0), nearest.get());
    }
}
