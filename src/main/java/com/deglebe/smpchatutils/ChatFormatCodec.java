package com.deglebe.smpchatutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Nullable;

public final class ChatFormatCodec {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final String NAME_VALIDATE_PLACEHOLDER = "\uE000\uE001\uE002";

    private static final TagResolver TAGS_NO_OBF = TagResolver.resolver(
        StandardTags.color(),
        TagResolver.resolver(
            StandardTags.decorations(TextDecoration.BOLD),
            StandardTags.decorations(TextDecoration.ITALIC),
            StandardTags.decorations(TextDecoration.UNDERLINED),
            StandardTags.decorations(TextDecoration.STRIKETHROUGH)
        ),
        StandardTags.gradient(),
        StandardTags.rainbow(),
        StandardTags.transition(),
        StandardTags.reset(),
        StandardTags.newline(),
        StandardTags.pride(),
        StandardTags.shadowColor()
    );

    private static final TagResolver TAGS_WITH_OBF = TagResolver.resolver(
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.gradient(),
        StandardTags.rainbow(),
        StandardTags.transition(),
        StandardTags.reset(),
        StandardTags.newline(),
        StandardTags.pride(),
        StandardTags.shadowColor()
    );

    private static final MiniMessage MINI_NO_OBF = MiniMessage.builder().tags(TAGS_NO_OBF).strict(false).build();
    private static final MiniMessage MINI_WITH_OBF = MiniMessage.builder().tags(TAGS_WITH_OBF).strict(false).build();

    private ChatFormatCodec() {
    }

    private static MiniMessage mini(boolean obf) {
        return obf ? MINI_WITH_OBF : MINI_NO_OBF;
    }

    /** strips {@code &k}/{@code §k} and minimessage obf tags. regex may miss some tag variations */
    public static String stripDisallowedObfuscation(String input, boolean miniMessageEnabled, boolean allowObfuscated) {
        if (allowObfuscated || input == null || input.isEmpty()) {
            return input;
        }
        String s = input.replaceAll("[&§][kK]", "");
        if (miniMessageEnabled) {
            s = s.replaceAll("(?i)</?!obfuscated[^>]*>", "")
                .replaceAll("(?i)</?!obf[^>]*>", "")
                .replaceAll("(?i)</?obfuscated[^>]*>", "")
                .replaceAll("(?i)</?obf[^>]*>", "");
        }
        return s;
    }

    public static boolean probablyMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '<') {
                char c = input.charAt(i + 1);
                if (Character.isLetter(c) || c == '/' || c == '#') {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean needsFormatting(String plain, boolean miniMessageEnabled) {
        return plain.indexOf('&') >= 0 || (miniMessageEnabled && probablyMiniMessage(plain));
    }

    public static Component deserializeForChat(String input, boolean miniMessageEnabled, boolean allowObfuscated) {
        String in = stripDisallowedObfuscation(input, miniMessageEnabled, allowObfuscated);
        if (miniMessageEnabled) {
            Component mm = tryMiniDeserialize(in, allowObfuscated);
            if (mm != null) {
                return mm;
            }
        }
        if (in.indexOf('&') >= 0) {
            return AMPERSAND.deserialize(in);
        }
        return Component.text(in);
    }

    public static Component deserializeStyledName(String format, String playerName, boolean miniMessageEnabled, boolean allowObfuscated) {
        String fmt = stripDisallowedObfuscation(format, miniMessageEnabled, allowObfuscated);
        if (miniMessageEnabled && probablyMiniMessage(fmt)) {
            MiniMessage m = mini(allowObfuscated);
            try {
                return m.deserialize(fmt + m.escapeTags(playerName));
            } catch (ParsingException ignored) {
                // fall through to legacy/plain name (could log at FINE with debug flag at some point)
            }
        }
        if (fmt.indexOf('&') >= 0) {
            return AMPERSAND.deserialize(fmt + playerName);
        }
        return Component.text(playerName);
    }

    public static @Nullable String validateMiniNameFormat(String format, boolean miniMessageEnabled, boolean allowObfuscated) {
        String fmt = stripDisallowedObfuscation(format, miniMessageEnabled, allowObfuscated);
        if (fmt.isEmpty() && !format.trim().isEmpty()) {
            return "Only obfuscated style was given; it is disabled (chat.format.obfuscated).";
        }
        if (!probablyMiniMessage(fmt)) {
            return null;
        }
        MiniMessage m = mini(allowObfuscated);
        final Component parsed;
        try {
            parsed = m.deserialize(fmt + m.escapeTags(NAME_VALIDATE_PLACEHOLDER));
        } catch (ParsingException e) {
            return briefParseMessage(e);
        }
        String plain = PLAIN.serialize(parsed);
        if (plain.indexOf('<') >= 0 || plain.indexOf('>') >= 0) {
            return "Unknown tag or color (e.g. use light_purple or #FFC0CB, not pink). See /nc help.";
        }
        if (!plain.contains(NAME_VALIDATE_PLACEHOLDER)) {
            return "Format did not apply to the name — check /nc help.";
        }
        return null;
    }

    private static @Nullable Component tryMiniDeserialize(String in, boolean allowObfuscated) {
        if (!probablyMiniMessage(in)) {
            return null;
        }
        try {
            return mini(allowObfuscated).deserialize(in);
        } catch (ParsingException ignored) {
            return null;
        }
    }

    private static String briefParseMessage(ParsingException e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            return "Invalid MiniMessage format.";
        }
        int idx = msg.indexOf(':');
        return idx >= 0 ? msg.substring(idx + 1).trim() : msg.trim();
    }
}
