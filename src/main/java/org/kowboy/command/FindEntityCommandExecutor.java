package org.kowboy.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.kowboy.util.BukkitUtils.formatLocation;
import static org.kowboy.util.BukkitUtils.getEntityType;
import static org.kowboy.util.ChatUtils.sendError;
import static org.kowboy.util.ChatUtils.sendSuccess;
import static org.kowboy.util.TabCompletionUtils.partialMatch;

/**
 * Finds all of a given LivingEntity type currently in the world, sorts them by distance from the player, and
 * shows the coordinates of the top-N closest mobs of that type. The default for N is 8.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class FindEntityCommandExecutor extends AbstractCommandExecutor implements TabCompleter {
    private static final byte TOP_N = 8;

    public FindEntityCommandExecutor(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            help(sender, true);
            return true;
        }

        EntityType entityType = getEntityType(args[0]);
        if (null == entityType) {
            sendError(sender, "Invalid entity type: " + args[0]);
            help(sender, false);
            return true;
        }

        Player player = (Player) sender;
        Location playerLoc = player.getLocation();
        sendSuccess(sender,"---- ENTITIES FOUND ----");
        player.getWorld().getLivingEntities().stream()
                .filter(le -> entityType.equals(le.getType()))
                .map(le -> new EntityDistance(le, playerLoc.distance(le.getLocation())))
                .sorted()
                .limit(TOP_N)
                .map(EntityDistance::getEntity)
                .map(le -> toString(le))
                .forEach(s -> sendSuccess(player, s));
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            if (null != getEntityType(args[0])) return Collections.emptyList();

            return Arrays.stream(EntityType.values())
                    .filter(EntityType::isAlive)
                    .map(EntityType::name)
                    .map(String::toLowerCase)
                    .filter(partialMatch(args))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    class EntityDistance implements Comparable<EntityDistance> {
        private final LivingEntity le;
        private final double distance;

        EntityDistance(LivingEntity le, double distance) {
            this.le = le;
            this.distance = distance;
        }

        LivingEntity getEntity() { return this.le; }

        @Override
        public int compareTo(EntityDistance ed) {
            return Double.compare(this.distance, ed.distance);
        }
    }

    private String toString(LivingEntity le) {
        return le.getType().toString().toLowerCase() + " - " + formatLocation(le.getLocation());
    }

    @Override
    protected String getDescription() {
        return "Find nearby mobs of the given type.";
    }

    @Override
    protected String getUsage() {
        return "entity <type>";
    }
}
