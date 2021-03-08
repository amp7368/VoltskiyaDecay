package apple.voltskiya.plugin.ore_regen.brush;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

public class Coords {
    public int x;
    public int y;
    public int z;
    public UUID worldUID;
    public Material markerBlock;
    public Material lastBlock;

    public Coords(int x, int y, int z, UUID worldUID, Material markerBlock, Material lastBlock) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldUID = worldUID;
        this.markerBlock = markerBlock;
        this.lastBlock = lastBlock;
    }

    public void mark(boolean marking) {
        World world = Bukkit.getWorld(worldUID);
        if (world != null) world.getBlockAt(x, y, z).setType(marking ? markerBlock : lastBlock);
    }
}
