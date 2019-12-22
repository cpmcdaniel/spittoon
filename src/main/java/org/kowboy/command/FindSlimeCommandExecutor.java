package org.kowboy.command;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.util.BukkitUtils;
import org.kowboy.util.NumberUtils;

import java.util.stream.Stream;

import static org.kowboy.util.BukkitUtils.chunkSpiral;
import static org.kowboy.util.ChatUtils.sendError;
import static org.kowboy.util.ChatUtils.sendSuccess;

/**
 * Finds nearby slime chunks.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class FindSlimeCommandExecutor extends AbstractCommandExecutor {
    private static final int DEFAULT_APOTHEM = 8;
    private static final int TOP_N = 8;

    public FindSlimeCommandExecutor(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            displaySlimeChunks(player, DEFAULT_APOTHEM);
        } else if ("help".equalsIgnoreCase(args[0])) {
            help(player, true);
        } else if (NumberUtils.isInteger(args[0])) {
            displaySlimeChunks(player, Integer.parseInt(args[0]));
        } else {
            sendError(player,"Invalid integer param: " + args[0]);
            help(player, false);
        }
        return true;
    }

    private void displaySlimeChunks(Player player, int apothem) {
        sendSuccess(player,"---- SLIME CHUNKS ----");
        findSlimeChunks(player, apothem)
                .limit(TOP_N)
                .map(BukkitUtils::formatLocation)
                .forEach(s -> sendSuccess(player, s));
    }

    Stream<Chunk> findSlimeChunks(Player player, int apothem) {
        World world = player.getWorld();
        return chunkSpiral(player, apothem)
                // I think this is going to force the chunk to generate if it hasn't already.
                .map(chunkPoint -> world.getChunkAt(chunkPoint[0], chunkPoint[1]))
                .filter(Chunk::isSlimeChunk);
    }

    @Override
    protected String getDescription() {
        return "Finds nearby slime chunks.";
    }

    @Override
    protected String getUsage() {
        return "slime [apothem]";
    }
}
