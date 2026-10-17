package ru.moonmag.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.moonmag.SpaceOneLive;
import ru.moonmag.config.MessageConfig;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class LifeService {

    private static final String BYPASS = "spaceonelive.bypass";
    private static final String INSERT =
            "INSERT OR IGNORE INTO dead_players (uuid, name, died_at) VALUES (?, ?, ?)";

    private final SpaceOneLive plugin;
    private final MessageConfig messages;
    private final Set<UUID> dead = ConcurrentHashMap.newKeySet();
    private final Set<UUID> animating = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SpaceOneLive-DB");
        thread.setDaemon(true);
        return thread;
    });

    private Connection connection;
    private PreparedStatement insert;

    public LifeService(SpaceOneLive plugin, MessageConfig messages) throws SQLException {
        this.plugin = plugin;
        this.messages = messages;
        try {
            init();
            loadAll();
        } catch (SQLException | RuntimeException e) {
            close();
            throw e;
        }
    }

    private void init() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
        }
        File db = new File(plugin.getDataFolder(), "data.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dead_players ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "name TEXT, "
                    + "died_at INTEGER NOT NULL)");
        }
        insert = connection.prepareStatement(INSERT);
    }

    private void loadAll() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid FROM dead_players")) {
            while (rs.next()) {
                try {
                    dead.add(UUID.fromString(rs.getString(1)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public boolean isDead(UUID uuid) {
        return dead.contains(uuid);
    }

    public boolean isAnimating(UUID uuid) {
        return animating.contains(uuid);
    }

    public String deathMessage() {
        return messages.get("death");
    }

    public void handleDeath(Player player) {
        if (player.hasPermission(BYPASS)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (dead.add(uuid)) {
            long now = System.currentTimeMillis();
            try {
                executor.execute(() -> {
                    try {
                        insert.setString(1, uuid.toString());
                        insert.setString(2, name);
                        insert.setLong(3, now);
                        insert.executeUpdate();
                    } catch (SQLException e) {
                    }
                });
            } catch (RejectedExecutionException e) {
            }
        }

        playAnimation(player, ThreadLocalRandom.current().nextBoolean() ? "fast" : "slow", true);
    }

    public void playAnimation(Player player, String type) {
        playAnimation(player, type, false);
    }

    private void playAnimation(Player player, String type, boolean kick) {
        String path = "death-animation.types." + type;
        int duration = plugin.getConfig().getInt(path + ".duration");
        double height = plugin.getConfig().getDouble(path + ".height");
        boolean allowFlight = player.getAllowFlight();
        animating.add(player.getUniqueId());
        if (plugin.getConfig().getBoolean("death-animation.allow-flight")) {
            player.setAllowFlight(true);
        }
        playSound(player.getLocation(), "death-animation.effects.ascent.sound");

        new BukkitRunnable() {
            private int ticks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    animating.remove(player.getUniqueId());
                    cancel();
                    return;
                }
                if (ticks >= duration) {
                    Location location = player.getLocation().add(0, 1, 0);
                    spawnParticle(location, "death-animation.effects.explosion.main");
                    spawnParticle(location, "death-animation.effects.explosion.flame");
                    spawnParticle(location, "death-animation.effects.explosion.portal");
                    playSound(location, "death-animation.effects.explosion.sound");
                    if (kick) {
                        player.setVelocity(new Vector(0, 0, 0));
                        player.setFlying(true);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            animating.remove(player.getUniqueId());
                            if (player.isOnline()) {
                                player.kickPlayer(deathMessage());
                            }
                        }, plugin.getConfig().getLong("death-animation.explosion-delay"));
                    } else {
                        player.setAllowFlight(allowFlight);
                        animating.remove(player.getUniqueId());
                    }
                    cancel();
                    return;
                }
                player.setVelocity(new Vector(0, height / duration, 0));
                Location location = player.getLocation().add(0, 1, 0);
                spawnParticle(location, "death-animation.effects.ascent.flame");
                spawnParticle(location, "death-animation.effects.ascent.portal");
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnParticle(Location location, String path) {
        location.getWorld().spawnParticle(
                Particle.valueOf(plugin.getConfig().getString(path + ".type")),
                location,
                plugin.getConfig().getInt(path + ".count"),
                plugin.getConfig().getDouble(path + ".offset-x"),
                plugin.getConfig().getDouble(path + ".offset-y"),
                plugin.getConfig().getDouble(path + ".offset-z"),
                plugin.getConfig().getDouble(path + ".speed"));
    }

    private void playSound(Location location, String path) {
        location.getWorld().playSound(
                location,
                Sound.valueOf(plugin.getConfig().getString(path + ".type")),
                (float) plugin.getConfig().getDouble(path + ".volume"),
                (float) plugin.getConfig().getDouble(path + ".pitch"));
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (insert != null) {
                insert.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
