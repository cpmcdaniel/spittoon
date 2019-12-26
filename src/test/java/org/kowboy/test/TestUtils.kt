package org.kowboy.test

import org.bukkit.ChatColor
import org.junit.Assert.assertEquals
import org.kowboy.util.BlockPoint
import org.kowboy.util.ChunkPoint

object TestUtils {
    @JvmStatic
    fun assertBlockPointSeqs(expected: Sequence<BlockPoint>, actual: Sequence<BlockPoint>) {
        val expectedList = expected.map { ChatColor.stripColor(it.toString()) }.toList()
        val actualList = actual.map { ChatColor.stripColor(it.toString()) }.toList();
        assertEquals(expectedList, actualList)
    }

    @JvmStatic
    fun assertChunkPointSeqs(expected: Sequence<ChunkPoint>, actual: Sequence<ChunkPoint>) {
        val expectedList = expected.map { ChatColor.stripColor(it.toString()) }.toList()
        val actualList = actual.map { ChatColor.stripColor(it.toString()) }.toList();
        assertEquals(expectedList, actualList)
    }
}