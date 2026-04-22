package com.deglebe.smpchatutils.persistence;

import com.deglebe.smpchatutils.persistence.sqlite.PluginSqlite;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Name metadata tables in sqlite: namecolors + player_chat_prefixes + player_chat_suffixes. */
final class SqliteNameColorBackend implements NameColorBackend {

    private static final String SQL_SELECT_FORMAT_ALL =
        "SELECT uuid, prefix FROM namecolors";

    private static final String SQL_UPSERT_FORMAT =
        "INSERT INTO namecolors (uuid, prefix) VALUES (?, ?) "
            + "ON CONFLICT(uuid) DO UPDATE SET prefix = excluded.prefix";

    private static final String SQL_DELETE_FORMAT =
        "DELETE FROM namecolors WHERE uuid = ?";

    private static final String SQL_SELECT_CHAT_PREFIX_ALL =
        "SELECT uuid, prefix FROM player_chat_prefixes";

    private static final String SQL_UPSERT_CHAT_PREFIX =
        "INSERT INTO player_chat_prefixes (uuid, prefix) VALUES (?, ?) "
            + "ON CONFLICT(uuid) DO UPDATE SET prefix = excluded.prefix";

    private static final String SQL_DELETE_CHAT_PREFIX =
        "DELETE FROM player_chat_prefixes WHERE uuid = ?";

    private static final String SQL_SELECT_CHAT_SUFFIX_ALL =
        "SELECT uuid, suffix FROM player_chat_suffixes";

    private static final String SQL_UPSERT_CHAT_SUFFIX =
        "INSERT INTO player_chat_suffixes (uuid, suffix) VALUES (?, ?) "
            + "ON CONFLICT(uuid) DO UPDATE SET suffix = excluded.suffix";

    private static final String SQL_DELETE_CHAT_SUFFIX =
        "DELETE FROM player_chat_suffixes WHERE uuid = ?";

    private final JavaPlugin plugin;
    private final PluginSqlite sqlite;

    private final ConcurrentHashMap<UUID, String> nameFormats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> chatPrefixes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> chatSuffixes = new ConcurrentHashMap<>();

    SqliteNameColorBackend(JavaPlugin plugin, PluginSqlite sqlite) {
        this.plugin = plugin;
        this.sqlite = sqlite;
    }

    @Override
    public void load() {
        nameFormats.clear();
        chatPrefixes.clear();
        chatSuffixes.clear();
        try {
            Connection c = sqlite.connection();
            loadMap(c, SQL_SELECT_FORMAT_ALL, "prefix", nameFormats);
            loadMap(c, SQL_SELECT_CHAT_PREFIX_ALL, "prefix", chatPrefixes);
            loadMap(c, SQL_SELECT_CHAT_SUFFIX_ALL, "suffix", chatSuffixes);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load name metadata from SQLite: " + e.getMessage());
        }
    }

    private static void loadMap(
        Connection c,
        String query,
        String valueColumn,
        Map<UUID, String> out
    ) throws SQLException {
        try (
            Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(query)
        ) {
            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString("uuid"));
                    String value = rs.getString(valueColumn);
                    if (value != null && !value.isEmpty()) {
                        out.put(id, value);
                    }
                } catch (IllegalArgumentException ignored) {
                    // skip malformed uuid row
                }
            }
        }
    }

    @Override
    public String getPrefix(UUID uuid) {
        return nameFormats.get(uuid);
    }

    @Override
    public void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            nameFormats.remove(uuid);
            delete(SQL_DELETE_FORMAT, uuid, "clear name format");
            return;
        }
        nameFormats.put(uuid, prefix);
        upsert(SQL_UPSERT_FORMAT, uuid, prefix, "save name format");
    }

    @Override
    public String getChatPrefix(UUID uuid) {
        return chatPrefixes.get(uuid);
    }

    @Override
    public void setChatPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            chatPrefixes.remove(uuid);
            delete(SQL_DELETE_CHAT_PREFIX, uuid, "clear chat prefix");
            return;
        }
        chatPrefixes.put(uuid, prefix);
        upsert(SQL_UPSERT_CHAT_PREFIX, uuid, prefix, "save chat prefix");
    }

    @Override
    public String getChatSuffix(UUID uuid) {
        return chatSuffixes.get(uuid);
    }

    @Override
    public void setChatSuffix(UUID uuid, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            chatSuffixes.remove(uuid);
            delete(SQL_DELETE_CHAT_SUFFIX, uuid, "clear chat suffix");
            return;
        }
        chatSuffixes.put(uuid, suffix);
        upsert(SQL_UPSERT_CHAT_SUFFIX, uuid, suffix, "save chat suffix");
    }

    private synchronized void upsert(String sql, UUID uuid, String value, String action) {
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, value);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not " + action + " (SQLite): " + e.getMessage());
        }
    }

    private synchronized void delete(String sql, UUID uuid, String action) {
        try {
            Connection c = sqlite.connection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not " + action + " (SQLite): " + e.getMessage());
        }
    }

    @Override
    public void clear(UUID uuid) {
        nameFormats.remove(uuid);
        delete(SQL_DELETE_FORMAT, uuid, "clear name format");
    }

    @Override
    public void clearChatPrefix(UUID uuid) {
        chatPrefixes.remove(uuid);
        delete(SQL_DELETE_CHAT_PREFIX, uuid, "clear chat prefix");
    }

    @Override
    public void clearChatSuffix(UUID uuid) {
        chatSuffixes.remove(uuid);
        delete(SQL_DELETE_CHAT_SUFFIX, uuid, "clear chat suffix");
    }

    @Override
    public void close() {
        nameFormats.clear();
        chatPrefixes.clear();
        chatSuffixes.clear();
    }
}
