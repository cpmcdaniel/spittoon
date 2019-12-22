package org.kowboy.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.util.BlockFilter;
import org.kowboy.util.Vein;
import org.kowboy.util.BukkitUtils;
import org.kowboy.util.NumberUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.kowboy.util.BukkitUtils.chunkBlocks;
import static org.kowboy.util.BukkitUtils.chunkSpiral;
import static org.kowboy.util.ChatUtils.sendError;
import static org.kowboy.util.ChatUtils.sendSuccess;

/**
 * Finds all veins for a configured set of block types within an apothem around the player.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
final class FindBlocksCommandExecutor extends AbstractCommandExecutor {
    private static final int DEFAULT_APOTHEM = 0;
    private static final int MAX_APOTHEM = 2;

    FindBlocksCommandExecutor(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Player player = (Player) sender;

        if (args.length >= 1) {
            if ("help".equalsIgnoreCase(args[0])) {
                help(sender, true);
            } else if (NumberUtils.isBetween(args[0], 0, MAX_APOTHEM)) {
                final int apothem = Integer.parseInt(args[0]);
                displayVeinSummary(player, apothem);
            } else {
                sendError(sender, "Apothem must be between 0 and " + MAX_APOTHEM + ".");
                help(sender, false);
            }
        } else {
            // no apothem param, use default
            displayVeinSummary(player);
        }
        return true;
    }

    private void displayVeinSummary(final Player player) {
        displayVeinSummary(player, DEFAULT_APOTHEM);
    }

    private void displayVeinSummary(final Player player, final int apothem) {
        sendSuccess(player, "---- BLOCKS FOUND ----");
        findVeins(player, apothem)
                .collect(Collectors.groupingBy(Vein::getType, TreeMap::new, Collectors.toCollection(TreeSet::new)))
                .values()
                .stream()
                .map(SortedSet::first)
                .map(Vein::toString)
                .forEach(s -> sendSuccess(player, s));
    }

    private Stream<Vein> findVeins(final Player player, final int apothem) {
        return chunkSpiral(player, apothem)
                .flatMap(chunkPoint -> BukkitUtils.chunkBlocks(player.getWorld(), chunkPoint[0], chunkPoint[1], makeBlockFilter()))
                .collect(TreeSet<Vein>::new,
                        (veins, block) -> Vein.addBlock(player, veins, block),
                        Vein::combine)
                .stream();
    }

    private BlockFilter makeBlockFilter() {
        List<String> materials = plugin.getConfig().getStringList("finder.block-filter");
        Set<Material> mset = new HashSet<>();
        for (String m : materials) {
            Material mat = BukkitUtils.getMaterial(m);
            if (mat == null || !mat.isBlock()) {
                plugin.getLogger().severe("Unrecognized block type in finder.block-finder config: " + m);
            } else {
                mset.add(mat);
            }
        }

        return (cs, x, y, z) -> mset.contains(cs.getBlockType(x, y, z));
    }

    @Override
    protected String getDescription() {
        return "Finds mineral veins within an apothem around the player";
    }

    @Override
    protected String getUsage() {
        return "blocks [apothem]";
    }
}
