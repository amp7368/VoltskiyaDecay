package apple.voltskiya.plugin.ore_regen.player_intervention;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class RegenPlayerInterventionListener implements Listener {
    public RegenPlayerInterventionListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        System.out.println("destroy");
        final Block block = event.getBlock();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        UUID world = block.getWorld().getUID();
        placeBlock(x, y, z, world, block.getType(), event.getNewState().getMaterial());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        System.out.println("place");
        final Block block = event.getBlock();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        UUID world = block.getWorld().getUID();
        placeBlock(x, y, z, world, block.getType(), Material.AIR);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockBreakEvent event) {
        System.out.println("break");
        removeBlock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        System.out.println("explode");
        removeBlock(event.getBlock());
    }

    private void removeBlock(Block block) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        UUID world = block.getWorld().getUID();
        RegenPlayerInterventionMonitor.addTodo(new Coords(x, y, z, world, Material.AIR, block.getType()));
    }

    private void placeBlock(int x, int y, int z, UUID world, Material oldBlock, Material newBlock) {
        RegenPlayerInterventionMonitor.addTodo(new Coords(x, y, z, world, oldBlock, newBlock));
    }
}
