package apple.voltskiya.plugin.ore_regen.brush;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

public class BrushUsageListener implements Listener {
    public BrushUsageListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler
    public void onBrushUse(PlayerInteractEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return; // this is possible
        Long uid = meta.getPersistentDataContainer().get(RegenConfigInstance.POWERTOOL_UID_KEY, PersistentDataType.LONG);
        if (uid != null) {
            new Thread(() -> {
                ActiveBrush brush = ActiveBrush.getBrush(uid);
                if (brush == null) {
                    return;
                }
                RayTraceResult rayTraceResult = event.getPlayer().rayTraceBlocks(150, FluidCollisionMode.NEVER);
                if (rayTraceResult != null && rayTraceResult.getHitBlock() != null) {
                    event.getPlayer().sendActionBar(
                            Component.text("Using brush - Radius: " + brush.getRadius())
                    );
                    brush.use(rayTraceResult.getHitBlock(), event.getAction());
                }
            }).start();
        }
    }
}
