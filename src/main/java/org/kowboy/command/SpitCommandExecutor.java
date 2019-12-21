package org.kowboy.command;

import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.command.CompositeCommandExecutor;
import org.kowboy.command.FindCommandExecutor;
import org.kowboy.command.LightLevelCommandExecutor;

public final class SpitCommandExecutor extends CompositeCommandExecutor {
    public SpitCommandExecutor(JavaPlugin plugin) {
        super("spit", plugin.getCommand("spit").getDescription());
        plugin.getCommand("spit").setExecutor(this);
        plugin.getCommand("spit").setTabCompleter(this);

        addSubCommand("light-level", new LightLevelCommandExecutor(plugin));
        addSubCommand("find", new FindCommandExecutor(plugin));
    }
}
