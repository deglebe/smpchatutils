package com.deglebe.smpchatutils.persistence;

import com.deglebe.smpchatutils.persistence.sqlite.PluginSqlite;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* namecolors table in sqlite */
final class SqliteNameColorBackend implements NameColorBackend {

    private static final String SQL_SELECT_ALL =
        "SELECT uuid, prefix FROM namecolors";

    private static final String SQL_UPSERT =
        "INSERT INTO namecolors (uuid, prefix) VALUES (?, ?) "
            + "ON CONFLICT(uuid) DO UPDATE SET prefix = excluded.prefix";

    private static final String SQL_DELETE =
        "DELETE FROM namecolors WHERE uuid = ?";

    private final JavaPlugin plugin;
    private final PluginSqlite sqlite;
    private final ConcurrentHashMap<UUID, String> cache = new ConcurrentHashMap<>();

    SqliteNameColorBackend(JavaPlugin plugin, PluginSqlite sqlite) {
        this.plugin = plugin;
        this.sqlite = sqlite;
    }

    @Override
    public void load() {
        cache.clear();
        try {
            Connection c = sqlite.connection();
            try (
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(SQL_SELECT_ALL)
            ) {
                while (rs.next()) {
                    try {
                        UUID id = UUID.fromString(rs.getString("uuid"));
                        String p = rs.getString("prefix");
                        if (p != null && !p.isEmpty()) {
                            cache.put(id, p);
                        }
                    } catch (IllegalArgumentException ignored) {
                        // skip bad row
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load name colors from SQLite: " + e.getMessage());
        }
    }

    @Override
    public String getPrefix(UUID uuid) {
        return cache.get(uuid);
    }

    @Override
    public void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            cache.remove(uuid);
            deleteRow(uuid);
            return;
        }
        cache.put(uuid, prefix);
        upsert(uuid, prefix);
    }

    @Override
    public void clear(UUID uuid) {
        cache.remove(uuid);
        deleteRow(uuid);
    }

    private synchronized void upsert(UUID uuid, String prefix) {
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(SQL_UPSERT)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, prefix);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not save name color (SQLite): " + e.getMessage());
        }
    }

    private synchronized void deleteRow(UUID uuid) {
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(SQL_DELETE)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not clear name color (SQLite): " + e.getMessage());
        }
    }

    @Override
    public void close() {
        cache.clear();
    }
}
