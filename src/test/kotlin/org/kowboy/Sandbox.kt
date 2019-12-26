package org.kowboy

import org.junit.Assert
import org.junit.Test
import java.util.*

class Sandbox {
    @Test
    fun sandbox() {
        val original = arrayOf("one", "two")
        var shifted = Arrays.copyOfRange(original, 1, original.size)
        Assert.assertEquals(1, shifted.size.toLong())
        Assert.assertEquals("two", shifted[0])
        shifted = Arrays.copyOfRange(shifted, 1, shifted.size)
        Assert.assertEquals(0, shifted.size.toLong())
    }
}