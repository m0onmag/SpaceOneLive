package ru.moonmag.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import ru.moonmag.SpaceOneLive;
import ru.moonmag.utils.Hex;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessageConfig {

    private final SpaceOneLive plugin;
    private volatile Map<String, String> cache = Collections.emptyMap();

    public MessageConfig(SpaceOneLive plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("messages");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                map.put(key, Hex.color(section.getString(key, "")));
            }
        }
        this.cache = map;
    }

    public String get(String path) {
        String msg = cache.get(path);
        return msg == null ? "" : msg;
    }

    public void send(CommandSender target, String path) {
        String msg = get(path);
        if (!msg.isEmpty()) {
            target.sendMessage(msg);
        }
    }
}
