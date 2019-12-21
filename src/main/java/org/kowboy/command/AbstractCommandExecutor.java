package org.kowboy.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static org.kowboy.util.ChatUtils.sendHelp;

public abstract class AbstractCommandExecutor implements CommandExecutor {
    protected abstract String getDescription();
    protected abstract String getUsage();

    protected void help(CommandSender sender, boolean withDescription) {
        if (withDescription) {
            sendHelp(sender, "Description: " + getDescription());
        }
        sendHelp(sender, "Usage: " + getUsage());
    }
}
