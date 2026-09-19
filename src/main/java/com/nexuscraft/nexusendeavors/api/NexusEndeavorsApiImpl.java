package com.nexuscraft.nexusendeavors.api;

import com.nexuscraft.nexusendeavors.EndeavorProgressEngine;

import java.util.UUID;

/** Thin adapter from the public {@link NexusEndeavorsApi} surface onto the real
 *  {@link EndeavorProgressEngine} -- same "public api package, thin adapter onto the real internal
 *  class" shape as NexusHouses' own NexusHousesApiImpl (which adapts onto HouseRegistry directly). */
public final class NexusEndeavorsApiImpl implements NexusEndeavorsApi {

    private final EndeavorProgressEngine engine;

    public NexusEndeavorsApiImpl(EndeavorProgressEngine engine) {
        this.engine = engine;
    }

    @Override
    public void reportProgress(UUID playerId, String playerName, String objectiveKey, int amount) {
        engine.reportProgress(playerId, playerName, objectiveKey, amount);
    }
}
