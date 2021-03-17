package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class BrushAutoAssign {
    private static final long RADIUS_LATER_MAX = 13;
    private static final int RADIUS_MAX = 4;
    private static final Map<BrushSelection, List<Coords>> coordsToAdd = new HashMap<>();
    private static final AtomicBoolean isBusyCreatingSelection = new AtomicBoolean(false);
    // todo make this come from a yml

    protected static final Set<Material> HOST_LIKE_MATERIALS = new HashSet<>() {{
        add(Material.STONE);
        add(Material.COBBLESTONE);
        add(Material.ANDESITE);
        add(Material.GRANITE);
        add(Material.DIORITE);
    }};
    public static final Set<Material> ORE_LIKE_MATERIALS = new HashSet<>() {{
        add(Material.IRON_ORE);
        add(Material.COAL_ORE);
        add(Material.DIAMOND_ORE);
        add(Material.EMERALD_ORE);
        add(Material.LAPIS_ORE);
        add(Material.GOLD_ORE);
        add(Material.REDSTONE_ORE);
    }};
    private static final Set<Material> STONE_LIKE_MATERIALS = new HashSet<>() {{
        addAll(HOST_LIKE_MATERIALS);
        addAll(ORE_LIKE_MATERIALS);
    }};
    private static List<BlockState> nearbyBlocks = new ArrayList<>();
    private static final Object WAIT_FOR_NEARBY_BLOCKS = new Object();
    private static final AtomicBoolean shouldWait = new AtomicBoolean();

    /**
     * figure out how to assign these blocks to newly created selections
     *
     * @param blocksToAssignAll all the blocks to assign to new selections (or already in a selection)
     */
    public static void dealWithBlocks(List<Coords> blocksToAssignAll) {
        BrushSelectionBuilder newSelection = null;
        Set<BrushSelection> foundSelections = new HashSet<>();
        Iterator<Coords> coordsIterator = blocksToAssignAll.iterator();
        while (coordsIterator.hasNext()) {
            Coords coord = coordsIterator.next();

            // check that the block is not in any already found selections before we do any sql
            boolean isSelected = false;
            for (BrushSelection nearby : foundSelections) {
                if (nearby.isInSD(coord)) {
                    addCoordToSelection(nearby, coord);
                    coordsIterator.remove();
                    isSelected = true;
                    break;
                }
            }
            if (isSelected) continue;

            // get nearby selections
            List<BrushSelection> nearbySelections = null;
            try {
                nearbySelections = DBRegen.getNearbyCenters(coord, RADIUS_MAX);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            if (nearbySelections != null) {
                foundSelections.addAll(nearbySelections);
                isSelected = false;
                // if the coord should be in another selection, put it in the selection
                for (BrushSelection nearby : nearbySelections) {
                    if (nearby.isInSD(coord)) {
                        isSelected = true;
                        addCoordToSelection(nearby, coord);
                        coordsIterator.remove();
                        break;
                    }
                }
            }
            if (newSelection != null && newSelection.isInSD(coord)) {
                newSelection.add(coord);
            }
            // otherwise, create a new selection and for the coord and stop adding to selections
            if (newSelection == null && !isSelected) {
                System.out.println("create");
                isBusyCreatingSelection.set(true);
                newSelection = createSelection(coord);
                isBusyCreatingSelection.set(false);
            }
        }
        try {
            if (newSelection != null)
                newSelection.build();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        for (Map.Entry<BrushSelection, List<Coords>> selection : coordsToAdd.entrySet()) {
            selection.getKey().addCoordsToSelection(selection.getValue());
        }
    }

    private static BrushSelectionBuilder createSelection(Coords coord) {
        // x,y,z is center to look around
        final int centerX = coord.x;
        final int centerY = coord.y;
        final int centerZ = coord.z;
        final UUID worldUid = coord.worldUID;
        shouldWait.set(true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
                    // what to do to create the selection
                    World world = Bukkit.getWorld(worldUid);
                    List<BlockState> blocks = new ArrayList<>();
                    if (world == null) return;
                    final int maxX = centerX + RADIUS_MAX;
                    final int maxY = centerY + RADIUS_MAX;
                    final int maxZ = centerZ + RADIUS_MAX;
                    for (int i = centerX - RADIUS_MAX; i < maxX; i++) {
                        for (int j = centerY - RADIUS_MAX; j < maxY; j++) {
                            for (int k = centerZ - RADIUS_MAX; k < maxZ; k++) {
                                Block block = world.getBlockAt(i, j, k);
                                if (STONE_LIKE_MATERIALS.contains(block.getType())) {
                                    blocks.add(block.getState());
                                }
                            }
                        }
                    }
                    nearbyBlocks.addAll(blocks);
                    synchronized (WAIT_FOR_NEARBY_BLOCKS) {
                        shouldWait.set(false);
                        WAIT_FOR_NEARBY_BLOCKS.notify();
                    }
                }
                , 1);
        try {
            synchronized (WAIT_FOR_NEARBY_BLOCKS) {
                if (shouldWait.get()) {
                    WAIT_FOR_NEARBY_BLOCKS.wait();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            // remove already selected blocks
            DBRegen.removeAlreadySelected(nearbyBlocks);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        // find the new center of mass
        CenterOfGravity centerOfGravity = new CenterOfGravity(nearbyBlocks, coord);
        final int minX = (int) (centerOfGravity.getMeanX() - RADIUS_LATER_MAX);
        final int minY = (int) (centerOfGravity.getMeanY() - RADIUS_LATER_MAX);
        final int minZ = (int) (centerOfGravity.getMeanZ() - RADIUS_LATER_MAX);
        final int maxX = (int) (centerOfGravity.getMeanX() + RADIUS_LATER_MAX);
        final int maxY = (int) (centerOfGravity.getMeanY() + RADIUS_LATER_MAX);
        final int maxZ = (int) (centerOfGravity.getMeanZ() + RADIUS_LATER_MAX);
        nearbyBlocks = new ArrayList<>();
        shouldWait.set(true);
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
                    // what to do to create the selection
                    World world = Bukkit.getWorld(worldUid);
                    List<BlockState> blocks = new ArrayList<>();
                    if (world == null) return;
                    for (int i = minX; i <= maxX; i++) {
                        for (int j = minY; j <= maxY; j++) {
                            for (int k = minZ; k <= maxZ; k++) {
                                Block block = world.getBlockAt(i, j, k);
                                if (STONE_LIKE_MATERIALS.contains(block.getType())) {
                                    blocks.add(block.getState());
                                }
                            }
                        }
                    }
                    nearbyBlocks.addAll(blocks);
                    synchronized (WAIT_FOR_NEARBY_BLOCKS) {
                        shouldWait.set(false);
                        WAIT_FOR_NEARBY_BLOCKS.notify();
                    }
                }
                , 1);
        try {
            synchronized (WAIT_FOR_NEARBY_BLOCKS) {
                if (shouldWait.get())
                    WAIT_FOR_NEARBY_BLOCKS.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            DBRegen.removeAlreadySelected(nearbyBlocks);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        BrushSelectionBuilder selection = new BrushSelectionBuilder(nearbyBlocks);
        selection.add(coord);
        nearbyBlocks = new ArrayList<>();
        return selection;
    }


    private static void addCoordToSelection(BrushSelection selection, Coords coord) {
        coordsToAdd.putIfAbsent(selection, new ArrayList<>());
        coordsToAdd.get(selection).add(coord);
    }
}


