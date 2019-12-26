package org.kowboy.command

import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.kowboy.util.ChatUtils.sendHelp

abstract class AbstractCommandExecutor protected constructor(val plugin: JavaPlugin) : CommandExecutor {
    protected abstract val description: String
    protected abstract val usage: String
    protected fun help(sender: CommandSender, withDescription: Boolean) {
        if (withDescription) {
            sendHelp(sender, "Description: $description")
        }
        sendHelp(sender, "Usage: $usage")
    }

}