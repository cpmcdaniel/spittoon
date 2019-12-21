package org.kowboy.command;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.kowboy.util.BukkitUtils.*;
import static org.kowboy.util.ChatUtils.*;
import static org.kowboy.util.TabCompletionUtils.partialMatch;

/**
 * Finds the nearest chunk with the given biome and print the chunk coordinates.
 *
 * <p>This is not an exhaustive search and likely to miss small biomes, as biome designation is store per block since
 * minecraft 1.15. That's right, it's not per chunk or even per (x, z) coordinate - it's in 3D!</p>
 *
 * <p>Obviously an exhaustive search, even in 2 dimensions, would be very expensive and cause major server lag. As a
 * result, the algorithm will hold the y-value constant (sea level) and sample a few blocks from each chunk. There are
 * also range limits to consider and unexplored chunks will have to be generated. Both of these issues have a big
 * impact on performance. As such, both of these things are configurable (biome-search-apothem and
 * search-ungenerated-chunks.</p>
 *
 * TODO: Implement config for search-ungenerated-chunks and optional parameter for search apothem.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
final class FindBiomeCommandExecutor extends AbstractCommandExecutor implements TabCompleter {
    private static final int DEFAULT_SEARCH_APOTHEM = 8;
    private final JavaPlugin plugin;

    FindBiomeCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Shouldn't happen, but bail out just in case...
        if (!(sender instanceof Player)) return true;

        if (args.length < 1) {
            help(sender, true);
            return true;
        }

        final Biome biome = getBiome(args[0]);
        if (null == biome) {
            sendError(sender,"Invalid biome: " + args[0]);
            help(sender, false);
            return true;
        }

        // Start with chunk at player location
        final Player player = (Player) sender;
        final World world = player.getWorld();
        final int seaLevel = world.getSeaLevel();

        new BukkitRunnable() {
            @Override
            public void run() {
                Optional<int[]> searchResult = findNearestBlock(player, 1024,
                        point -> biome.equals(world.getBiome(point[0], seaLevel, point[1])));
                if (searchResult.isPresent()) {
                    int[] blockPoint = searchResult.get();
                    sendSuccess(sender, "Nearest " + biome.toString().toLowerCase() + ": " +
                            formatLocation(blockPoint[0], blockPoint[1]));
                } else {
                    sendInfo(sender, "Not found");
                }
            }
        }.runTaskLater(plugin, 10);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (null != getBiome(args[0])) return Collections.emptyList();

            return Arrays.stream(Biome.values())
                    .map(Biome::name)
                    .map(String::toLowerCase)
                    .filter(partialMatch(args))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected String getDescription() {
        return "Finds the nearest chunk with the given biome and prints it's coordinates";
    }

    @Override
    protected String getUsage() {
        return "biome <biome-type>";
    }
}
