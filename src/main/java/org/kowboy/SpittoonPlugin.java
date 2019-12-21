package org.kowboy;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.command.FindCommandExecutor;
import org.kowboy.command.SpitCommandExecutor;
import org.kowboy.listener.LightLevelListener;

/**
 * <p>
 *     A minecraft server plugin which implements a random assortment of arguably useful features. This collection of
 *     junk is fondly referred to as "Spittoon" (a receptacle for tobacco spit...yum).
 * </p>
 *
 * <p>
 *     Current features implemented:
 *     <dl>
 *         <dt>light-level</dt>
 *         <dd>
 *             When enabled, displays indicators of light-level on the top face of blocks near the player.
 *             Useful for lighting up an area to prevent mob spawning. See {@link LightLevelListener} for more detail.
 *         </dd>
 *         <dt>find</dt>
 *         <dd>
 *             Find nearby blocks, entities, and slime chunks. See {@link FindCommandExecutor} for more detail.
 *         </dd>
 *         <dt>journal</dt>
 *         <dd>
 *             Keep a player journal with teleport bookmarks. See {@link JournalCommandExecutor} for more detail.
 *         </dd>
 *     </dl>
 * </p>
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class SpittoonPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Be sure to write out plugins/Spittoon/config.yaml
        // Does not overwrite if it already exists.
        saveDefaultConfig();

        // Commands
        addCommands();

        // Listeners
        addListeners();
    }

    private void addListeners() {
        new LightLevelListener(this);
    }

    /**
     * Convenience callback for event listeners to register themselves in their constructor.
     * @param listener the listener to register events for.
     */
    public void registerEvents(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    private void addCommands() {
        new SpitCommandExecutor(this);
    }

    @Override
    public void onDisable() {
        saveConfig();
        super.onDisable();
    }
}
