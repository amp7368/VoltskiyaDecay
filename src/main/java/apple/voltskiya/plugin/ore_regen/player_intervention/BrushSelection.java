package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BrushSelection {
    private final int x;
    private final int y;
    private final int z;
    private final int toolUid;
    private CenterOfGravity centerOfGravity = null;

    private static final double NEARBY_IN_SDS = 2;

    public BrushSelection(int x, int y, int z, int toolUid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.toolUid = toolUid;
    }

    public void addStandardDeviation(double sdx, double sdy, double sdz) {
        centerOfGravity = new CenterOfGravity(x, y, z, (long) sdx, (long) sdy, (long) sdz);
    }

    public int getTool() {
        return toolUid;
    }

    public boolean isInSD(Coords coord) {
        return coord.x > x - centerOfGravity.getSdx() &&
                coord.x < x + centerOfGravity.getSdx() &&
                coord.y > y - centerOfGravity.getSdy() &&
                coord.y < y + centerOfGravity.getSdy() &&
                coord.z > z - centerOfGravity.getSdz() &&
                coord.z < z + centerOfGravity.getSdz();
    }

    @Override
    public int hashCode() {
        return toolUid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BrushSelection && toolUid == ((BrushSelection) obj).toolUid;
    }

    public void addCoordsToSelection(List<Coords> blocks) {
        List<Coords.CoordsWithUID> newBlocks = new ArrayList<>(blocks.size());
        try {
            for (Coords block : blocks) {
                newBlocks.add(block.convertToWithUID());
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            DBRegen.addToSelection(toolUid, newBlocks);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
