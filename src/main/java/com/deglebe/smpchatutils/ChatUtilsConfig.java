package com.deglebe.smpchatutils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatUtilsConfig {

    private final JavaPlugin plugin;
    private boolean chatFormatEnabled;
    private boolean miniMessageEnabled;
    private boolean formatObfuscated;
    private boolean nameColorEnabled;
    private int nameColorMaxPrefixLength;

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
        nameColorMaxPrefixLength = Math.max(1, Math.min(128, c.getInt("chat.namecolor.max-prefix-length", 48)));
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

    public int nameColorMaxPrefixLength() {
        return nameColorMaxPrefixLength;
    }
}
