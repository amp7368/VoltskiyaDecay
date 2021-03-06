package apple.voltskiya.plugin.ore_regen.brush;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.sql.SQLException;

public class BrushUsageListener implements Listener {
    public BrushUsageListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();
        Long uid = meta.getPersistentDataContainer().get(RegenConfigInstance.POWERTOOL_UID_KEY, PersistentDataType.LONG);
        if (uid != null) {
            ActiveBrush brush = ActiveBrush.getBrush(uid);
            if (brush == null) {
                try {
                    brush = DBRegen.getBrush(uid);
                } catch (SQLException throwables) {
                    //todo
                    throwables.printStackTrace();
                    return;
                }
                if (brush == null) return;
                ActiveBrush.addBrush(brush);
            }
            RayTraceResult rayTraceResult = event.getPlayer().rayTraceBlocks(150, FluidCollisionMode.NEVER);
            if (rayTraceResult != null && rayTraceResult.getHitBlock() != null)
                brush.use(rayTraceResult.getHitBlock(), event.getAction());
        }
    }
}
