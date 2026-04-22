package com.deglebe.smpchatutils.persistence;

import java.util.Set;
import java.util.UUID;

/* player a ignores player b via uuid */
interface IgnoreListBackend {

    void load();

    boolean isIgnoring(UUID ignorer, UUID ignored);

    void addIgnore(UUID ignorer, UUID ignored);

    void removeIgnore(UUID ignorer, UUID ignored);

    /* snapshot of who ignorer is ignoring */
    Set<UUID> getIgnored(UUID ignorer);

    void close();
}
