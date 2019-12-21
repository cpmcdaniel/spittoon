package org.kowboy.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kowboy.listener.LightLevelListener;
import org.kowboy.task.LightLevelTask;
import org.kowboy.util.NumberUtils;

import static org.kowboy.util.ChatUtils.notPlayerError;
import static org.kowboy.util.ChatUtils.sendError;

/**
 * A command executor for turning the light-level feature on/off and configuring the apothem of effect.
 *
 * @author Craig McDaniel
 * @since 1.0
 * @see LightLevelListener
 * @see LightLevelTask
 */
public final class LightLevelCommandExecutor extends CompositeCommandExecutor {

    public LightLevelCommandExecutor(JavaPlugin plugin) {
        super("light-level",
                "Shows block light levels when the player is holding a torch and sneaking.");

        addSubCommand("on", new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                plugin.getConfig().set("light-level.on", true);
                return true;
            }
        });

        addSubCommand("off", new CommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                plugin.getConfig().set("light-level.on", false);
                return true;
            }
        });

        addSubCommand("apothem", new AbstractCommandExecutor() {
            @Override
            protected String getDescription() {
                return "Sets the apothem of the square around the player for light level display.";
            }

            @Override
            protected String getUsage() {
                return "apothem <int>";
            }

            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                if (args.length < 1) {
                    help(sender, true);
                } else if (NumberUtils.isInteger(args[0])) {
                    int apothem = Integer.parseInt(args[0]);
                    plugin.getConfig().set("light-level.apothem", apothem);
                } else {
                    sendError(sender, "Apothem must be an integer");
                    help(sender, false);
                }
                return true;
            }
        });
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            notPlayerError(sender);
            return true; // don't print usage text
        }
        return super.onCommand(sender, command, label, args);
    }
}
