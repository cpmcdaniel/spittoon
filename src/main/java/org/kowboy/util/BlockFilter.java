package org.kowboy.util;

import org.bukkit.ChunkSnapshot;

@FunctionalInterface
public interface BlockFilter {
    public boolean test(ChunkSnapshot cs, int x, int y, int z);
}
