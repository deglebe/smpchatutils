package com.deglebe.smpchatutils.persistence;

import com.deglebe.smpchatutils.ChatUtilsConfig;
import com.deglebe.smpchatutils.Smpchatutils;
import com.deglebe.smpchatutils.persistence.sqlite.PluginSqlite;

import java.util.Set;
import java.util.UUID;

/*
 * ignore relationships. uses the same storage type as name colours.
 * in sqlite mode, reuses NameColorStore#sqlite(), call NameColorStore#load() before
 * load() on reload so shared db handle is current
*/
public final class IgnoreListStore {

    private final Smpchatutils plugin;
    private IgnoreListBackend backend;

    public IgnoreListStore(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    public void load() {
        if (backend != null) {
            backend.close();
            backend = null;
        }
        ChatUtilsConfig cfg = plugin.config();
        if (cfg.storageType() == StorageType.SQLITE) {
            PluginSqlite sqlite = plugin.nameColors().sqlite();
            if (sqlite == null) {
                plugin.getLogger().severe(
                    "Ignore list: storage is SQLite but no DB handle; ensure name-color storage loaded first."
                );
                return;
            }
            backend = new SqliteIgnoreListBackend(plugin, sqlite);
        } else {
            backend = new YamlIgnoreListBackend(plugin);
        }
        backend.load();
    }

    public void close() {
        if (backend != null) {
            backend.close();
            backend = null;
        }
    }

    public boolean isIgnoring(UUID ignorer, UUID ignored) {
        return backend != null && backend.isIgnoring(ignorer, ignored);
    }

    public void addIgnore(UUID ignorer, UUID ignored) {
        if (backend != null) {
            backend.addIgnore(ignorer, ignored);
        }
    }

    public void removeIgnore(UUID ignorer, UUID ignored) {
        if (backend != null) {
            backend.removeIgnore(ignorer, ignored);
        }
    }

    public Set<UUID> getIgnored(UUID ignorer) {
        return backend == null ? Set.of() : backend.getIgnored(ignorer);
    }
}
