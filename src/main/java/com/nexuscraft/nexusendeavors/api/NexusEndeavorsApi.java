package com.nexuscraft.nexusendeavors.api;

import java.util.UUID;

/**
 * Small, stable public surface the REST of the Nexus family reports real progress to, without
 * needing NexusEndeavors as a compile dependency -- same shape as NexusHousesApi/NexusPulseApi's
 * own javadoc documents:
 *
 * <pre>
 *   RegisteredServiceProvider&lt;NexusEndeavorsApi&gt; reg =
 *       Bukkit.getServicesManager().getRegistration(NexusEndeavorsApi.class);
 * </pre>
 *
 * A plugin without this interface on its own classpath reaches it via reflection --
 * {@code Class.forName("com.nexuscraft.nexusendeavors.api.NexusEndeavorsApi")}, pull the
 * registration off {@code Bukkit.getServicesManager()}, invoke {@code reportProgress} by
 * {@code Method.invoke} -- the same {@code Class.forName} + {@code ServicesManager} pattern this
 * whole project uses everywhere else (see NexusHouses' own PulseBridge for the canonical outbound
 * shape this plugin's callers copy). This is the INBOUND direction of that same pattern: instead of
 * NexusEndeavors reaching into three other plugins' internals to guess when something happened, each
 * of those plugins reports it here at the one moment they already know it happened for real --
 * NexusDungeons on a dungeon kill, NexusCraftersGuild on a forged item, NexusHouses on a treasury
 * donation. Safe to call whether or not any objective is currently listening for that key --
 * unmatched progress reports are just quietly discarded.
 */
public interface NexusEndeavorsApi {

    /**
     * Reports that {@code playerId} just made {@code amount} of progress toward objectives keyed
     * {@code objectiveKey} (a plain string both sides agree on by convention -- see this plugin's
     * README for the current key list, e.g. {@code "dungeon.kill"}, {@code "forge.craft"},
     * {@code "house.donate"}). {@code playerName} is a best-effort display name, used only if this
     * is the first time this player has been seen; pass the caller's own best guess (a null is
     * fine, it just means a later "/endeavors board" for a player Endeavors has never otherwise
     * seen shows their raw UUID until they're seen by something that does have a name).
     */
    void reportProgress(UUID playerId, String playerName, String objectiveKey, int amount);
}
