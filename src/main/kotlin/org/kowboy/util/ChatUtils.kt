package org.kowboy.util

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender

/**
 * Utility functions for sending messages to players and the console.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
object ChatUtils {
    fun sendError(sender: CommandSender, msg: String) {
        sender.sendMessage(ChatColor.DARK_RED.toString() + msg)
    }

    fun sendInfo(sender: CommandSender, msg: String) {
        sender.sendMessage(ChatColor.WHITE.toString() + msg)
    }

    fun sendSuccess(sender: CommandSender, msg: String) {
        sender.sendMessage(ChatColor.GOLD.toString() + msg)
    }

    fun sendDebug(sender: CommandSender, msg: String) {
        sender.sendMessage(ChatColor.AQUA.toString() + msg)
    }

    fun sendHelp(sender: CommandSender, msg: String) {
        sender.sendMessage(ChatColor.GRAY.toString() + msg)
    }

    fun notPlayerError(sender: CommandSender) {
        sendError(sender, "You must be a player to use this command!")
    }

    fun permissionError(sender: CommandSender) {
        sendError(sender, "You don't have permission to do that!")
    }

    fun broadcast(msg: String) {
        for (player in Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.YELLOW.toString() + msg)
        }
    }

    fun getColor(m: Material?): ChatColor {
        return when (m) {
            Material.COAL_ORE -> ChatColor.DARK_GRAY
            Material.GRASS, Material.DIRT -> ChatColor.DARK_GREEN
            Material.EMERALD_ORE -> ChatColor.GREEN
            Material.REDSTONE_ORE -> ChatColor.DARK_RED
            Material.AIR -> ChatColor.DARK_AQUA
            Material.CLAY -> ChatColor.DARK_PURPLE
            Material.WATER, Material.LAPIS_ORE -> ChatColor.BLUE
            Material.NETHER_QUARTZ_ORE, Material.IRON_ORE -> ChatColor.WHITE
            Material.GOLD_ORE -> ChatColor.GOLD
            Material.DIAMOND_ORE -> ChatColor.AQUA
            Material.GLOWSTONE, Material.SAND, Material.SANDSTONE -> ChatColor.YELLOW
            Material.LAVA -> ChatColor.RED
            else -> ChatColor.GRAY
        }
    }
}