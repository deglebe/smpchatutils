package com.deglebe.smpchatutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SmpchatutilsCommand implements TabExecutor {

    private final Smpchatutils plugin;
    private final NameColorCommand nameColor;
    private final NameAffixCommand affix;

    public SmpchatutilsCommand(Smpchatutils plugin) {
        this.plugin = plugin;
        this.nameColor = new NameColorCommand(plugin);
        this.affix = new NameAffixCommand(plugin);
    }

    private static boolean isNameColorLabel(@NotNull String s) {
        return s.equalsIgnoreCase("namecolor") || s.equalsIgnoreCase("nc");
    }

    private static boolean isPrefixLabel(@NotNull String s) {
        return s.equalsIgnoreCase("prefix");
    }

    private static boolean isSuffixLabel(@NotNull String s) {
        return s.equalsIgnoreCase("suffix");
    }

    private static @Nullable NameAffixCommand.Mode affixMode(@NotNull String s) {
        if (isPrefixLabel(s)) {
            return NameAffixCommand.Mode.PREFIX;
        }
        if (isSuffixLabel(s)) {
            return NameAffixCommand.Mode.SUFFIX;
        }
        return null;
    }

    private boolean runNameColor(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        return nameColor.runPlayer(player, args);
    }

    private boolean runAffix(
        @NotNull CommandSender sender,
        @NotNull String @NotNull [] args,
        @NotNull NameAffixCommand.Mode mode
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        return affix.runPlayer(player, args, mode);
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String @NotNull [] args
    ) {
        NameAffixCommand.Mode directAffix = affixMode(label);
        if (directAffix != null) {
            return runAffix(sender, args, directAffix);
        }
        if (isNameColorLabel(label)) {
            return runNameColor(sender, args);
        }

        if (args.length > 0) {
            NameAffixCommand.Mode subAffix = affixMode(args[0]);
            if (subAffix != null) {
                return runAffix(sender, Arrays.copyOfRange(args, 1, args.length), subAffix);
            }
            if (isNameColorLabel(args[0])) {
                return runNameColor(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        if (args.length == 0) {
            sender.sendMessage(usage());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("smpchatutils.reload")) {
                sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
                return true;
            }
            plugin.config().reload();
            plugin.nameColors().load();
            plugin.ignoreLists().load();
            sender.sendMessage(Component.text("smpchatutils: configuration and stores reloaded.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(usage());
        return true;
    }

    private static Component usage() {
        return Component.text(
            "Usage: /smpchatutils reload | /namecolor … | /prefix … | /suffix …",
            NamedTextColor.GRAY
        );
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String alias,
        @NotNull String @NotNull [] args
    ) {
        NameAffixCommand.Mode directAffix = affixMode(alias);
        if (directAffix != null) {
            return affix.tabComplete(sender, args, directAffix);
        }
        if (isNameColorLabel(alias)) {
            return nameColor.tabComplete(sender, args);
        }

        if (args.length == 1) {
            String p = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            if (sender.hasPermission("smpchatutils.reload") && "reload".startsWith(p)) {
                out.add("reload");
            }
            if (sender.hasPermission("smpchatutils.chat.namecolor")) {
                if ("namecolor".startsWith(p)) {
                    out.add("namecolor");
                }
                if ("nc".startsWith(p)) {
                    out.add("nc");
                }
            }
            if (sender.hasPermission("smpchatutils.chat.prefix") && "prefix".startsWith(p)) {
                out.add("prefix");
            }
            if (sender.hasPermission("smpchatutils.chat.suffix") && "suffix".startsWith(p)) {
                out.add("suffix");
            }
            return out;
        }

        if (args.length >= 2 && sender instanceof Player) {
            NameAffixCommand.Mode subAffix = affixMode(args[0]);
            if (subAffix != null) {
                return affix.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length), subAffix);
            }
            if (isNameColorLabel(args[0])) {
                return nameColor.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return List.of();
    }
}
