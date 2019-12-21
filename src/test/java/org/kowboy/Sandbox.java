package org.kowboy;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Sandbox {

    @Test
    public void sandbox() {
        String[] original = new String[] {"one", "two"};

        String[] shifted = Arrays.copyOfRange(original, 1, original.length);
        assertEquals(1, shifted.length);
        assertEquals("two", shifted[0]);

        shifted = Arrays.copyOfRange(shifted, 1, shifted.length);
        assertEquals(0, shifted.length);
    }
}
