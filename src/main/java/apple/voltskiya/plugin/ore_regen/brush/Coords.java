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

    public Coords(int x, int y, int z, UUID worldUID, Material markerBlock, Material lastBlock) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldUID = worldUID;
        this.newBlock = markerBlock;
        this.lastBlock = lastBlock;
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
}
