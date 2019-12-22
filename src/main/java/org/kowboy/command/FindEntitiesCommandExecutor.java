package org.kowboy.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.util.BukkitUtils;

import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.kowboy.util.ChatUtils.sendSuccess;

/**
 * Finds all living entities present in the world (except for players) and displays a summary per entity type.
 *
 * <p>The summary format is: <code>entity-type * count: (nearest-coordinates)</code></p>
 *
 * <p>
 *     Example Summary: <em>spider * 12: (-176, 67, 34)</em>
 * </p>
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public class FindEntitiesCommandExecutor implements CommandExecutor {
    private final JavaPlugin plugin;

    public FindEntitiesCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sendSuccess(sender, "---- ENTITIES FOUND ----");

        Player p = (Player) sender;
        Supplier<EntityGroupSummary> supplier = () -> new EntityGroupSummary(p.getLocation());

        p.getWorld().getLivingEntities().stream()

                // don't include players
                .filter(le -> !(le instanceof Player))

                // group entities by type
                .collect(Collectors.groupingBy(Entity::getType,

                        // ...and summarize each homogeneous group
                        Collector.of(supplier,                  // provides initial result container
                                EntityGroupSummary::acc,        // accumulates new values into result container
                                EntityGroupSummary::combine,    // combines 2 result containers (if needed)
                                EntityGroupSummary::toString))) // converts final result to a String for display

                // The result of the reduction above is a Map<EntityType, String> where the values are ready
                // for sending to the player's console.
                .forEach((entityType, summaryString) -> sendSuccess(p, summaryString));

        return true;
    }

    class EntityGroupSummary {
        EntityType type;
        int count = 0;
        Location nearest;

        private double nearestDistance = Double.MAX_VALUE;
        private Location playerLocation;

        EntityGroupSummary(Location playerLocation) {
            this.playerLocation = playerLocation;
        }

        // Accumulate a new entity into this result container
        void acc(LivingEntity le) {
            // We should only be accumulating entities of the same type, so we only set this once.
            if (this.type == null) this.type = le.getType();

            // Increment the entity count
            this.count++;

            // Possibly update the nearest location
            double d = this.playerLocation.distance(le.getLocation());
            if (d < this.nearestDistance) {
                this.nearest = le.getLocation();
                this.nearestDistance = d;
            }
        }

        // Combines two of these objects into one (for folding parallel results).
        EntityGroupSummary combine(EntityGroupSummary summary) {
            this.count = this.count + summary.count;
            if (summary.nearestDistance < this.nearestDistance) {
                this.nearestDistance = summary.nearestDistance;
                this.nearest = summary.nearest;
            }
            return this;
        }

        @Override
        public String toString() {
            // Example:    spider * 6: (-176, 66, 191)
            return this.type.toString().toLowerCase() + " * " +
                    this.count + ": " + BukkitUtils.formatLocation(this.nearest, 0);
        }
    }
}
