package org.kowboy.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.kowboy.util.ChatUtils.notPlayerError;
import static org.kowboy.util.ChatUtils.permissionError;

/**
 * A simple wrapper around a CommandExecutor that will call the wrapped executor only
 * if the sender has the specified permission. If playerOnly is true, it will also verify
 * that the sender is a Player instance.
 *
 * @author Craig McDaniel
 * @since 1.0
 */
public class SenderPermissionFilter implements CommandExecutor {
    private final String permission;
    private final CommandExecutor wrapped;
    private final boolean playerOnly;

    public SenderPermissionFilter(CommandExecutor wrapped, String permission, boolean playerOnly) {
        this.permission = permission;
        this.wrapped = wrapped;
        this.playerOnly = playerOnly;
    }

    public SenderPermissionFilter(CommandExecutor wrapped, String permission) {
        this(wrapped, permission, true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (playerOnly && !(sender instanceof Player)) {
            notPlayerError(sender);
        } else if (permission == null || sender.hasPermission(permission)) {
            wrapped.onCommand(sender, command, label, args);
        } else {
            permissionError(sender);
        }
        return true;
    }
}
