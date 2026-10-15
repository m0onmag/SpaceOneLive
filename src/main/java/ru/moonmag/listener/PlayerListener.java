package ru.moonmag.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.moonmag.SpaceOneLive;
import ru.moonmag.config.MessageConfig;
import ru.moonmag.service.LifeService;

@RequiredArgsConstructor
public class PlayerListener implements Listener {

    private final SpaceOneLive plugin;
    private final LifeService lifeService;
    private final MessageConfig messages;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (lifeService.isDead(event.getUniqueId())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, lifeService.deathMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                messages.send(player, "first-time");
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        lifeService.handleDeath(event.getEntity());
    }
}
