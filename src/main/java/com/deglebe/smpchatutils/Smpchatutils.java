package com.deglebe.smpchatutils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Smpchatutils extends JavaPlugin {

    private ChatUtilsConfig chatUtilsConfig;
    private NameColorStore nameColorStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        chatUtilsConfig = new ChatUtilsConfig(this);
        chatUtilsConfig.load();

        nameColorStore = new NameColorStore(this);
        nameColorStore.load();

        var cmd = getCommand("smpchatutils");
        if (cmd == null) {
            getLogger().severe("Command 'smpchatutils' missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        var executor = new SmpchatutilsCommand(this);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);

        Bukkit.getPluginManager().registerEvents(new ChatFormatListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    public ChatUtilsConfig config() {
        return chatUtilsConfig;
    }

    public NameColorStore nameColors() {
        return nameColorStore;
    }
}
