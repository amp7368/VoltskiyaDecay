package apple.voltskiya.plugin.ore_regen.brush;

import apple.voltskiya.plugin.ore_regen.sql.DBUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.UUID;

public class Coords {
    public int x;
    public int y;
    public int z;
    public Material newBlock;
    public Material lastBlock;
    public UUID worldUID;

    public Coords(int x, int y, int z, UUID worldUID, Material newBlock, Material lastBlock) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldUID = worldUID;
        this.newBlock = newBlock;
        this.lastBlock = lastBlock;
    }

    public CoordsWithUID convertToWithUID() throws SQLException {
        return new CoordsWithUID(this);
    }

    public static class CoordsWithUID extends Coords {
        public int myLastBlockUid = -1;
        public int myWorldUID = -1;

        public CoordsWithUID(int x, int y, int z, UUID worldUID, Material markerBlock, Material lastBlock) throws SQLException {
            super(x, y, z, worldUID, markerBlock, lastBlock);
            this.myWorldUID = DBUtils.getMyWorldUid(worldUID.toString());
        }

        public CoordsWithUID(int x, int y, int z, int worldUID, Material markerBlock, int lastBlock) {
            super(x, y, z, null, markerBlock, null);
            this.myWorldUID = worldUID;
            this.myLastBlockUid = lastBlock;
        }

        private CoordsWithUID(Coords block) throws SQLException {
            super(block.x, block.y, block.z, block.worldUID, block.newBlock, block.lastBlock);
            updateBlockAndWorld();
        }

        public void updateBlockAndWorld() throws SQLException {
            if (worldUID == null)
                this.worldUID = DBUtils.getRealWorldUid(myWorldUID);
            else
                this.myWorldUID = DBUtils.getMyWorldUid(worldUID.toString());
            if (lastBlock == null)
                this.lastBlock = DBUtils.getBlockName(myLastBlockUid);
            else
                this.myLastBlockUid = DBUtils.getMyBlockUid(lastBlock);
        }

        public void mark(boolean marking) {
            World world = Bukkit.getWorld(worldUID);
            if (world != null) world.getBlockAt(x, y, z).setType(marking ? newBlock : lastBlock);
        }
    }

    @Override
    public int hashCode() {
        long hash = 0;
        hash += x;
        hash += y;
        hash += z;
        hash += worldUID.hashCode();
        return (int) (hash % Integer.MAX_VALUE);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coords) {
            Coords other = (Coords) obj;
            return x == other.x && y == other.y && z == other.z && worldUID.equals(other.worldUID);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s -> [%d,%d,%d] -> %s", lastBlock.name(), x, y, z, newBlock.name());
    }
}
