package com.deglebe.smpchatutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class NameColorCommand {

    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final NamedTextColor[] LEGACY_COLOR = {
        NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN,
        NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED, NamedTextColor.DARK_PURPLE,
        NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.BLUE,
        NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE,
        NamedTextColor.YELLOW, NamedTextColor.WHITE
    };
    private static final String[] EXAMPLES_LEGACY = { "&6", "&c&l", "&#FF5555", "&b&o" };
    private static final String[] EXAMPLES_MINI = {
        "<gradient:gold:red>",
        "<gradient:#5e4fa2:#f79459>",
        "<rainbow>",
        "<rainbow:!>",
        "<transition:#ff0000:#00ff00:#0000ff>",
        "<bold><gradient:aqua:dark_purple>",
        "<italic><gradient:#FFD700:#FF69B4>"
    };
    private static final String[] SAMPLE_NAMES = {
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
        "dark_purple", "gold", "gray", "dark_gray", "blue",
        "green", "aqua", "red", "light_purple", "yellow", "white"
    };
    private static final NamedTextColor[] SAMPLE_COLORS = {
        NamedTextColor.BLACK, NamedTextColor.DARK_BLUE, NamedTextColor.DARK_GREEN,
        NamedTextColor.DARK_AQUA, NamedTextColor.DARK_RED,
        NamedTextColor.DARK_PURPLE, NamedTextColor.GOLD, NamedTextColor.GRAY, NamedTextColor.DARK_GRAY, NamedTextColor.BLUE,
        NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.RED, NamedTextColor.LIGHT_PURPLE, NamedTextColor.YELLOW, NamedTextColor.WHITE
    };

    private final Smpchatutils plugin;

    public NameColorCommand(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    public boolean runPlayer(@NotNull Player player, @NotNull String @NotNull [] args) {
        if (!player.hasPermission("smpchatutils.chat.namecolor")) {
            player.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            plugin.nameColors().clear(player.getUniqueId());
            player.sendMessage(Component.text("Name color cleared.", NamedTextColor.GREEN));
            return true;
        }
        if (args[0].equalsIgnoreCase("preview")) {
            sendPreview(player);
            return true;
        }
        String format = String.join(" ", args).trim();
        int max = plugin.config().nameColorMaxPrefixLength();
        if (format.length() > max) {
            player.sendMessage(Component.text("Format too long (max " + max + " characters).", NamedTextColor.RED));
            return true;
        }
        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        String stripped = ChatFormatCodec.stripDisallowedObfuscation(format, mini, obf);
        if (stripped.trim().isEmpty() && !format.trim().isEmpty()) {
            player.sendMessage(Component.text(
                "Only obfuscated formatting (&k / <obf>) — disabled unless chat.format.obfuscated is true.",
                NamedTextColor.RED));
            return true;
        }
        if (!ChatFormatCodec.needsFormatting(stripped, mini)) {
            player.sendMessage(Component.text("Use legacy & codes (e.g. &6) or MiniMessage (e.g. <gold>).", NamedTextColor.RED));
            return true;
        }
        if (mini && ChatFormatCodec.probablyMiniMessage(stripped)) {
            String error = ChatFormatCodec.validateMiniNameFormat(format, mini, obf);
            if (error != null) {
                player.sendMessage(Component.text("Invalid format: ", NamedTextColor.RED).append(Component.text(error, NamedTextColor.GRAY)));
                player.sendMessage(Component.text("See /nc help — gradients need no close tag.", NamedTextColor.GRAY));
                return true;
            }
        }
        plugin.nameColors().setPrefix(player.getUniqueId(), format);
        player.sendMessage(Component.text("Name color set! Preview: ", NamedTextColor.GREEN)
            .append(ChatFormatCodec.deserializeStyledName(format, player.getName(), mini, obf)));
        return true;
    }

    private void sendHelp(@NotNull Player player) {
        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        String name = player.getName();
        String cur = plugin.nameColors().getPrefix(player.getUniqueId());

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- Name Color ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        if (cur != null && !cur.isEmpty()) {
            player.sendMessage(Component.text("Current: ", NamedTextColor.GRAY)
                .append(ChatFormatCodec.deserializeStyledName(cur, name, mini, obf)));
        } else {
            player.sendMessage(Component.text("Current: none", NamedTextColor.GRAY));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Usage: /nc <format> | clear | preview | help", NamedTextColor.YELLOW));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Legacy & codes:", NamedTextColor.YELLOW));
        player.sendMessage(legacyColorLegend());
        player.sendMessage(Component.text("  &l", NamedTextColor.WHITE).append(Component.text(" bold", NamedTextColor.WHITE, TextDecoration.BOLD))
            .append(Component.text("  &o", NamedTextColor.WHITE)).append(Component.text(" italic", NamedTextColor.WHITE, TextDecoration.ITALIC))
            .append(Component.text("  &n", NamedTextColor.WHITE)).append(Component.text(" underline", NamedTextColor.WHITE, TextDecoration.UNDERLINED)));
        player.sendMessage(Component.text("  &m", NamedTextColor.WHITE).append(Component.text(" strikethrough", NamedTextColor.WHITE, TextDecoration.STRIKETHROUGH))
            .append(obf
                ? Component.text("  &k", NamedTextColor.WHITE).append(Component.text(" obfuscated", NamedTextColor.WHITE, TextDecoration.OBFUSCATED))
                : Component.empty())
            .append(Component.text("  &r reset", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Hex &#RRGGBB (e.g. &#FF5555)", NamedTextColor.GRAY));

        if (mini) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("MiniMessage:", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  Styles: ", NamedTextColor.GRAY).append(Component.text(
                obf
                    ? "<bold> <italic> <underlined> <strikethrough> <obfuscated>"
                    : "<bold> <italic> <underlined> <strikethrough> (+ <obf> if chat.format.obfuscated)",
                NamedTextColor.WHITE)));
            player.sendMessage(Component.text("  <gradient:a:b> <rainbow> <rainbow:!> <transition:a:b…> — no </…> needed for names", NamedTextColor.GRAY));
            player.sendMessage(colorNameSamples());
            player.sendMessage(Component.text("  Hex <#RRGGBB> in tags", NamedTextColor.GRAY));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("Examples:", NamedTextColor.YELLOW));
        for (String ex : EXAMPLES_LEGACY) {
            sendExampleLine(player, ex, name, mini, obf);
        }
        if (mini) {
            for (String ex : EXAMPLES_MINI) {
                sendExampleLine(player, ex, name, mini, obf);
            }
        }
        player.sendMessage(Component.empty());
    }

    private static Component legacyColorLegend() {
        Component line = Component.text("  ");
        for (int i = 0; i < LEGACY_CODES.length(); i++) {
            if (i > 0) {
                line = line.append(Component.text(" "));
            }
            line = line.append(Component.text("&" + LEGACY_CODES.charAt(i), LEGACY_COLOR[i]));
        }
        return line;
    }

    private static Component colorNameSamples() {
        Component line = Component.text("  Names: ");
        for (int i = 0; i < SAMPLE_NAMES.length; i++) {
            if (i > 0) {
                line = line.append(Component.text(" "));
            }
            line = line.append(Component.text(SAMPLE_NAMES[i], SAMPLE_COLORS[i]));
        }
        return line;
    }

    private static void sendExampleLine(Player player, String code, String name, boolean mini, boolean obf) {
        player.sendMessage(Component.text("  " + code + " → ", NamedTextColor.GRAY)
            .append(ChatFormatCodec.deserializeStyledName(code, name, mini, obf)));
    }

    private void sendPreview(@NotNull Player player) {
        boolean mini = plugin.config().miniMessageEnabled();
        boolean obf = plugin.config().formatObfuscated();
        String cur = plugin.nameColors().getPrefix(player.getUniqueId());
        if (cur == null || cur.isEmpty()) {
            player.sendMessage(Component.text("No name color set. Use /nc <format>.", NamedTextColor.GRAY));
            return;
        }
        player.sendMessage(Component.text("Preview: ", NamedTextColor.GRAY)
            .append(ChatFormatCodec.deserializeStyledName(cur, player.getName(), mini, obf)));
    }

    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("smpchatutils.chat.namecolor")) {
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
}
