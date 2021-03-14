package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.INTERVENTION_EXECUTE_INTERVAL;

public class RegenPlayerInterventionMonitor {
    private static ArrayList<Coords> impactToFix = new ArrayList<Coords>();
    private static final Object WRITING_TODO_SYNC_OBJECT = new Object();
    private static final AtomicBoolean isBusy = new AtomicBoolean(false);

    public static void addTodo(Coords block) {
        impactToFix.add(block);
    }

    private static void completeTodo() {
        synchronized (WRITING_TODO_SYNC_OBJECT) {
            try {
                DBRegen.setBlock(impactToFix);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }impactToFix = new ArrayList<>();
        }
    }

    public static void heartbeat() {
        if (!isBusy.get()) {
            isBusy.set(true);
            new RegenPlayerInterventionHeartbeat().start();
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), RegenPlayerInterventionMonitor::heartbeat, INTERVENTION_EXECUTE_INTERVAL);
    }

    private static class RegenPlayerInterventionHeartbeat extends Thread {
        @Override
        public void run() {
            synchronized (WRITING_TODO_SYNC_OBJECT) {
                if (impactToFix.isEmpty()) {
                    return;
                }
            }
            completeTodo();
            isBusy.set(false);
        }
    }
}
