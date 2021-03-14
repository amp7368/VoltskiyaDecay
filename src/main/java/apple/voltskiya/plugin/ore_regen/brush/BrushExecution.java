package apple.voltskiya.plugin.ore_regen.brush;


import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.*;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.BRUSH_EXECUTE_INTERVAL;

public class BrushExecution {
    private static final int BLOCKS_PER_INTERVAL = 10000;
    private static final double DROP_IN_TPS = 3;
    private static Map<Long, List<Coords.CoordsWithUID>> toolToImpact = new HashMap<>();

    private static final Object TOOL_TO_IMPACT_SYNC_OBJECT = new Object();

    private static final Object WRITING_BUSY_SYNC_OBJECT = new Object();
    private static boolean isBusy = false;

    public static void addTodo(long uid, List<Coords.CoordsWithUID> coords) {
        synchronized (TOOL_TO_IMPACT_SYNC_OBJECT) {
            List<Coords.CoordsWithUID> last = toolToImpact.putIfAbsent(uid, coords);
            if (last != null) last.addAll(coords);
        }
    }

    public static void completeTodo() {
        final Map<Long, List<Coords.CoordsWithUID>> todo = new HashMap<>();
        synchronized (TOOL_TO_IMPACT_SYNC_OBJECT) {
            Set<Long> keys = toolToImpact.keySet();
            Iterator<Long> keysIterator = keys.iterator();
            Long key = keysIterator.next();
            List<Coords.CoordsWithUID> value = toolToImpact.get(key);
            for (int i = 0; i < BLOCKS_PER_INTERVAL; ) {
                if (value.size() < BLOCKS_PER_INTERVAL - i) {
                    i += value.size();
                    todo.put(key, value);
                    keysIterator.remove();
                    if (keysIterator.hasNext())
                        key = keysIterator.next();
                    else
                        break; // our job is done
                } else {
                    List<Coords.CoordsWithUID> todoFromKey = new ArrayList<>();
                    todo.putIfAbsent(key, todoFromKey);
                    while (i++ < BLOCKS_PER_INTERVAL) {
                        todo.get(key).add(value.remove(0));
                    }
                }
            }
            if (todo.isEmpty()) {
                if (toolToImpact.isEmpty())
                    toolToImpact = new HashMap<>();
                synchronized (WRITING_BUSY_SYNC_OBJECT) {
                    isBusy = false;
                    return;
                }
            }
            // schedule this so I get back on the main thread
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> markBlocks(todo, true), 0);
        }
        try {
            for (Map.Entry<Long, List<Coords.CoordsWithUID>> tool : todo.entrySet()) {
                for (Coords.CoordsWithUID coords : tool.getValue()) {
                    coords.updateBlockAndWorld();
                }
            }
            DBRegen.setMarked(todo, true);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void markBlocks(Map<Long, List<Coords.CoordsWithUID>> allCoords, boolean marking) {
        for (List<Coords.CoordsWithUID> coords : allCoords.values()) {
            for (Coords.CoordsWithUID coord : coords) {
                coord.mark(marking);
            }
        }

        // new thread because I don't want the main thread to be waiting on the database to finish writing
        new Thread(() -> {
            synchronized (WRITING_BUSY_SYNC_OBJECT) {
                isBusy = false;
            }
        }).start();

    }

    public static void heartbeat() {
        new BrushExecutionHeartbeat().start();
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), BrushExecution::heartbeat, BRUSH_EXECUTE_INTERVAL);
    }


    private static class BrushExecutionHeartbeat extends Thread {
        @Override
        public void run() {
            synchronized (WRITING_BUSY_SYNC_OBJECT) {
                if (isBusy) {
                    System.out.println("isbusy");
                    return;
                }
                if (toolToImpact.isEmpty()) {
                    return;
                }
                double[] tps = Bukkit.getTPS();
                if (tps[0] < tps[2] - DROP_IN_TPS) {
                    return;
                }
                isBusy = true;
            }
            BrushExecution.completeTodo();
        }
    }
}
