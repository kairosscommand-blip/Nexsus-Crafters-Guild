package com.nexuscraft.nexusendeavors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Reports one minute of "playtime.minutes" progress for every currently-online player, once a
 *  minute -- the one objective key with no real Bukkit event of its own to hang off of. */
final class PlaytimeTracker implements Runnable {

    private final EndeavorProgressEngine engine;

    PlaytimeTracker(EndeavorProgressEngine engine) {
        this.engine = engine;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            engine.reportProgress(player.getUniqueId(), player.getName(), "playtime.minutes", 1);
        }
    }
}
