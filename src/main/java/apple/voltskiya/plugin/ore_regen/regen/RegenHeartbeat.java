package apple.voltskiya.plugin.ore_regen.regen;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;

import static apple.voltskiya.plugin.decay.PluginDecay.REGEN_INTERVAL;

public class RegenHeartbeat {
    private static boolean isHeartBeating = false;

    public synchronized static void startBeating() {
        if (!isHeartBeating) {
            isHeartBeating = true;
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), RegenHeartbeat::tick, REGEN_INTERVAL);
        }
    }

    public synchronized static void stopBeating() {
        isHeartBeating = false;
    }

    private static synchronized void tick() {
        System.out.println("beat");
        new Thread(RegenSectionManager::random).start(); // finish whenever. no rush
        if (isHeartBeating) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), RegenHeartbeat::tick, REGEN_INTERVAL);
        }
    }
}
