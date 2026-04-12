package com.deglebe.smpchatutils.persistence;

import com.deglebe.smpchatutils.ChatUtilsConfig;
import com.deglebe.smpchatutils.Smpchatutils;
import com.deglebe.smpchatutils.persistence.sqlite.PluginSqlite;

import java.util.UUID;

public final class NameColorStore {

    private final Smpchatutils plugin;
    private NameColorBackend backend;
    private PluginSqlite sqlite;

    public NameColorStore(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    /* reload from disk using current config */
    public void load() {
        disposeBackends();
        ChatUtilsConfig cfg = plugin.config();
        if (cfg.storageType() == StorageType.SQLITE) {
            sqlite = new PluginSqlite(plugin, cfg);
            backend = new SqliteNameColorBackend(plugin, sqlite);
        } else {
            backend = new YamlNameColorBackend(plugin);
        }
        backend.load();
    }

    public void close() {
        disposeBackends();
    }

    private void disposeBackends() {
        if (backend != null) {
            backend.close();
            backend = null;
        }
        if (sqlite != null) {
            sqlite.close();
            sqlite = null;
        }
    }

    public String getPrefix(UUID uuid) {
        return backend == null ? null : backend.getPrefix(uuid);
    }

    public void setPrefix(UUID uuid, String prefix) {
        if (backend != null) {
            backend.setPrefix(uuid, prefix);
        }
    }

    public void clear(UUID uuid) {
        if (backend != null) {
            backend.clear(uuid);
        }
    }

    /* return shared db handle in sqlire mode, or null for yaml */
    public PluginSqlite sqlite() {
        return sqlite;
    }
}
