package apple.voltskiya.plugin.decay.record;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.PluginDecay;
import apple.voltskiya.plugin.decay.sql.InsertDB;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.sql.SQLException;
import java.util.logging.Level;

public class DecayBlockPlaceListener implements Listener {
    public DecayBlockPlaceListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        System.out.println(event.getPlayer().getUniqueId() + "  " + event.getPlayer().getUniqueId().toString().length());
        try {
            InsertDB.playerPlace(event);
        } catch (SQLException throwables) {
            PluginDecay.get().log(Level.INFO, "SQL exception placing a block. Probably because the DB has a block that doesn't exist in game\"" + throwables.getMessage() + "\"");
        }
    }
}
