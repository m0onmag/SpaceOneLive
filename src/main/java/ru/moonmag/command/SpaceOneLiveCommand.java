package ru.moonmag.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.moonmag.SpaceOneLive;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        if (args.length == 3 && args[0].equalsIgnoreCase("animate")
                && (args[1].equalsIgnoreCase("fast") || args[1].equalsIgnoreCase("slow"))) {
            Player player = Bukkit.getPlayer(args[2]);
            if (player == null) {
                plugin.getMessageConfig().send(sender, "player-not-found");
                return true;
            }
            plugin.getLifeService().playAnimation(player, args[1].toLowerCase());
            return true;
        }
        plugin.getMessageConfig().send(sender, "usage");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return java.util.stream.Stream.of("reload", "animate")
                    .filter(value -> value.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("animate")) {
            return java.util.stream.Stream.of("fast", "slow")
                    .filter(value -> value.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("animate")
                && (args[1].equalsIgnoreCase("fast") || args[1].equalsIgnoreCase("slow"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
