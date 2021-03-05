package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class InventoryRegenListener implements Listener {
    public InventoryRegenListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void clickListener(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof InventoryRegenBox) {
            @Nullable ItemStack item = event.getCurrentItem();
            if (item != null) {
                String action = item.getItemMeta().getPersistentDataContainer().get(InventoryRegenItemToAction.actionKey, PersistentDataType.STRING);
                for (InventoryRegenItemToAction a : InventoryRegenItemToAction.values())
                    if (a.name().equals(action)) a.run((InventoryRegenBox) holder, event);
                event.setCancelled(true);
            }
        }
    }
}
