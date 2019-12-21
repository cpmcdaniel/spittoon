package org.kowboy.test;

import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.kowboy.util.BukkitUtils.formatLocation;

public class TestUtils {    // This assertion is not very informative when it fails, but it works.
    public static final void assertPointStreamEquals(Stream<int[]> expected, Stream<int[]> actual) {
        Iterator<int[]> ite = expected.iterator(), ita = actual.iterator();
        int count = 0;
        while(ite.hasNext() && ita.hasNext()) {
            int[] e = ite.next();
            int[] a = ita.next();
            assertArrayEquals("Expected: " + formatLocation(e) + ", Actual: " + formatLocation(a),
                    e, a);
            count++;
        }
        assertTrue("Actual has fewer points than expected. Expected more than " + count, !ite.hasNext());
        assertTrue("Actual has more points than expected. Expected " + count, !ita.hasNext());
    }

}
