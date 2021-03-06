package apple.voltskiya.plugin.ore_regen.brush;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class ActiveBrush {
    public static final long PRUNE_PERIOD = 1000 * 60 * 10;
    private static final long MAX_RECENTLY_USED_TIME = 1000 * 60 * 10;
    public static final int BLOCKS_TO_UPDATE_AT_ONCE = 500;

    private final RegenConfigInstance.BrushType brushType;
    private final int radius;
    private final long uid;
    private final Material markerBlock;
    private final Set<Material> hostBlocks;
    private long lastUsed = System.currentTimeMillis();

    private final static Map<Long, ActiveBrush> activeBrushes = new HashMap<>();

    public ActiveBrush(long uid, RegenConfigInstance.BrushType brushType, int radius, Material markerBlock, Map<Material, Integer> hostBlocks) {
        this.brushType = brushType;
        this.radius = radius;
        this.uid = uid;
        this.markerBlock = markerBlock;
        this.hostBlocks = new HashSet<>(hostBlocks.keySet());
    }

    public synchronized static void prune() {
        activeBrushes.entrySet().removeIf(longActiveBrushEntry -> System.currentTimeMillis() - longActiveBrushEntry.getValue().lastUsed > MAX_RECENTLY_USED_TIME);
    }

    public synchronized static ActiveBrush getBrush(long uid) {
        ActiveBrush brush = activeBrushes.get(uid);
        if (brush == null) {
            try {
                brush = DBRegen.getBrush(uid);
            } catch (SQLException throwables) {
                //todo
                throwables.printStackTrace();
                return null;
            }
            if (brush == null) return null;
            ActiveBrush.addBrush(brush);
        }
        return brush;
    }

    public synchronized static void addBrush(ActiveBrush brush) {
        activeBrushes.put(brush.uid, brush);
    }

    public void use(@NotNull Block hitBlock, @NotNull Action action) {
        lastUsed = System.currentTimeMillis();
        switch (brushType) {
            case CUBE:
                List<Coords> coords = new ArrayList<>();
                int xMin = hitBlock.getX() - radius + 1;
                int yMin = hitBlock.getY() - radius + 1;
                int zMin = hitBlock.getZ() - radius + 1;
                int xMax = hitBlock.getX() + radius;
                int yMax = hitBlock.getY() + radius;
                int zMax = hitBlock.getZ() + radius;
                World world = hitBlock.getWorld();
                UUID worldUid = world.getUID();
                for (int x = xMin; x < xMax; x++) {
                    for (int y = yMin; y < yMax; y++) {
                        for (int z = zMin; z < zMax; z++) {
                            Material materialThere = world.getBlockAt(x, y, z).getType();
                            if (this.hostBlocks.contains(materialThere))
                                coords.add(new Coords(x, y, z, worldUid, markerBlock, materialThere));
                        }
                    }
                }
                BrushExecution.addTodo(uid, coords);
                break;
            case SPHERE:
                //todo
                break;
            case CORNER_SELECT:
                //todo
                break;
        }
    }

    public void markAll(boolean marking) {
        List<Coords> coordsToUnmark;
        try {
            coordsToUnmark = DBRegen.marking(uid,marking);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return;
        }
        List<List<Coords>> dividedCoordsToMark = new ArrayList<>();
        int i = 0;
        List<Coords> current = new ArrayList<>(BLOCKS_TO_UPDATE_AT_ONCE);
        for (Coords coords : coordsToUnmark) {
            if (++i % BLOCKS_TO_UPDATE_AT_ONCE == 0) {
                dividedCoordsToMark.add(current);
                current = new ArrayList<>(BLOCKS_TO_UPDATE_AT_ONCE);
            }
            current.add(coords);
        }
        dividedCoordsToMark.add(current);
        markDivided(dividedCoordsToMark,marking);
    }

    private void markDivided(List<List<Coords>> dividedCoordsTomark,boolean marking) {
        if (dividedCoordsTomark.isEmpty()) return;
        for (Coords coords : dividedCoordsTomark.remove(0)) coords.mark(marking);
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> this.markDivided(dividedCoordsTomark,marking), 5);
    }
}
