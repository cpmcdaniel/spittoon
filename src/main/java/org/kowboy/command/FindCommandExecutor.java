package org.kowboy.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static org.kowboy.util.ChatUtils.notPlayerError;
import static org.kowboy.util.TabCompletionUtils.stopCompletion;

/**
 * A command executor for finding blocks, entities, and slime chunks near the player.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class FindCommandExecutor extends CompositeCommandExecutor {
    FindCommandExecutor(JavaPlugin plugin) {
        super("find", "Find nearby blocks, enities, biomes, and slime chunks.");

        addSubCommand("entities",
                new SenderPermissionFilter(new FindEntitiesCommandExecutor(plugin),"spittoon.find.entity"),
                stopCompletion());

        FindEntityCommandExecutor findEntity = new FindEntityCommandExecutor(plugin);
        addSubCommand("entity",
                new SenderPermissionFilter(findEntity, "spittoon.find.entity"),
                findEntity);

        FindBlockCommandExecutor findBlock = new FindBlockCommandExecutor(plugin);
        addSubCommand("block",
                new SenderPermissionFilter(findBlock, "spittoon.find.block"),
                findBlock);

        addSubCommand("blocks",
                new SenderPermissionFilter(new FindBlocksCommandExecutor(plugin), "spittoon.find.block"),
                stopCompletion());

        addSubCommand("slime",
                new SenderPermissionFilter(new FindSlimeCommandExecutor(plugin), "spittoon.find.slime"),
                stopCompletion());

        FindBiomeCommandExecutor findBiome = new FindBiomeCommandExecutor(plugin);
        addSubCommand("biome",
                new SenderPermissionFilter(findBiome, "spittoon.find.biome"),
                findBiome);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            return super.onCommand(sender, command, label, args);
        }
        notPlayerError(sender);
        return true; // don't print usage text
    }
}
