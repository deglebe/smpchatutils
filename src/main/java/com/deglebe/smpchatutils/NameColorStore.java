package com.deglebe.smpchatutils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NameColorStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, String> prefixes = new ConcurrentHashMap<>();

    public NameColorStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "namecolors.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for name colors.");
        }
        prefixes.clear();
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("Could not create namecolors.yml.");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create namecolors.yml: " + e.getMessage());
            }
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                String v = section.getString(key, "");
                if (v == null || v.isEmpty()) {
                    continue;
                }
                prefixes.put(UUID.fromString(key), v);
            } catch (IllegalArgumentException ignored) {
                // skip malformed keys in players.<uuid>
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("players");
        for (Map.Entry<UUID, String> e : prefixes.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                section.set(e.getKey().toString(), e.getValue());
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save namecolors.yml: " + e.getMessage());
        }
    }

    public String getPrefix(UUID uuid) {
        return prefixes.get(uuid);
    }

    public void setPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            prefixes.remove(uuid);
        } else {
            prefixes.put(uuid, prefix);
        }
        save();
    }

    public void clear(UUID uuid) {
        prefixes.remove(uuid);
        save();
    }
}
