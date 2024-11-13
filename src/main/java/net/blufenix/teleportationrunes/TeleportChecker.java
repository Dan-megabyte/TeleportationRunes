package net.blufenix.teleportationrunes;

import net.blufenix.common.Log;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class TeleportChecker extends BukkitRunnable {

    private static final Queue<TeleportTask> taskPool = new LinkedList<>();

    public static BukkitTask start() {
        return new TeleportChecker().runTaskTimer(TeleportationRunes.getInstance(), 0, 20);
    }

    @Override
    public void run() {
        Collection<? extends Player> players = TeleportationRunes.getInstance().getServer().getOnlinePlayers();
        for (final Player p: players) {
            TeleportTask task = pollTask();
            try {
                if (task == null) {
                    // If no task is available in the pool, create a new one
                    task = new TeleportTask(p, p.getLocation().add(Vectors.DOWN), null);
                } else {
                    // Reset or initialize the task for reuse
                    task.reset(p, p.getLocation().add(Vectors.DOWN));
                }
                task.execute();
            } catch (Throwable t) {
                Log.e("whoops!", t);
            } finally {
                // Add the task back to the pool for reuse
                recycleTask(task);
            }
        }
    }

    /**
     * Polls a task from the pool or creates a new one if none are available.
     *
     * @return A reusable TeleportTask or null if the pool is empty.
     */
    private TeleportTask pollTask() {
        synchronized (taskPool) {
            return taskPool.poll();
        }
    }

    /**
     * Recycles a task back into the pool for later use.
     *
     * @param task The TeleportTask to be recycled.
     */
    private void recycleTask(TeleportTask task) {
        synchronized (taskPool) {
            taskPool.offer(task); // Add the task back to the pool
        }
    }

}