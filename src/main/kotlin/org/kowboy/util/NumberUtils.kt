package org.kowboy.util

/**
 * Utility functions for strings representing numbers. Because Java hates data.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
object NumberUtils {
    fun isInteger(s: String?): Boolean {
        if (s == null) return false
        try {
            s.toInt()
        } catch (e: NumberFormatException) {
            return false
        }
        return true
    }

    /**
     * Inclusive range check for a given String argument.
     *
     * @param arg The string to parse.
     * @param min The minimum of the range check (inclusive).
     * @param max The maximum of the range check (inclusive).
     * @return `true` if the argument is an integer string and is between `min` and
     * `max`, otherwise `false`.
     */
    fun isBetween(arg: String?, min: Int, max: Int): Boolean {
        if (arg == null) return false
        try {
            val i = arg.toInt()
            return i in min..max
        } catch (e: NumberFormatException) {
        }
        return false
    }
}