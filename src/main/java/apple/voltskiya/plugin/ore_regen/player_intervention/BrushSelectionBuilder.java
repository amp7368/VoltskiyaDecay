package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import apple.voltskiya.plugin.ore_regen.sql.DBUtils;
import org.bukkit.Material;
import org.bukkit.block.BlockState;

import java.sql.SQLException;
import java.util.*;

import static apple.voltskiya.plugin.DBNames.*;
import static apple.voltskiya.plugin.DBNames.Regen.*;
import static apple.voltskiya.plugin.ore_regen.player_intervention.BrushAutoAssign.HOST_LIKE_MATERIALS;
import static apple.voltskiya.plugin.ore_regen.player_intervention.BrushAutoAssign.ORE_LIKE_MATERIALS;

public class BrushSelectionBuilder {
    private final Set<Coords> myBlocks = new HashSet<>();
    private CenterOfGravity centerOfGravity;

    private final List<String> toolToDensity = new ArrayList<>();
    private final List<String> toolToVein = new ArrayList<>();
    private final List<String> toolToHost = new ArrayList<>();
    private final List<String> blocksSql = new ArrayList<>();

    public BrushSelectionBuilder(List<BlockState> myBlocks) {
        if (!myBlocks.isEmpty())
            this.centerOfGravity = new CenterOfGravity(myBlocks);
        else this.centerOfGravity = null;
        for (BlockState block : myBlocks) {
            this.myBlocks.add(new Coords(
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    block.getWorld().getUID(),
                    block.getType(),
                    block.getType()
            ));
        }
    }

    public boolean isInSD(Coords coord) {
        return centerOfGravity.isInSD(coord);
    }

    public void add(Coords coord) {
        myBlocks.remove(coord); // remove so I can replace it with a more accurate Coord (The memory of what was there before)
        myBlocks.add(coord);
        if (centerOfGravity == null) centerOfGravity = new CenterOfGravity(myBlocks);
    }

    public void build() throws SQLException {
        Map<Material, Integer> toolToDensityMap = new HashMap<>();
        Map<Material, List<Integer>> toolToVeinMap = calculateVeinSizes(myBlocks);
        Map<Material, Integer> toolToHostMap = new HashMap<>();
        for (Coords block : myBlocks) {
            toolToDensityMap.compute(block.lastBlock, (m, c) -> c == null ? 1 : c + 1);
            if (HOST_LIKE_MATERIALS.contains(block.lastBlock)) {
                toolToHostMap.compute(block.lastBlock, (m, c) -> c == null ? 1 : c + 1);
            }
        }

        for (Map.Entry<Material, Integer> density : toolToDensityMap.entrySet()) {
            toolToDensity.add(String.format(
                    "INSERT INTO %s (%s, %s, %s)\n" +
                            "VALUES (%%d, %d, %d)",
                    TOOL_TO_DENSITY_TABLE,
                    TOOL_UID,
                    BLOCK_NAME,
                    BLOCK_COUNT,
                    DBUtils.getMyBlockUid(density.getKey()),
                    density.getValue()
            ));
        }
        for (Map.Entry<Material, List<Integer>> vein : toolToVeinMap.entrySet()) {
            int i = 1;
            final int material = DBUtils.getMyBlockUid(vein.getKey());
            for (Integer count : vein.getValue()) {
                toolToVein.add(String.format(
                        "INSERT INTO %s (%s, %s, %s, %s)\n" +
                                "VALUES (%%d,%d,%d,%d)",
                        TOOL_TO_VEIN_BLOCK_TABLE,
                        TOOL_UID, BLOCK_NAME, VEIN_INDEX, BLOCK_COUNT,
                        material, i++, count
                ));
            }
        }
        for (Map.Entry<Material, Integer> host : toolToHostMap.entrySet()) {
            toolToHost.add(String.format(
                    "INSERT INTO %s (%s, %s, %s)\n" +
                            "VALUES (%%d,%d,%d)",
                    TOOL_TO_HOST_BLOCK_TABLE,
                    TOOL_UID,
                    BLOCK_NAME,
                    BLOCK_COUNT,
                    DBUtils.getMyBlockUid(host.getKey()),
                    host.getValue()
            ));
        }
        for (Coords block : myBlocks) {
            blocksSql.add(String.format(
                    "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s)\n" +
                            "VALUES (%%d,%d,%d,%d,%d,%b,%b,%d) ON CONFLICT DO NOTHING",
                    SECTION_TO_BLOCK_TABLE, TOOL_UID, X, Y, Z, PlayerBlock.WORLD_UUID, IS_MARKED, IS_ORE, BLOCK_NAME,
                    block.x, block.y, block.z, DBUtils.getMyWorldUid(block.worldUID.toString()),
                    false, ORE_LIKE_MATERIALS.contains(block.lastBlock), DBUtils.getMyBlockUid(block.newBlock)
            ));
        }
        try {
            DBRegen.insertNewSelection(this);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private static Map<Material, List<Integer>> calculateVeinSizes(Set<Coords> blocksOld) {
        Map<Coords, Coords> blocks = new HashMap<>();
        for (Coords block : blocksOld) blocks.put(block, block); // make a copy
        Iterator<Map.Entry<Coords, Coords>> blocksIterator = blocks.entrySet().iterator();
        while (blocksIterator.hasNext()) {
            if (!ORE_LIKE_MATERIALS.contains(blocksIterator.next().getKey().lastBlock))
                blocksIterator.remove();
        }

        List<List<Coords>> groups = new ArrayList<>();
        blocksIterator = blocks.entrySet().iterator();
        while (blocksIterator.hasNext()) {
            Coords block = blocksIterator.next().getKey();
            blocksIterator.remove();
            groups.add(findNearby(blocks, block));
            blocksIterator = blocks.entrySet().iterator();
        }
        Map<Material, List<Integer>> veinSizes = new HashMap<>();
        for (List<Coords> group : groups) {
            final Material groupMaterial = group.get(0).lastBlock;
            veinSizes.putIfAbsent(groupMaterial, new ArrayList<>());
            veinSizes.get(groupMaterial).add(group.size());
        }
        return veinSizes;
    }

    private static List<Coords> findNearby(Map<Coords, Coords> blocks, Coords block) {
        List<Coords> group = new ArrayList<>();
        group.add(block);
        for (int i = 0; i < 3; i++) {
            block.x++;
            for (int j = 0; j < 3; j++) {
                block.y++;
                for (int k = 0; k < 3; k++) {
                    block.z++;
                    if (blocks.containsKey(block)) {
                        final Coords blockToCheck = blocks.get(block);
                        if (blockToCheck.lastBlock == block.lastBlock) {
                            blocks.remove(block);
                            group.addAll(findNearby(blocks, blockToCheck));
                        }
                    }
                }
                block.z-=3;
            }
            block.y-=3;
        }
        block.x-=3;
        return group;
    }


    public List<String> getToolToDensity() {
        return toolToDensity;
    }

    public List<String> getToolToVein() {
        return toolToVein;
    }

    public List<String> getToolToHost() {
        return toolToHost;
    }

    public List<String> getBlocks() {
        return blocksSql;
    }
}
