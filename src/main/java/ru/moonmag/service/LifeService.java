package ru.moonmag.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
import java.util.concurrent.TimeUnit;

public class LifeService {

    private static final String BYPASS = "spaceonelive.bypass";
    private static final String INSERT =
            "INSERT OR IGNORE INTO dead_players (uuid, name, died_at) VALUES (?, ?, ?)";

    private final SpaceOneLive plugin;
    private final MessageConfig messages;
    private final Set<UUID> dead = ConcurrentHashMap.newKeySet();
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

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.kickPlayer(deathMessage());
            }
        });
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
