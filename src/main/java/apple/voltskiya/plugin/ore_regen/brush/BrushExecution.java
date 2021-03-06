package apple.voltskiya.plugin.ore_regen.brush;


import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrushExecution {
    private static Map<Long, List<Coords>> toolToImpact = new HashMap<>();

    public synchronized static void addTodo(long uid, List<Coords> coords) {
        List<Coords> last = toolToImpact.putIfAbsent(uid, coords);
        if (last != null) last.addAll(coords);
    }

    public synchronized static void completeTodo() {
        final Map<Long, List<Coords>> todo = toolToImpact;
        toolToImpact = new HashMap<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> markBlocks(todo,true), 0);
    }

    public static void markBlocks(Map<Long, List<Coords>> allCoords,boolean marking) {
        for (List<Coords> coords : allCoords.values()) {
            for (Coords coord : coords) {
                coord.mark(marking);
            }
        }
        try {
            DBRegen.setMarked(allCoords,marking);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
