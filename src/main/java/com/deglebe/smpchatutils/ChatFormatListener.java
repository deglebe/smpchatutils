package com.deglebe.smpchatutils;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

public final class ChatFormatListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Smpchatutils plugin;

    public ChatFormatListener(Smpchatutils plugin) {
        this.plugin = plugin;
    }

    /* drop viewers who have ignored the sender */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChatFilterIgnored(AsyncChatEvent event) {
        if (!plugin.config().ignoreChatEnabled()) {
            return;
        }
        Player sender = event.getPlayer();
        UUID senderId = sender.getUniqueId();
        event.viewers().removeIf(audience -> filterOutIgnoredViewer(audience, senderId));
    }

    private boolean filterOutIgnoredViewer(Audience audience, UUID senderId) {
        if (!(audience instanceof Player viewer)) {
            return false;
        }
        if (viewer.getUniqueId().equals(senderId)) {
            return false;
        }
        return plugin.ignoreLists().isIgnoring(viewer.getUniqueId(), senderId);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        applyNameDecorations(event);
        applyMessageFormat(event);
    }

    private void applyNameDecorations(final AsyncChatEvent event) {
        var cfg = plugin.config();
        Player player = event.getPlayer();
        boolean allowNameColor = cfg.nameColorEnabled() && player.hasPermission("smpchatutils.chat.namecolor");
        boolean allowChatPrefix = cfg.chatPrefixEnabled() && player.hasPermission("smpchatutils.chat.prefix");
        boolean allowChatSuffix = cfg.chatSuffixEnabled() && player.hasPermission("smpchatutils.chat.suffix");

        String nameFormat = allowNameColor ? plugin.nameColors().getPrefix(player.getUniqueId()) : null;
        String chatPrefix = allowChatPrefix ? plugin.nameColors().getChatPrefix(player.getUniqueId()) : null;
        String chatSuffix = allowChatSuffix ? plugin.nameColors().getChatSuffix(player.getUniqueId()) : null;

        boolean hasNameFormat = nameFormat != null && !nameFormat.isEmpty();
        boolean hasChatPrefix = chatPrefix != null && !chatPrefix.isEmpty();
        boolean hasChatSuffix = chatSuffix != null && !chatSuffix.isEmpty();
        if (!hasNameFormat && !hasChatPrefix && !hasChatSuffix) {
            return;
        }
        boolean mini = cfg.miniMessageEnabled();
        boolean obf = cfg.formatObfuscated();
        ChatRenderer base = event.renderer();
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component display = sourceDisplayName;
            if (hasNameFormat) {
                // name format still flattens component tree; prefix/suffix below keep structure when used alone
                String plainName = PLAIN.serialize(sourceDisplayName);
                display = ChatFormatCodec.deserializeStyledName(nameFormat, plainName, mini, obf);
            }
            if (hasChatPrefix) {
                display = ChatFormatCodec.deserializeForChat(chatPrefix, mini, obf).append(display);
            }
            if (hasChatSuffix) {
                display = display.append(ChatFormatCodec.deserializeForChat(chatSuffix, mini, obf));
            }
            return base.render(source, display, message, viewer);
        });
    }

    private void applyMessageFormat(final AsyncChatEvent event) {
        var cfg = plugin.config();
        if (!cfg.chatFormatEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("smpchatutils.chat.format")) {
            return;
        }
        String raw = PLAIN.serialize(event.message());
        boolean mini = cfg.miniMessageEnabled();
        boolean obf = cfg.formatObfuscated();
        String work = ChatFormatCodec.stripDisallowedObfuscation(raw, mini, obf);
        if (!ChatFormatCodec.needsFormatting(work, mini)) {
            if (!work.equals(raw)) {
                event.message(Component.text(work));
            }
            return;
        }
        event.message(ChatFormatCodec.deserializeForChat(work, mini, obf));
    }
}
