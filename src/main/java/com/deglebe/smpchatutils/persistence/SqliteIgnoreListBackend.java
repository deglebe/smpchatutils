package com.deglebe.smpchatutils.persistence;

import com.deglebe.smpchatutils.persistence.sqlite.PluginSqlite;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* rows in player_ignores table in sqlite */
final class SqliteIgnoreListBackend implements IgnoreListBackend {

    private static final String SQL_SELECT_ALL =
        "SELECT ignorer_uuid, ignored_uuid FROM player_ignores";

    private static final String SQL_INSERT =
        "INSERT OR IGNORE INTO player_ignores (ignorer_uuid, ignored_uuid) VALUES (?, ?)";

    private static final String SQL_DELETE =
        "DELETE FROM player_ignores WHERE ignorer_uuid = ? AND ignored_uuid = ?";

    private final JavaPlugin plugin;
    private final PluginSqlite sqlite;
    /* ignorer -> ignored (cached; kept in sync with db on writes) */
    private final ConcurrentHashMap<UUID, Set<UUID>> byIgnorer = new ConcurrentHashMap<>();

    SqliteIgnoreListBackend(JavaPlugin plugin, PluginSqlite sqlite) {
        this.plugin = plugin;
        this.sqlite = sqlite;
    }

    @Override
    public void load() {
        byIgnorer.clear();
        try {
            Connection c = sqlite.connection();
            try (
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_ALL)
            ) {
                while (rs.next()) {
                    try {
                        UUID ignorer = UUID.fromString(rs.getString("ignorer_uuid"));
                        UUID ignored = UUID.fromString(rs.getString("ignored_uuid"));
                        byIgnorer
                            .computeIfAbsent(ignorer, k -> ConcurrentHashMap.newKeySet())
                            .add(ignored);
                    } catch (IllegalArgumentException ignored) {
                        // skip bad row
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load ignore list from SQLite: " + e.getMessage());
        }
    }

    @Override
    public boolean isIgnoring(UUID ignorer, UUID ignored) {
        Set<UUID> set = byIgnorer.get(ignorer);
        return set != null && set.contains(ignored);
    }

    @Override
    public void addIgnore(UUID ignorer, UUID ignored) {
        if (ignorer.equals(ignored)) {
            return;
        }
        byIgnorer.computeIfAbsent(ignorer, k -> ConcurrentHashMap.newKeySet()).add(ignored);
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(SQL_INSERT)) {
                ps.setString(1, ignorer.toString());
                ps.setString(2, ignored.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not save ignore (SQLite): " + e.getMessage());
        }
    }

    @Override
    public void removeIgnore(UUID ignorer, UUID ignored) {
        Set<UUID> set = byIgnorer.get(ignorer);
        if (set != null) {
            set.remove(ignored);
            if (set.isEmpty()) {
                byIgnorer.remove(ignorer);
            }
        }
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
                ps.setString(1, ignorer.toString());
                ps.setString(2, ignored.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not remove ignore (SQLite): " + e.getMessage());
        }
    }

    @Override
    public Set<UUID> getIgnored(UUID ignorer) {
        Set<UUID> set = byIgnorer.get(ignorer);
        if (set == null || set.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(set);
    }

    @Override
    public void close() {
        byIgnorer.clear();
    }
}
