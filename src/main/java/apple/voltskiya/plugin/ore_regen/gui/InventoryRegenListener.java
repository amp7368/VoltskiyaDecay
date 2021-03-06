package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

public class InventoryRegenListener implements Listener {
    public InventoryRegenListener() {
        Bukkit.getPluginManager().registerEvents(this, VoltskiyaPlugin.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void drag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof InventoryRegenBox) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void move(InventoryMoveItemEvent event) {
        if (event.getDestination().getHolder() instanceof InventoryRegenBox ||
                event.getInitiator().getHolder() instanceof InventoryRegenBox) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void click(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof InventoryRegenBox) {
            @Nullable ItemStack item = event.getCurrentItem();
            if (item != null) {
                ClickType click = event.getClick();
                if (click.isCreativeAction()|| click.isKeyboardClick() || click == ClickType.DOUBLE_CLICK ) {
                    event.setCancelled(true);
                    return;
                }
                String action = item.getItemMeta().getPersistentDataContainer().get(InventoryRegenItemToAction.actionKey, PersistentDataType.STRING);
                boolean isAction = false;
                for (InventoryRegenItemToAction a : InventoryRegenItemToAction.values()) {
                    if (a.name().equals(action)) {
                        a.run((InventoryRegenBox) holder, event);
                        isAction = true;
                        break;
                    }
                }
                event.setCancelled(isAction);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void closeInventory(InventoryCloseEvent event) {
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW || event.getReason() == InventoryCloseEvent.Reason.PLUGIN)
            return;
        Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof InventoryRegenBox) {
            InventoryRegenBox newBox = ((InventoryRegenBox) inventory.getHolder()).tryToClose();
            if (newBox != null) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> event.getPlayer().openInventory(newBox.getInventory()), 0);
            }
        }
    }
}
