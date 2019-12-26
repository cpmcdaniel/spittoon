package org.kowboy.util

import org.bukkit.command.TabCompleter
import java.util.function.Predicate

/**
 * Utilities for tab completion quality of life improvements.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
object TabCompletionUtils {
    fun partialMatch(args: Array<String>): (String) -> Boolean {
        return { candidate: String ->
            args.isNotEmpty() &&
                    (args[0].isEmpty() || candidate.toLowerCase().startsWith(args[0].toLowerCase()))
        }
    }

    private var stopInstance: TabCompleter? = null
    fun stopCompletion(): TabCompleter? {
        if (stopInstance == null) {
            stopInstance = TabCompleter { _, _, _, _ -> emptyList() }
        }
        return stopInstance
    }
}