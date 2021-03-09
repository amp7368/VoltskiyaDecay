package apple.voltskiya.plugin.ore_regen.brush;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionManager;
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
    public static final int BLOCKS_TO_UPDATE_AT_ONCE_INTERVAL = 5;

    public static final Object WAIT_TO_UPDATE_OBJECT = new Object();

    private static final int BRUSH_USAGE_COUNT = 100000;
    private static final int BRUSH_USAGE_INTERVAL = 10;

    public static boolean isBusy = false;

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

    public static void pruneHeartbeat() {
        new PruneHeartbeat().start();
    }

    public void use(@NotNull Block hitBlock, @NotNull Action action) {
        lastUsed = System.currentTimeMillis();
        switch (brushType) {
            case CUBE:
                int xMin = hitBlock.getX() - radius + 1;
                int yMin = hitBlock.getY() - radius + 1;
                int zMin = hitBlock.getZ() - radius + 1;
                int xMax = hitBlock.getX() + radius;
                int yMax = hitBlock.getY() + radius;
                int zMax = hitBlock.getZ() + radius;
                World world = hitBlock.getWorld();
                addCubeBlocks(xMin, yMin, zMin, xMax, yMax, zMax, xMin, yMin, zMin, world);
                break;
            case SPHERE:
                //todo
                break;
            case CORNER_SELECT:
                //todo
                break;
        }
    }

    private void addCubeBlocks(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int currentX, int currentY, int currentZ, World world) {
        UUID worldUid = world.getUID();
        List<Coords> coords = new ArrayList<>();
        int count = 0;
        boolean isFinished = true;
        boolean firstTime = true;
        addLoop:
        for (int x = xMin; x < xMax; x++) {
            if (firstTime) x = currentX;
            for (int y = yMin; y < yMax; y++) {
                if (firstTime) y = currentY;
                for (int z = zMin; z < zMax; z++) {
                    if (firstTime) {
                        firstTime = false;
                        z = currentZ;
                    }
                    Material materialThere = world.getBlockAt(x, y, z).getType();
                    if (this.hostBlocks.contains(materialThere))
                        coords.add(new Coords(x, y, z, worldUid, markerBlock, materialThere));
                    if (++count == BRUSH_USAGE_COUNT) {
                        currentX = x;
                        currentY = y;
                        currentZ = z;
                        isFinished = false;
                        break addLoop;
                    }
                }
            }
        }
        BrushExecution.addTodo(uid, coords);
        if (!isFinished) {
            int finalCurrentX = currentX;
            int finalCurrentY = currentY;
            int finalCurrentZ = currentZ;
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    VoltskiyaPlugin.get(),
                    () -> addCubeBlocks(xMin, yMin, zMin, xMax, yMax, zMax, finalCurrentX, finalCurrentY, finalCurrentZ, world),
                    BRUSH_USAGE_INTERVAL
            );
        }
    }

    public void markAll(boolean marking, Runnable afterFinish) {
        synchronized (WAIT_TO_UPDATE_OBJECT) {
            while (isBusy) {
                try {
                    WAIT_TO_UPDATE_OBJECT.wait();
                } catch (InterruptedException ignored) {
                }
            }
            isBusy = true;
        }
        List<Coords> coordsToUnmark;
        try {
            coordsToUnmark = DBRegen.getMarking(uid, marking);
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
        markDivided(dividedCoordsToMark, marking, afterFinish);
    }

    private void markDivided(List<List<Coords>> dividedCoordsTomark, boolean marking, Runnable afterFinish) {
        if (dividedCoordsTomark.isEmpty()) {
            afterFinish.run();
            synchronized (WAIT_TO_UPDATE_OBJECT) {
                isBusy = false;
                WAIT_TO_UPDATE_OBJECT.notify();
            }
            return;
        }

        // make this happen on the main thread
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
            if (!dividedCoordsTomark.isEmpty())
                for (Coords coords : dividedCoordsTomark.remove(0)) coords.mark(marking);
        }, 0);

        // we're not on the main thread
        try {
            Thread.sleep(BLOCKS_TO_UPDATE_AT_ONCE_INTERVAL);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.markDivided(dividedCoordsTomark, marking, afterFinish);
    }

    public int getRadius() {
        return radius;
    }

    private static class PruneHeartbeat extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(ActiveBrush.PRUNE_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("prune");
                ActiveBrush.prune();
            }
        }
    }
}
