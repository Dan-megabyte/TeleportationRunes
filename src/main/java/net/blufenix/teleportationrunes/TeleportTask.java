package net.blufenix.teleportationrunes;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {

    // modify these
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int UPDATE_INTERVAL_TICKS = 2;

    // auto-calculated
    // todo de-dupe this calc
    private static final int COUNTDOWN_TICKS = COUNTDOWN_SECONDS * 20; // assumes 20 ticks per second standard server
    private final Callback callback;

    private int elapsedTicks = 0;

    private final Player player;
    private final Location potentialTeleporterLoc;

    private Teleporter teleporter;
    private Waypoint waypoint;

    private SwirlAnimation animation;
    private boolean isRunning;

    TeleportTask(Player player, Location potentialTeleporterLoc, Callback callback) {
        this.player = player;
        this.potentialTeleporterLoc = potentialTeleporterLoc;
        this.callback = callback;
    }

    public void execute() {
        if (startTeleportationTask()) {
            isRunning = true;
        } else {
            onSuccessOrFail();
        }
    }

    private boolean startTeleportationTask() {
        teleporter = TeleUtils.getTeleporterNearLocation(potentialTeleporterLoc);
        if (teleporter == null) return false;
        waypoint = TeleUtils.getWaypointForTeleporter(teleporter);
        if (waypoint == null) return false;

        // start teleport animation and timer
        animation = SwirlAnimation.getDefault();
        animation.setLocation(teleporter.loc.clone().add(Vectors.UP));

        runTaskTimer(TeleportationRunes.getInstance(), 0, UPDATE_INTERVAL_TICKS);
        return true;
    }

    @Override
    public void run() {

        // we haven't actually ticked yet, but if we pass 0 into our animation ticks
        // it will animate a single frame regardless of the interval or fake tick settings
        // TODO is there a cleaner way to fix this and remove the time shift?
        elapsedTicks += UPDATE_INTERVAL_TICKS;

        if (!playerStillAtTeleporter()) {
            player.sendMessage("You left the teleporter area. Cancelling...");
            onSuccessOrFail();
            return;
        }

        if (elapsedTicks < COUNTDOWN_TICKS) {
            animation.update(elapsedTicks);
            //if (elapsedTicks % 20 == 0) player.sendMessage("Teleporting in " + ((COUNTDOWN_TICKS-elapsedTicks)/20) + "...");
        } else {
            TeleUtils.attemptTeleport(player, teleporter.loc, waypoint);
            onSuccessOrFail();
        }

    }

    private void onSuccessOrFail() {
        if (isRunning) this.cancel();
        if (callback != null) {
            callback.onFinished();
        }
    }

    private boolean playerStillAtTeleporter() {
        return player.getLocation().distance(potentialTeleporterLoc) < 2;
    }

    public static abstract class Callback {
        abstract void onFinished();
    }
}