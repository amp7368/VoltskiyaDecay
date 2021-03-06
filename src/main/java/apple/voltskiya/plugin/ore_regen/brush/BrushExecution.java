package apple.voltskiya.plugin.ore_regen.brush;


import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;

import java.util.Collection;
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
        Collection<List<Coords>> todoWorld = todo.values();
        toolToImpact = new HashMap<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> markBlock(todoWorld), 0);
    }

    public static void markBlock(Collection<List<Coords>> allCoords) {
        for (List<Coords> coords : allCoords) {
            for (Coords coord : coords) {
                coord.mark();
            }
        }
    }
}
