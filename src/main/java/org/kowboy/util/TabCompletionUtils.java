package org.kowboy.util;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Utilities for tab completion quality of life improvements.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class TabCompletionUtils {
    public static final Predicate<String> partialMatch(String[] args) {
        return candidate -> args.length >= 1 &&
                (args[0].isEmpty() || candidate.toLowerCase().startsWith(args[0].toLowerCase()));
    }

    public static final TabCompleter stopCompletion() {
        return new TabCompleter() {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
                return Collections.emptyList();
            }
        };
    }
}
