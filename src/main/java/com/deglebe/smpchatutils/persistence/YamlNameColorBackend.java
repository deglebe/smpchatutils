package com.deglebe.smpchatutils.persistence;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class YamlNameColorBackend implements NameColorBackend {

    private final JavaPlugin plugin;
    private final File file;

    // Existing /nc format (legacy key was players.<uuid>: "<format>")
    private final Map<UUID, String> nameFormats = new ConcurrentHashMap<>();
    private final Map<UUID, String> chatPrefixes = new ConcurrentHashMap<>();
    private final Map<UUID, String> chatSuffixes = new ConcurrentHashMap<>();

    YamlNameColorBackend(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "namecolors.yml");
    }

    @Override
    public void load() {
        if (!plugin.getDataFolder().isDirectory() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create data folder for name metadata.");
        }
        nameFormats.clear();
        chatPrefixes.clear();
        chatSuffixes.clear();

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
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            // Backward compatibility: players.<uuid>: "<format>"
            String legacy = players.getString(key);
            if (legacy != null && !legacy.isEmpty()) {
                nameFormats.put(id, legacy);
                continue;
            }

            ConfigurationSection node = players.getConfigurationSection(key);
            if (node == null) {
                continue;
            }

            putIfNonEmpty(nameFormats, id, node.getString("format", ""));
            putIfNonEmpty(chatPrefixes, id, node.getString("chat-prefix", ""));
            putIfNonEmpty(chatSuffixes, id, node.getString("chat-suffix", ""));
        }
    }

    private static void putIfNonEmpty(Map<UUID, String> map, UUID id, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(id, value);
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection players = yaml.createSection("players");

        Set<UUID> ids = new HashSet<>();
        ids.addAll(nameFormats.keySet());
        ids.addAll(chatPrefixes.keySet());
        ids.addAll(chatSuffixes.keySet());

        for (UUID id : ids) {
            ConfigurationSection node = players.createSection(id.toString());
            String f = nameFormats.get(id);
            String p = chatPrefixes.get(id);
            String s = chatSuffixes.get(id);
            if (f != null && !f.isEmpty()) {
                node.set("format", f);
            }
            if (p != null && !p.isEmpty()) {
                node.set("chat-prefix", p);
            }
            if (s != null && !s.isEmpty()) {
                node.set("chat-suffix", s);
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save namecolors.yml: " + e.getMessage());
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
        } else {
            nameFormats.put(uuid, prefix);
        }
        save();
    }

    @Override
    public String getChatPrefix(UUID uuid) {
        return chatPrefixes.get(uuid);
    }

    @Override
    public void setChatPrefix(UUID uuid, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            chatPrefixes.remove(uuid);
        } else {
            chatPrefixes.put(uuid, prefix);
        }
        save();
    }

    @Override
    public String getChatSuffix(UUID uuid) {
        return chatSuffixes.get(uuid);
    }

    @Override
    public void setChatSuffix(UUID uuid, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            chatSuffixes.remove(uuid);
        } else {
            chatSuffixes.put(uuid, suffix);
        }
        save();
    }

    @Override
    public void clear(UUID uuid) {
        nameFormats.remove(uuid);
        save();
    }

    @Override
    public void clearChatPrefix(UUID uuid) {
        chatPrefixes.remove(uuid);
        save();
    }

    @Override
    public void clearChatSuffix(UUID uuid) {
        chatSuffixes.remove(uuid);
        save();
    }

    @Override
    public void close() {
        nameFormats.clear();
        chatPrefixes.clear();
        chatSuffixes.clear();
    }
}
