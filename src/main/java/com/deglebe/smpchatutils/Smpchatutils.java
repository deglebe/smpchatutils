package com.deglebe.smpchatutils;

import com.deglebe.smpchatutils.persistence.IgnoreListStore;
import com.deglebe.smpchatutils.persistence.NameColorStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Smpchatutils extends JavaPlugin {

    private ChatUtilsConfig chatUtilsConfig;
    private NameColorStore nameColorStore;
    private IgnoreListStore ignoreListStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        chatUtilsConfig = new ChatUtilsConfig(this);
        chatUtilsConfig.load();

        nameColorStore = new NameColorStore(this);
        nameColorStore.load();

        ignoreListStore = new IgnoreListStore(this);
        ignoreListStore.load();

        var cmd = getCommand("smpchatutils");
        if (cmd == null) {
            getLogger().severe("Command 'smpchatutils' missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var executor = new SmpchatutilsCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        for (String name : new String[] { "prefix", "suffix" }) {
            var c = getCommand(name);
            if (c == null) {
                getLogger().warning("Command '" + name + "' missing from plugin.yml.");
            } else {
                c.setExecutor(executor);
                c.setTabCompleter(executor);
            }
        }

        var ignores = new IgnoreCommands(this);
        for (String name : new String[] { "ignore", "unignore", "ignorelist" }) {
            var c = getCommand(name);
            if (c == null) {
                getLogger().warning("Command '" + name + "' missing from plugin.yml.");
            } else {
                c.setExecutor(ignores);
                c.setTabCompleter(ignores);
            }
        }

        Bukkit.getPluginManager().registerEvents(new ChatFormatListener(this), this);
    }

    @Override
    public void onDisable() {
        if (ignoreListStore != null) {
            ignoreListStore.close();
        }
        if (nameColorStore != null) {
            nameColorStore.close();
        }
    }

    public ChatUtilsConfig config() {
        return chatUtilsConfig;
    }

    public NameColorStore nameColors() {
        return nameColorStore;
    }

    public IgnoreListStore ignoreLists() {
        return ignoreListStore;
    }
}
