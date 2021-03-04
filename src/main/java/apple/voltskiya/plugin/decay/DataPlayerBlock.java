package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.decay.sql.DBPlayerBlock;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import static apple.voltskiya.plugin.decay.PluginDecay.DECAY_INTENSITY;

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
        return random.nextFloat() < DECAY_INTENSITY;
    }

    public void decay() {
        Material nextMaterial = DecayModifiers.getNextDecay(block);
        int newStrength = DecayModifiers.getResistance(block);
        synchronized (VerifyDecayDB.syncDB) {
            try {
                if (newStrength == 0) DBPlayerBlock.remove(x, y, z);
                else DBPlayerBlock.update(x, y, z, -(myStrength - newStrength));
                Bukkit.getWorld(worldUUID).getBlockAt(x, y, z).setType(nextMaterial);
            } catch (SQLException throwables) {
                //TODO
                throwables.printStackTrace();
            }
        }
    }
}
