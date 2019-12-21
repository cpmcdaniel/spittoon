package org.kowboy.util;

/**
 * Utility functions for strings representing numbers. Because Java hates data.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class NumberUtils {
    public static final boolean isInteger(String s) {
        if (s == null) return false;

        try {
            int i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Inclusive range check for a given String argument.
     *
     * @param arg The string to parse.
     * @param min The minimum of the range check (inclusive).
     * @param max The maximum of the range check (inclusive).
     * @return <code>true</code> if the argument is an integer string and is between <code>min</code> and
     *          <code>max</code>, otherwise <code>false</code>.
     */
    public static boolean isBetween(String arg, int min, int max) {
        if (arg == null) return false;

        try {
            int i = Integer.parseInt(arg);
            return min <= i && i <= max;
        } catch (NumberFormatException e) {}

        return false;
    }
}
