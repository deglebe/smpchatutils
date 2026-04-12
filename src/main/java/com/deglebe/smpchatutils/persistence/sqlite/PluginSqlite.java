package com.deglebe.smpchatutils.persistence.sqlite;

import com.deglebe.smpchatutils.ChatUtilsConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/* shared sqlite file under plugin data folder */
public final class PluginSqlite implements AutoCloseable {

    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public PluginSqlite(JavaPlugin plugin, ChatUtilsConfig config) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), config.storageSqliteFile());
    }

    public synchronized Connection connection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for SQLite.");
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
        }
        SqliteSchema.apply(connection);
        return connection;
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }
}
