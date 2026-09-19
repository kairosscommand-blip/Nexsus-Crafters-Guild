package com.nexuscraft.nexuscraftersguild;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The live set of world events -- in-memory only, on purpose (see {@link WorldEvent}'s own
 * comment). Shared between {@link GuildEventScheduler} (which creates, expires, and completes
 * events) and {@link GuildEventListener} (which reacts to combat/looting against them); neither
 * owns it exclusively, so it lives here as its own small class instead of being private state on
 * either.
 */
final class GuildEventRegistry {

    private final Map<UUID, WorldEvent> events = new LinkedHashMap<>();

    void add(WorldEvent event) {
        events.put(event.id, event);
    }

    WorldEvent byId(UUID id) {
        return id == null ? null : events.get(id);
    }

    void remove(UUID id) {
        events.remove(id);
    }

    Collection<WorldEvent> all() {
        return events.values();
    }

    List<WorldEvent> active() {
        List<WorldEvent> result = new ArrayList<>();
        for (WorldEvent event : events.values()) {
            if (event.status == WorldEvent.Status.ACTIVE) {
                result.add(event);
            }
        }
        return result;
    }

    int activeCount() {
        return active().size();
    }
}
