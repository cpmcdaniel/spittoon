package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.ChatUtils.sendError
import org.kowboy.util.NumberUtils.isInteger
import org.kowboy.util.TabCompletionUtils.stopCompletion

/**
 * A command executor for turning the light-level feature on/off and configuring the apothem of effect.
 *
 * @author Craig McDaniel
 * @since 1.0
 * @see LightLevelListener
 * @see LightLevelTask
 */
class LightLevelCommandExecutor(plugin: JavaPlugin) : CompositeCommandExecutor("light-level",
        "Shows block light levels when the player is holding a torch and sneaking.") {
    private inner class Apothem(plugin: JavaPlugin) : AbstractCommandExecutor(plugin) {
        override val description: String
            get() = "Sets the apothem for the light-level area of effect."
        override val usage: String
            get() = "apothem <int>"

        override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
            when {
                args.isEmpty() -> {
                    help(sender, true)
                }
                isInteger(args[0]) -> {
                    val apothem = args[0].toInt()
                    plugin.config["light-level.apothem"] = apothem
                }
                else -> {
                    sendError(sender, "Apothem must be an integer")
                    help(sender, false)
                }
            }
            return true
        }
    }

    companion object {
        private const val PERMISSION = "spittoon.light-level.configure"
    }

    init {
        val on = CommandExecutor { _, _, _, _ ->
            plugin.config["light-level.on"] = true
            true
        }
        addSubCommand("on", SenderPermissionFilter(on, PERMISSION, false),
                stopCompletion())
        val off = CommandExecutor { _, _, _, _ ->
            plugin.config["light-level.on"] = false
            true
        }
        addSubCommand("off", SenderPermissionFilter(off, PERMISSION, false),
                stopCompletion())
        addSubCommand("apothem", SenderPermissionFilter(Apothem(plugin), PERMISSION, false),
                stopCompletion())
    }
}