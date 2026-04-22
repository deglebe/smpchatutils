package com.deglebe.smpchatutils.persistence;

import java.util.UUID;

/* per-player name-colour prefix via & or minimessage */
interface NameColorBackend {

    void load();

    String getPrefix(UUID uuid);

    void setPrefix(UUID uuid, String prefix);

    String getChatPrefix(UUID uuid);

    void setChatPrefix(UUID uuid, String prefix);

    String getChatSuffix(UUID uuid);

    void setChatSuffix(UUID uuid, String suffix);

    void clear(UUID uuid);

    void clearChatPrefix(UUID uuid);

    void clearChatSuffix(UUID uuid);

    /* release backend-only resources, no sqlite */
    void close();
}
