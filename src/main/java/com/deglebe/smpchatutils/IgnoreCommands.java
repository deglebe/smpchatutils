package com.deglebe.smpchatutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class IgnoreCommands implements TabExecutor {

    private final Smpchatutils plugin;

    public IgnoreCommands(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("smpchatutils.chat.ignore")) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }
        if (!plugin.config().ignoreChatEnabled()) {
            player.sendMessage(Component.text("Ignore is disabled on this server.", NamedTextColor.GRAY));
            return true;
        }

        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "ignore" -> runIgnore(player, args);
            case "unignore" -> runUnignore(player, args);
            case "ignorelist" -> runIgnoreList(player);
            default -> true;
        };
    }

    private boolean runIgnore(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length < 1 || args[0].isBlank()) {
            player.sendMessage(Component.text("Usage: /ignore <player>", NamedTextColor.GRAY));
            return true;
        }
        String raw = String.join(" ", args).trim();
        UUID target = resolveTargetUuid(player, raw);
        if (target == null) {
            player.sendMessage(Component.text("Unknown player.", NamedTextColor.RED));
            return true;
        }
        if (target.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You can't ignore yourself.", NamedTextColor.RED));
            return true;
        }
        if (plugin.ignoreLists().isIgnoring(player.getUniqueId(), target)) {
            player.sendMessage(Component.text("You're already ignoring ", NamedTextColor.GRAY)
                .append(Component.text(displayName(target), NamedTextColor.WHITE))
                .append(Component.text(".", NamedTextColor.GRAY)));
            return true;
        }
        plugin.ignoreLists().addIgnore(player.getUniqueId(), target);
        player.sendMessage(Component.text("Ignoring ", NamedTextColor.GREEN)
            .append(Component.text(displayName(target), NamedTextColor.WHITE))
            .append(Component.text(" - you won't see their public chat.", NamedTextColor.GREEN)));
        return true;
    }

    private boolean runUnignore(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (args.length < 1 || args[0].isBlank()) {
            player.sendMessage(Component.text("Usage: /unignore <player>", NamedTextColor.GRAY));
            return true;
        }
        String raw = String.join(" ", args).trim();
        UUID target = resolveTargetUuid(player, raw);
        if (target == null) {
            player.sendMessage(Component.text("Unknown player.", NamedTextColor.RED));
            return true;
        }
        if (!plugin.ignoreLists().isIgnoring(player.getUniqueId(), target)) {
            player.sendMessage(Component.text("You're not ignoring ", NamedTextColor.GRAY)
                .append(Component.text(displayName(target), NamedTextColor.WHITE))
                .append(Component.text(".", NamedTextColor.GRAY)));
            return true;
        }
        plugin.ignoreLists().removeIgnore(player.getUniqueId(), target);
        player.sendMessage(Component.text("No longer ignoring ", NamedTextColor.GREEN)
            .append(Component.text(displayName(target), NamedTextColor.WHITE))
            .append(Component.text(".", NamedTextColor.GREEN)));
        return true;
    }

    private boolean runIgnoreList(@NotNull Player player) {
        var ignored = plugin.ignoreLists().getIgnored(player.getUniqueId());
        if (ignored.isEmpty()) {
            player.sendMessage(Component.text("You're not ignoring anyone.", NamedTextColor.GRAY));
            return true;
        }
        player.sendMessage(Component.text("Ignored (" + ignored.size() + "):", NamedTextColor.GOLD));
        var line = Component.empty();
        boolean first = true;
        for (UUID id : ignored) {
            if (!first) {
                line = line.append(Component.text(", ", NamedTextColor.DARK_GRAY));
            }
            first = false;
            line = line.append(Component.text(displayName(id), NamedTextColor.WHITE));
        }
        player.sendMessage(line);
        return true;
    }

    private static String displayName(UUID id) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(id);
        String n = off.getName();
        return n != null && !n.isEmpty() ? n : id.toString();
    }

    /* online match, then offline if they've played before */
    private static @Nullable UUID resolveTargetUuid(Player source, String name) {
        Player exact = source.getServer().getPlayerExact(name);
        if (exact != null) {
            return exact.getUniqueId();
        }
        for (Player p : source.getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p.getUniqueId();
            }
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        if (off.hasPlayedBefore() || off.isOnline()) {
            return off.getUniqueId();
        }
        return null;
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player player) || !player.hasPermission("smpchatutils.chat.ignore")) {
            return List.of();
        }
        if (!plugin.config().ignoreChatEnabled()) {
            return List.of();
        }
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        if (args.length != 1) {
            return List.of();
        }
        String p = args[0].toLowerCase(Locale.ROOT);
        if ("ignore".equals(cmd)) {
            List<String> out = new ArrayList<>();
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.equals(player)) {
                    continue;
                }
                if (o.getName().toLowerCase(Locale.ROOT).startsWith(p)) {
                    out.add(o.getName());
                }
            }
            return out;
        }
        if ("unignore".equals(cmd)) {
            List<String> out = new ArrayList<>();
            for (UUID id : plugin.ignoreLists().getIgnored(player.getUniqueId())) {
                OfflinePlayer off = Bukkit.getOfflinePlayer(id);
                String n = off.getName();
                if (n != null && !n.isEmpty() && n.toLowerCase(Locale.ROOT).startsWith(p)) {
                    out.add(n);
                }
            }
            return out;
        }
        return List.of();
    }
}
