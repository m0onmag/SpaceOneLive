package ru.moonmag;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import ru.moonmag.command.SpaceOneLiveCommand;
import ru.moonmag.config.MessageConfig;
import ru.moonmag.listener.PlayerListener;
import ru.moonmag.service.LifeService;
import ru.moonmag.utils.Hex;
import java.sql.SQLException;

@Getter
public final class SpaceOneLive extends JavaPlugin {

    private static final String FRAME = "&#ff7c00";
    private static final String LINK = "https://t.me/spacestudiomc";

    @Getter
    private static SpaceOneLive instance;

    private MessageConfig messageConfig;
    private LifeService lifeService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.messageConfig = new MessageConfig(this);

        try {
            this.lifeService = new LifeService(this, messageConfig);
        } catch (SQLException | RuntimeException e) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(this, lifeService, messageConfig), this);

        PluginCommand command = getCommand("spaceonelive");
        if (command != null) {
            SpaceOneLiveCommand executor = new SpaceOneLiveCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        banner("Запуск плагина...", "&#00ff17Плагин запустился!");
        new ru.moonmag.utils.Metrics(this, 34698);
        new ru.moonmag.utils.UpdateChecker(this).checkForUpdates();
    }

    @Override
    public void onDisable() {
        if (lifeService != null) {
            lifeService.close();
            lifeService = null;
        }
        instance = null;

        banner("Отключение плагина...", "&#ff0000Плагин отключился!");
    }

    private void banner(String title, String status) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        console.sendMessage(Hex.color(FRAME + "╔"));
        console.sendMessage(Hex.color(FRAME + "║ &f" + title));
        console.sendMessage(Hex.color(FRAME + "║ " + status + " &fКодер: &#ff6e00m0onmag"));
        console.sendMessage(Hex.color(FRAME + "║ ") + Hex.gradient(LINK, "#00f5ff", "#00d9ff"));
        console.sendMessage(Hex.color(FRAME + "╚"));
    }
}
