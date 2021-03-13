package apple.voltskiya.plugin.ore_regen.regen;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicBoolean;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.REGEN_INTERVAL;

public class RegenHeartbeat {
    private static boolean isHeartBeating = false;
    private static AtomicBoolean isBusy = new AtomicBoolean(false);

    public synchronized static void startBeating() {
        if (!isHeartBeating) {
            isHeartBeating = true;
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), RegenHeartbeat::tick, REGEN_INTERVAL);
        }
    }

    public synchronized static void stopBeating() {
        isHeartBeating = false;
    }

    public synchronized static void tick() {
        if (!isBusy.get()) {
            isBusy.set(true);
            new RHeartBeat().start();
        }
        if (isHeartBeating) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), RegenHeartbeat::tick, REGEN_INTERVAL);
        }
    }

    private static class RHeartBeat extends Thread {
        @Override
        public void run() {
            System.out.println("beat");
            RegenSectionManager.randomOre();
            RegenSectionManager.randomAir();
            isBusy.set(false);
        }
    }
}
