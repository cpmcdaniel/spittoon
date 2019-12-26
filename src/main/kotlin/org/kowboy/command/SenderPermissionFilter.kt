package org.kowboy.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.kowboy.util.ChatUtils.notPlayerError
import org.kowboy.util.ChatUtils.permissionError

/**
 * A simple wrapper around a CommandExecutor that will call the wrapped executor only
 * if the sender has the specified permission. If playerOnly is true, it will also verify
 * that the sender is a Player instance.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
class SenderPermissionFilter @JvmOverloads
constructor(private val wrapped: CommandExecutor,
            private val permission: String?,
            private val playerOnly: Boolean = true) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (playerOnly && sender !is Player) {
            notPlayerError(sender)
        } else if (permission == null || sender.hasPermission(permission)) {
            wrapped.onCommand(sender, command, label, args)
        } else {
            permissionError(sender)
        }
        return true
    }
}