package org.kowboy.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility functions for sending messages to players and the console.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public final class ChatUtils {
    public static void sendError(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.DARK_RED + msg);
    }

    public static void sendInfo(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.WHITE + msg);
    }

    public static void sendSuccess(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.GOLD + msg);
    }

    public static void sendDebug(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.AQUA + msg);
    }

    public static void sendHelp(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.GRAY + msg);
    }

    public static void notPlayerError(CommandSender sender) {
        sendError(sender, "You must be a player to use this command!");
    }

    public static void permissionError(CommandSender sender) {
        sendError(sender, "You don't have permission to do that!");
    }

    public static void broadcast(String msg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.YELLOW + msg);
        }
    }
}
