package org.kowboy.command

import org.bukkit.plugin.java.JavaPlugin

class SpitCommandExecutor(plugin: JavaPlugin) : CompositeCommandExecutor("spit",
        plugin.getCommand("spit")!!.description) {
    init {
        plugin.getCommand("spit")!!.setExecutor(this)
        plugin.getCommand("spit")!!.tabCompleter = this
        addSubCommand("light-level", LightLevelCommandExecutor(plugin))
        addSubCommand("find", FindCommandExecutor(plugin))
    }
}