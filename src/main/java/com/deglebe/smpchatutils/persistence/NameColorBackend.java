package com.deglebe.smpchatutils.persistence;

import java.util.UUID;

/* per-player name-colour prefix via & or minimessage */
interface NameColorBackend {

    void load();

    String getPrefix(UUID uuid);

    void setPrefix(UUID uuid, String prefix);

    void clear(UUID uuid);

    /* release backend-only resources, no sqlite */
    void close();
}
