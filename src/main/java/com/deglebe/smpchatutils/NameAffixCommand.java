package com.deglebe.smpchatutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NameAffixCommand {

    public enum Mode {
        PREFIX,
        SUFFIX
    }

    private final Smpchatutils plugin;

    public NameAffixCommand(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    public boolean runPlayer(@NotNull Player player, @NotNull String @NotNull [] args, Mode mode) {
        if (!player.hasPermission(permission(mode))) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }
        if (!enabled(mode)) {
            player.sendMessage(Component.text(label(mode) + " is disabled on this server.", NamedTextColor.GRAY));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player, mode);
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            clear(player, mode);
            player.sendMessage(Component.text(cap(mode) + " cleared.", NamedTextColor.GREEN));
            return true;
        }
        if (args[0].equalsIgnoreCase("preview")) {
            sendPreview(player, mode);
            return true;
        }

        String raw = String.join(" ", args).trim();
        int max = maxLen(mode);
        if (raw.length() > max) {
            player.sendMessage(Component.text(cap(mode) + " too long (max " + max + " chars).", NamedTextColor.RED));
            return true;
        }

        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        String cleaned = ChatFormatCodec.stripDisallowedObfuscation(raw, mini, obf);
        if (cleaned.trim().isEmpty() && !raw.trim().isEmpty()) {
            player.sendMessage(Component.text(
                "Only obfuscated formatting (&k / <obf>) - disabled unless chat.format.obfuscated is true.",
                NamedTextColor.RED
            ));
            return true;
        }

        set(player, mode, cleaned);
        player.sendMessage(Component.text(cap(mode) + " set! Preview: ", NamedTextColor.GREEN)
            .append(previewComponent(player, mode, cleaned)));
        return true;
    }

    public @Nullable List<String> tabComplete(
        @NotNull CommandSender sender,
        @NotNull String @NotNull [] args,
        Mode mode
    ) {
        if (!(sender instanceof Player player) || !player.hasPermission(permission(mode))) {
            return List.of();
        }
        if (!enabled(mode)) {
            return List.of();
        }
        if (args.length == 0 || (args.length == 1 && args[0].isEmpty())) {
            return List.of("clear", "preview", "help");
        }
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            return List.of("clear", "preview", "help").stream().filter(s -> s.startsWith(p)).toList();
        }
        return List.of();
    }

    private void sendHelp(Player player, Mode mode) {
        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        String cur = get(player, mode);

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- " + cap(mode) + " ---", NamedTextColor.GOLD));
        if (cur == null || cur.isEmpty()) {
            player.sendMessage(Component.text("Current: none", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Current: ", NamedTextColor.GRAY)
                .append(ChatFormatCodec.deserializeForChat(cur, mini, obf)));
        }
        player.sendMessage(Component.text("Usage: /" + label(mode) + " <text> | clear | preview | help", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Also: /smpchatutils " + label(mode) + " <text>", NamedTextColor.GRAY));
        player.sendMessage(Component.text(
            "Supports plain text, legacy & codes, and MiniMessage when enabled.",
            NamedTextColor.GRAY
        ));
    }

    private void sendPreview(Player player, Mode mode) {
        String cur = get(player, mode);
        if (cur == null || cur.isEmpty()) {
            player.sendMessage(Component.text("No " + label(mode) + " set.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("Preview: ", NamedTextColor.GRAY)
            .append(previewComponent(player, mode, cur)));
    }

    private Component previewComponent(Player player, Mode mode, String value) {
        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        Component affix = ChatFormatCodec.deserializeForChat(value, mini, obf);
        return mode == Mode.PREFIX
            ? affix.append(Component.text(player.getName(), NamedTextColor.WHITE))
            : Component.text(player.getName(), NamedTextColor.WHITE).append(affix);
    }

    private boolean enabled(Mode mode) {
        return mode == Mode.PREFIX ? plugin.config().chatPrefixEnabled() : plugin.config().chatSuffixEnabled();
    }

    private int maxLen(Mode mode) {
        return mode == Mode.PREFIX ? plugin.config().chatPrefixMaxLength() : plugin.config().chatSuffixMaxLength();
    }

    private static String permission(Mode mode) {
        return mode == Mode.PREFIX ? "smpchatutils.chat.prefix" : "smpchatutils.chat.suffix";
    }

    private static String label(Mode mode) {
        return mode == Mode.PREFIX ? "prefix" : "suffix";
    }

    private static String cap(Mode mode) {
        return mode == Mode.PREFIX ? "Prefix" : "Suffix";
    }

    private String get(Player player, Mode mode) {
        return mode == Mode.PREFIX
            ? plugin.nameColors().getChatPrefix(player.getUniqueId())
            : plugin.nameColors().getChatSuffix(player.getUniqueId());
    }

    private void set(Player player, Mode mode, String value) {
        if (mode == Mode.PREFIX) {
            plugin.nameColors().setChatPrefix(player.getUniqueId(), value);
        } else {
            plugin.nameColors().setChatSuffix(player.getUniqueId(), value);
        }
    }

    private void clear(Player player, Mode mode) {
        if (mode == Mode.PREFIX) {
            plugin.nameColors().clearChatPrefix(player.getUniqueId());
        } else {
            plugin.nameColors().clearChatSuffix(player.getUniqueId());
        }
    }
}
