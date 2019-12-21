package org.kowboy.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.util.Vein;
import org.kowboy.util.NumberUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.kowboy.util.BukkitUtils.*;
import static org.kowboy.util.ChatUtils.sendError;
import static org.kowboy.util.ChatUtils.sendSuccess;
import static org.kowboy.util.TabCompletionUtils.partialMatch;

/**
 * Locates veins of blocks of a specified type nearby and displays summary information to the player (location, how
 * many blocks in the vein, etc).
 *
 * @author Craig McDaniel
 * @since 1.0
 */
final class FindBlockCommandExecutor extends AbstractCommandExecutor implements TabCompleter {
    private static final int DEFAULT_APOTHEM = 0; // only searches the Player's chunk
    private static final int MAX_APOTHEM = 2;
    private static final int CHUNK_BLOCK_LIMIT = 1000;
    private static final int TOP_N = 5; // only show 5 closest veins.

    private final JavaPlugin plugin;

    FindBlockCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Shouldn't happen, but bail out just in case...
        if (!(sender instanceof Player)) return true;

        final Player player = (Player) sender;

        if (args.length >= 1) {
            final Material mat = getMaterial(args[0]);
            if (null == mat) {
                sendError(sender, "Invalid block material: " + args[0] );
                help(sender, false);
            } else if (args.length >= 2) {
                if (NumberUtils.isBetween(args[1], 0, MAX_APOTHEM)) {
                    final int apothem = Integer.parseInt(args[1]);
                    displayVeinSummary(player, mat, apothem);
                } else {
                    sendError(sender, "Apothem must be between 0 and " + MAX_APOTHEM + ".");
                    help(sender, false);
                }
            } else {
                // no apothem param, use default.
                displayVeinSummary(player, mat);
            }
        } else {
            help(sender, true);
        }

        return true;
    }

    public static void displayVeinSummary(final Player player, final Material mat) {
        displayVeinSummary(player, mat, DEFAULT_APOTHEM);
    }

    public static void displayVeinSummary(final Player player, final Material mat, final int apothem) {
        sendSuccess(player, "---- VEINS FOUND ----");
        findVeins(player, mat, apothem)
                .limit(TOP_N)
                .map(Vein::toString)
                .forEach(s -> sendSuccess(player, s));
    }

    public static Stream<Vein> findVeins(final Player player, final Material mat, final int apothem) {
        return chunkSpiral(player, apothem)
                .flatMap(chunkPoint -> chunkBlocks(player.getWorld(), chunkPoint[0], chunkPoint[1], mat))
                // We are only going to show the top 5 closes veins, so even with a large apothem, we probably don't
                // need a huge number of blocks. Also, the next step is an aggregate operation, so we need to realize
                // all of the stream values. This avoids realizing way more than we need.
                .limit(CHUNK_BLOCK_LIMIT)
                .collect(TreeSet<Vein>::new,
                        (veins, block) -> Vein.addBlock(player, veins, block),
                        Vein::combine)
                .stream();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1) {
            if (null != getMaterial(args[0])) return Collections.emptyList();

            return Arrays.stream(Material.values())
                    .filter(Material::isBlock)
                    .map(Material::name)
                    .filter(partialMatch(args))
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected String getDescription() {
        return "Finds nearby veins of blocks of the given type";
    }

    @Override
    protected String getUsage() {
        return "block <block-type> [apothem]";
    }
}
