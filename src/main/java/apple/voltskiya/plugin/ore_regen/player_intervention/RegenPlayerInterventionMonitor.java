package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import apple.voltskiya.plugin.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.INTERVENTION_EXECUTE_INTERVAL;

public class RegenPlayerInterventionMonitor {
    private static ArrayList<Pair<Coords, Long>> impactToFix = new ArrayList<>();
    private static final Object WRITING_TODO_SYNC_OBJECT = new Object();
    private static final AtomicBoolean isBusy = new AtomicBoolean(false);

    public static void addTodo(Coords block) {
        new Thread(() -> {
            synchronized (WRITING_TODO_SYNC_OBJECT) {
                impactToFix.add(new Pair<>(block, System.currentTimeMillis()));
            }
        }).start();
    }

    private static void completeTodo() {
        synchronized (WRITING_TODO_SYNC_OBJECT) {
            List<Coords> blocksToAssign = null;
            Map<Coords, Long> blocksToAssignDistict = getBlocksDistinct();
            try {
                blocksToAssign = DBRegen.setBlock(blocksToAssignDistict.keySet());
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            if (blocksToAssign == null) {
                return;
            }
            blocksToAssign.removeIf(block -> block.lastBlock == Material.AIR); // remove if we're placing blocks

            if (blocksToAssign.isEmpty()) {
                impactToFix = new ArrayList<>();
                return;
            }
            try {
                blocksToAssign = DBRegen.getNotAlreadyAssignedFromList(blocksToAssign);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            for (Coords block : blocksToAssign) {
                System.out.println("remove  " + block.toString());
            }
            BrushAutoAssign.dealWithBlocks(blocksToAssign);
            impactToFix = new ArrayList<>();
            for (Coords block : blocksToAssign) {
                System.out.println("add back " + block.toString());
                impactToFix.add(new Pair<>(block, blocksToAssignDistict.get(block)));
            }
        }
    }

    private static Map<Coords, Long> getBlocksDistinct() {
        synchronized (WRITING_TODO_SYNC_OBJECT) {
            Map<Coords, List<Pair<Coords, Long>>> distinctMap = new HashMap<>();
            for (Pair<Coords, Long> block : RegenPlayerInterventionMonitor.impactToFix) {
                distinctMap.compute(block.getKey(), (k, v) -> {
                    if (v == null)
                        return new ArrayList<>() {{
                            add(block);
                        }};
                    else {
                        v.add(block);
                        return v;
                    }
                });
            }
            Map<Coords, Long> distinct = new HashMap<>();
            for (List<Pair<Coords, Long>> block : distinctMap.values()) {
                block.sort(((o1, o2) -> {
                    long diff = o1.getValue() - o2.getValue();
                    if (diff > 0) return 1;
                    else if (diff == 0) return 0;
                    else return -1;
                }));
                Coords b = block.get(0).getKey();
                b.newBlock = block.get(block.size() - 1).getKey().newBlock;
                distinct.put(b, block.get(0).getValue());
            }
            return distinct;
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
            System.out.println("beatIntervention");
            synchronized (WRITING_TODO_SYNC_OBJECT) {
                if (impactToFix.isEmpty()) {
                    isBusy.set(false);
                    return;
                }
            }
            completeTodo();
            isBusy.set(false);
        }
    }
}
