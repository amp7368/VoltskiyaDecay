package apple.voltskiya.plugin.decay.sql;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.UPDATE_DECAY_DB_INTERVAL;


public class PlayerBlockMonitor {
    private static Set<BlockForDB> blocksPlaced = new HashSet<>();
    private static Set<BlockForDB> blocksDestroyed = new HashSet<>();

    private static final Object blocksSync = new Object();

    public static void playerPlace(BlockPlaceEvent event) {
        @NotNull Block blockPlaced = event.getBlockPlaced();
        synchronized (blocksSync) {
            blocksPlaced.add(new BlockForDB(blockPlaced, event.getPlayer().getUniqueId().toString()));
        }
    }

    public static void remove(BlockBreakEvent event) {
        @NotNull Block blockPoofed = event.getBlock();
        synchronized (blocksSync) {
            blocksDestroyed.add(new BlockForDB(blockPoofed, null));
        }
    }

    public static void remove(int x, int y, int z, UUID worldUUID, Material material, String owner) {
        synchronized (blocksSync) {
            blocksDestroyed.add(new BlockForDB(x, y, z, worldUUID, material, owner));
        }
    }

    private static final AtomicBoolean isTicking = new AtomicBoolean(false);

    public static void tick() {
        if (!isTicking.get()) {
            isTicking.set(true);
            new PlayerBlockHeartbeat().start();
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), PlayerBlockMonitor::tick, UPDATE_DECAY_DB_INTERVAL);
    }

    public static class PlayerBlockHeartbeat extends Thread {
        @Override
        public void run() {
            synchronized (blocksSync) {
                try {
                    DBPlayerBlock.remove(blocksDestroyed);
                    DBPlayerBlock.playerPlace(blocksPlaced);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                blocksDestroyed = new HashSet<>();
                blocksPlaced = new HashSet<>();
            }
            isTicking.set(false);
        }
    }

    public static class BlockForDB {
        private final int x;
        private final int y;
        private final int z;
        private final UUID worldUUID;
        private final Material material;
        private final String owner;

        public BlockForDB(int x, int y, int z, UUID worldUUID, Material material, String owner) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldUUID = worldUUID;
            this.material = material;
            this.owner = owner;
        }

        public BlockForDB(Block block, String owner) {
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
            this.worldUUID = block.getWorld().getUID();
            this.material = block.getType();
            this.owner = owner;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public UUID getWorldUUID() {
            return worldUUID;
        }

        public Material getMaterial() {
            return material;
        }

        public String getOwner() {
            return owner;
        }
    }
}
