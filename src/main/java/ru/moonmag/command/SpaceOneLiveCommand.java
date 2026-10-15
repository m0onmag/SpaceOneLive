package ru.moonmag.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ru.moonmag.SpaceOneLive;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SpaceOneLiveCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "spaceonelive.admin";

    private final SpaceOneLive plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            plugin.getMessageConfig().send(sender, "nopermission");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getMessageConfig().reload();
            plugin.getMessageConfig().send(sender, "reload");
            return true;
        }
        plugin.getMessageConfig().send(sender, "usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(PERMISSION)
                && "reload".startsWith(args[0].toLowerCase())) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
