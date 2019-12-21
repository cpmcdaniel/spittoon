package org.kowboy.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.kowboy.task.LightLevelTask;
import org.kowboy.SpittoonPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * <p>When a player sneaks, this listener will schedule a task to show light levels on blocks within a configured apothem.
 * If such a task is already running for the given {@link org.bukkit.entity.Player}, the event handler does nothing.
 * This prevents spawning many concurrent tasks for the same Player that would all be doing the same thing anyway.</p>
 *
 * <p>This handler does nothing if the "light-level.on" config setting is <code>false</code>.</p>
 *
 * <p>Additionally, the task is only scheduled if the player is also holding a torch in either the main hand
 * or offhand.</p>
 *
 * @author  Craig McDaniel
 * @since   1.0
 * @see LightLevelTask
 */
public final class LightLevelListener implements Listener {
    private static final byte DEFAULT_APOTHEM = 8;
    private final JavaPlugin plugin;

    // Keeps track of which players have an active light level task.
    // This is used to prevent spawning additional tasks on every move event.
    private final Set<UUID> locks = new HashSet<>();

    public LightLevelListener(SpittoonPlugin plugin) {
        plugin.registerEvents(this);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (null == player) return;

        // Do nothing if the feature is not enabled/
        if (!plugin.getConfig().getBoolean("light-level.on", false)) return;

        // Do nothing if there is already a task for this player
        if (locks.contains(player.getUniqueId())) return;

        // Only create the task if the player is sneaking...
        if (!event.isSneaking()) return;

        // ...and holding a torch
        if (!isHoldingTorch(player)) return;

        scheduleTask(player);
    }

    private boolean isHoldingTorch(Player player) {
        Material mainHand = player.getInventory().getItemInMainHand().getType();
        Material offHand = player.getInventory().getItemInOffHand().getType();
        return Material.TORCH.equals(mainHand) || Material.TORCH.equals(offHand);
    }

    private void scheduleTask(Player player) {
        if (lock(player)) {
            int apothem = plugin.getConfig().getInt("light-level.apothem", DEFAULT_APOTHEM);
            BukkitRunnable task = new LightLevelTask(apothem, player, () -> release(player));
            task.runTaskTimer(plugin, 10, 10);
        } else {
            // Theoretically, this should never happen. I believe event handlers are all processed in a
            // single thread, so there should be no lock contention.
            plugin.getLogger().warning("Player " + player.getName() + " already has a lock for light-level task");
        }
    }

    private boolean lock(Player p) {
        synchronized (this) {
            if (locks.contains(p.getUniqueId())) return false;
            locks.add(p.getUniqueId());
            return true;
        }
    }

    private void release(Player p) {
        synchronized (this) {
            if (locks.contains(p.getUniqueId())) {
                locks.remove(p.getUniqueId());
            }
        }
    }
}
