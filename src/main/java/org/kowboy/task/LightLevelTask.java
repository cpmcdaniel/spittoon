package org.kowboy.task;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.stream.IntStream;

/**
 * <p>
 *     Displays visual indicators of light-level in a apothem around the player. Uses colored particle effects to
 *     indicate light level according to the following table.
 * </p>
 * <table>
 *     <thead>
 *         <th>Light Level</th><th>Color</th>
 *     </thead>
 *     <tbody>
 *         <tr><td>12+</td><td>Bright Green</td></tr>
 *         <tr><td>10,11</td><td>Green</td></tr>
 *         <tr><td>8,9</td><td>Yellow</td></tr>
 *         <tr><td>0-7</td><td>Dark Red*</td></tr>
 *     </tbody>
 * </table>
 *
 * <p>
 *     *The dark red color indicates that mobs can spawn on that block.
 * </p>
 *
 * <p>
 *     This task is repeated as long as the player is online and sneaking. If either of these conditions is false, the
 *     task will cancel itself.
 * </p>
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public class LightLevelTask extends BukkitRunnable {
    private final Player player;
    private Runnable onCancel;
    private int apothem;

    public LightLevelTask(int apothem, Player player, Runnable onCancel) {
        this.player = player;
        this.onCancel = onCancel;
        this.apothem = apothem;
    }

    @Override
    public void run() {
        if (player.isOnline() && player.isSneaking()) {
            double playerYaw = player.getLocation().getYaw();

            IntStream.range(-apothem, apothem).forEach(
                    x -> IntStream.range(-apothem, apothem).forEach(
                            z -> IntStream.range(-apothem, 3).forEach(
                                    y -> process(x, y, z)
                            )
                    )
            );

        } else {
            cancel();
        }
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        onCancel.run();
        super.cancel();
    }

    private void process(int x, int y, int z) {
        Location blockLocation = player.getLocation().add(x, y, z);
        Block block = blockLocation.getBlock();

        // We only care about solid blocks that have Water or Air above.
        if (!block.getType().isSolid()) return;
        switch (block.getRelative(BlockFace.UP).getType()) {
            case AIR:
            case WATER:
                // Get the location of the center of the top face of the block
                Location particleLoc = block.getLocation().add(0.5, 1, 0.5);
                // getLightFromBlocks ignores light from the sun
                byte lightLevel = particleLoc.getBlock().getLightFromBlocks();
                Color color = getColor(lightLevel);
                Particle.DustOptions data = new Particle.DustOptions(color, 1.0f);
                player.spawnParticle(Particle.REDSTONE, particleLoc, 3, data);
                break;
            default:
                break;
        }
    }

    public static Color getColor(byte lightLevel) {
        if (lightLevel >= 12) return Color.LIME;
        if (lightLevel >= 10) return Color.GREEN;
        if (lightLevel >= 8) return Color.YELLOW;
        return Color.MAROON;
    }
}
