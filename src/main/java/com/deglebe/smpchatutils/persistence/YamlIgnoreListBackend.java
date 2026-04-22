package com.deglebe.smpchatutils.persistence;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/* ignores.yml under plugins/smpchatutils/ */
final class YamlIgnoreListBackend implements IgnoreListBackend {

    private final JavaPlugin plugin;
    private final File file;
    /* ignorer -> ignored players */
    private final ConcurrentHashMap<UUID, Set<UUID>> byIgnorer = new ConcurrentHashMap<>();

    YamlIgnoreListBackend(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ignores.yml");
    }

    @Override
    public void load() {
        if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for ignore list.");
        }
        byIgnorer.clear();
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create ignores.yml.");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create ignores.yml: " + e.getMessage());
            }
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("ignores");
        if (root == null) {
            return;
        }
        for (String ignorerKey : root.getKeys(false)) {
            UUID ignorer;
            try {
                ignorer = UUID.fromString(ignorerKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            List<String> list = root.getStringList(ignorerKey);
            Set<UUID> set = ConcurrentHashMap.newKeySet();
            for (String s : list) {
                try {
                    set.add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                    // skip bad uuid
                }
            }
            if (!set.isEmpty()) {
                byIgnorer.put(ignorer, set);
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection ignores = yaml.createSection("ignores");
        for (var e : byIgnorer.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }
            ignores.set(e.getKey().toString(), e.getValue().stream().map(UUID::toString).toList());
        }
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save ignores.yml: " + ex.getMessage());
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
        save();
    }

    @Override
    public void removeIgnore(UUID ignorer, UUID ignored) {
        Set<UUID> set = byIgnorer.get(ignorer);
        if (set == null) {
            return;
        }
        set.remove(ignored);
        if (set.isEmpty()) {
            byIgnorer.remove(ignorer);
        }
        save();
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
