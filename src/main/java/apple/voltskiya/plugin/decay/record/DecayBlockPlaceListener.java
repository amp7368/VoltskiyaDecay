package apple.voltskiya.plugin.decay.record;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.PluginDecay;
import apple.voltskiya.plugin.decay.sql.DBPlayerBlock;
import apple.voltskiya.plugin.decay.sql.PlayerBlockMonitor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.sql.SQLException;
import java.util.logging.Level;

public class DecayBlockPlaceListener implements Listener {
    public DecayBlockPlaceListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        PlayerBlockMonitor.playerPlace(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGone(BlockBreakEvent event) {
        PlayerBlockMonitor.remove(event);
    }
}
