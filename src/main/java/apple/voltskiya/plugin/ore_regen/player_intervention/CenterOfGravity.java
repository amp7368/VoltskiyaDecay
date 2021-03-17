package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.utils.Triple;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CenterOfGravity {
    private long meanX;
    private long meanY;
    private long meanZ;
    private long sdx;
    private long sdy;
    private long sdz;


    public CenterOfGravity(Collection<BlockState> blocks, Coords... coords) throws IllegalArgumentException {
        List<Triple<Integer, Integer, Integer>> xyz = new ArrayList<>(blocks.size());
        for (BlockState block : blocks) xyz.add(new Triple<>(block.getX(), block.getY(), block.getZ()));
        for (Coords coord : coords) xyz.add(new Triple<>(coord.x, coord.y, coord.z));
        init(xyz);
    }


    public CenterOfGravity(Set<Coords> myBlocks) {
        List<Triple<Integer, Integer, Integer>> xyz = new ArrayList<>(myBlocks.size());
        for (Coords block : myBlocks) xyz.add(new Triple<>(block.x, block.y, block.z));
        init(xyz);
    }

    public CenterOfGravity(long meanX, long meanY, long meanZ, long sdx, long sdy, long sdz) {
        this.meanX = meanX;
        this.meanY = meanY;
        this.meanZ = meanZ;
        this.sdx = sdx;
        this.sdy = sdy;
        this.sdz = sdz;
    }

    private void init(Collection<Triple<Integer, Integer, Integer>> blocks) {
        final int count = blocks.size();
        if (count == 0) throw new IllegalArgumentException("The center of gravity should always have at least 1 block");
        this.meanX = 0;
        this.meanY = 0;
        this.meanZ = 0;
        for (Triple<Integer, Integer, Integer> block : blocks) {
            this.meanX += block.getX();
            this.meanY += block.getY();
            this.meanZ += block.getZ();
        }
        this.meanX /= count;
        this.meanY /= count;
        this.meanZ /= count;

        this.sdx = 0;
        this.sdy = 0;
        this.sdz = 0;
        for (Triple<Integer, Integer, Integer> block : blocks) {
            long diff = this.meanX - block.getX();
            this.sdx += diff * diff;
            diff = this.meanY - block.getY();
            this.sdy += diff * diff;
            diff = this.meanZ - block.getZ();
            this.sdz += diff * diff;
        }
        this.sdx/=count;
        this.sdy/=count;
        this.sdz/=count;
        this.sdx = (long) Math.sqrt(this.sdx);
        this.sdy = (long) Math.sqrt(this.sdy);
        this.sdz = (long) Math.sqrt(this.sdz);
    }


    public long getMeanX() {
        return meanX;
    }

    public long getMeanY() {
        return meanY;
    }

    public long getMeanZ() {
        return meanZ;
    }

    public long getSdx() {
        return sdx * 4;
    }

    public long getSdy() {
        return sdy * 4;
    }

    public long getSdz() {
        return sdz * 4;
    }

    public boolean isInSD(Coords coord) {
        return coord.x > getMeanX() - getSdx() &&
                coord.x < getMeanX() + getSdx() &&
                coord.y > getMeanY() - getSdy() &&
                coord.y < getMeanY() + getSdy() &&
                coord.z > getMeanZ() - getSdz() &&
                coord.z < getMeanZ() + getSdz();
    }
}
