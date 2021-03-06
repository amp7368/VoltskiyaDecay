package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.decay.sql.DBPlayerBlock;
import apple.voltskiya.plugin.decay.sql.PlayerBlockMonitor;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.DECAY_INTENSITY;

public class DataPlayerBlock {
    private static final Random random = new Random();
    private final int x;
    private final int y;
    private final int z;
    private final Material block;
    private final int myStrength;
    private final int effectiveStrength;
    private final String uuid;
    private UUID worldUUID;

    public DataPlayerBlock(int x, int y, int z, Material block, int myStrength, int effectiveStrength, String uuid, String worldUUIDString) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
        this.myStrength = myStrength;
        this.effectiveStrength = effectiveStrength;
        this.uuid = uuid;
        this.worldUUID = UUID.fromString(worldUUIDString);
    }

    public boolean isDecay() {
        return random.nextFloat() < DECAY_INTENSITY / effectiveStrength;
    }

    public void decay() {
        Material nextMaterial = DecayModifiers.getNextDecay(block);
        int newStrength = DecayModifiers.getResistance(nextMaterial);
        System.out.printf("%s -> %s : %d\n", block.toString(), nextMaterial.toString(), newStrength);
        synchronized (VerifyDecayDB.syncDB) {
            try {
                if (newStrength == 0) PlayerBlockMonitor.remove(x, y, z, worldUUID,block,uuid);
                else DBPlayerBlock.update(x, y, z, -(myStrength - newStrength), nextMaterial);
                final World world = Bukkit.getWorld(worldUUID);
                if (world != null)
                    world.getBlockAt(x, y, z).setType(nextMaterial);
            } catch (SQLException throwables) {
                //TODO
                throwables.printStackTrace();
            }
        }
    }
}
