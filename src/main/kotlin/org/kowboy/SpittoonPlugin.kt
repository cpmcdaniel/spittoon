package org.kowboy

import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.command.SpitCommandExecutor
import org.kowboy.listener.LightLevelListener

/**
 *
 *
 * A minecraft server plugin which implements a random assortment of arguably useful features. This collection of
 * junk is fondly referred to as "Spittoon" (a receptacle for tobacco spit...yum).
 *
 *
 *
 *
 * Current features implemented:
 * <dl>
 * <dt>light-level</dt>
 * <dd>
 * When enabled, displays indicators of light-level on the top face of blocks near the player.
 * Useful for lighting up an area to prevent mob spawning. See [LightLevelListener] for more detail.
</dd> *
 * <dt>find</dt>
 * <dd>
 * Find nearby blocks, entities, and slime chunks. See [FindCommandExecutor] for more detail.
</dd> *
 * <dt>journal</dt>
 * <dd>
 * Keep a player journal with teleport bookmarks. See [JournalCommandExecutor] for more detail.
</dd> *
</dl> *
 *
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class SpittoonPlugin : JavaPlugin() {
    override fun onEnable() {
        // Be sure to write out plugins/Spittoon/config.yaml
        // Does not overwrite if it already exists.
        saveDefaultConfig()
        // Commands
        addCommands()
        // Listeners
        addListeners()
    }

    private fun addListeners() {
        LightLevelListener(this)
    }

    /**
     * Convenience callback for event listeners to register themselves in their constructor.
     * @param listener the listener to register events for.
     */
    fun registerEvents(listener: Listener) {
        server.pluginManager.registerEvents(listener, this)
    }

    private fun addCommands() {
        SpitCommandExecutor(this)
    }

    override fun onDisable() {
        saveConfig()
        super.onDisable()
    }
}