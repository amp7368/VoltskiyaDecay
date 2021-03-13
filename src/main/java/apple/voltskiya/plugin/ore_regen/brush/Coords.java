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
    public UUID worldUID = null;
    public Material markerBlock;
    public Material lastBlock;
    public int myWorldUID = -1;

    public Coords(int x, int y, int z, UUID worldUID, Material markerBlock, Material lastBlock) throws SQLException {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldUID = worldUID;
        this.markerBlock = markerBlock;
        this.lastBlock = lastBlock;
        this.myWorldUID = DBUtils.getMyWorldUid(worldUID.toString());
    }

    public Coords(int x, int y, int z, int worldUID, Material markerBlock, Material lastBlock) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.myWorldUID = worldUID;
        this.markerBlock = markerBlock;
        this.lastBlock = lastBlock;
    }

    public void updateWorld() throws SQLException {
        if (worldUID == null)
            this.worldUID = DBUtils.getRealWorldUid(myWorldUID);
        else
            this.myWorldUID = DBUtils.getMyWorldUid(worldUID.toString());
    }

    public void mark(boolean marking) {
        World world = Bukkit.getWorld(worldUID);
        if (world != null) world.getBlockAt(x, y, z).setType(marking ? markerBlock : lastBlock);
    }
}
