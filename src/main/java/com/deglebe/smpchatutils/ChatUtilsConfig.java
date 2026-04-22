package com.deglebe.smpchatutils;

import com.deglebe.smpchatutils.persistence.StorageType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class ChatUtilsConfig {

    private final JavaPlugin plugin;
    private boolean chatFormatEnabled;
    private boolean miniMessageEnabled;
    private boolean formatObfuscated;
    private boolean nameColorEnabled;
    private boolean ignoreChatEnabled;
    private int nameColorMaxPrefixLength;
    private StorageType storageType = StorageType.YAML;
    private String storageSqliteFile = "smpchatutils.db";

    public ChatUtilsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();
        chatFormatEnabled = c.getBoolean("chat.format.enabled", true);
        miniMessageEnabled = c.getBoolean("chat.format.minimessage", true);
        formatObfuscated = c.getBoolean("chat.format.obfuscated", false);
        nameColorEnabled = c.getBoolean("chat.namecolor.enabled", true);
        ignoreChatEnabled = c.getBoolean("chat.ignore.enabled", true);
        nameColorMaxPrefixLength = Math.max(1, Math.min(128, c.getInt("chat.namecolor.max-prefix-length", 48)));

        String rawType = c.getString("storage.type", "yaml");
        if (rawType == null || rawType.isBlank()) {
            rawType = "yaml";
        }
        String t = rawType.trim().toLowerCase(Locale.ROOT);
        if ("sqlite".equals(t)) {
            storageType = StorageType.SQLITE;
        } else {
            if (!"yaml".equals(t)) {
                plugin.getLogger().warning("Unknown storage.type '" + rawType + "', using yaml.");
            }
            storageType = StorageType.YAML;
        }

        String sqliteInput = c.getString("storage.sqlite-file", "smpchatutils.db");
        storageSqliteFile = sanitizeSqliteFileName(sqliteInput);
        if (sqliteInput != null && !sqliteInput.isBlank()) {
            String expect = sqliteInput.trim();
            if (!expect.equals(storageSqliteFile)) {
                plugin.getLogger().warning("Invalid storage.sqlite-file; using '" + storageSqliteFile + "'.");
            }
        }
    }

    /* single file name only */
    private static String sanitizeSqliteFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "smpchatutils.db";
        }
        String s = raw.trim();
        if (s.contains("..") || s.indexOf('/') >= 0 || s.indexOf('\\') >= 0) {
            return "smpchatutils.db";
        }
        return s;
    }

    public boolean chatFormatEnabled() {
        return chatFormatEnabled;
    }

    public boolean miniMessageEnabled() {
        return miniMessageEnabled;
    }

    public boolean formatObfuscated() {
        return formatObfuscated;
    }

    public boolean nameColorEnabled() {
        return nameColorEnabled;
    }

    /* when false, /ignore commands no-op and chat is not filtered by ignore lists. */
    public boolean ignoreChatEnabled() {
        return ignoreChatEnabled;
    }

    public int nameColorMaxPrefixLength() {
        return nameColorMaxPrefixLength;
    }

    public StorageType storageType() {
        return storageType;
    }

    /* file name only, resolved under plugins/smpchatutils/ when using sqlite */
    public String storageSqliteFile() {
        return storageSqliteFile;
    }
}
