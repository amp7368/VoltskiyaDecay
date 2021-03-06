package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;

public enum InventoryRegenItemToAction {
    NUFFIN(InventoryRegenBox::nuffin),
    BACK(InventoryRegenBox::dealWithBack),
    DISCARD(InventoryRegenBox::dealWithDiscard),
    NEXT(InventoryRegenBox::dealWithForward);
    private final BiConsumer<InventoryRegenBox, InventoryClickEvent> action;

    private static final String ACTION_KEY_NAME = "regen-item-action";
    public static final NamespacedKey actionKey = new NamespacedKey(VoltskiyaPlugin.get(), ACTION_KEY_NAME);

    InventoryRegenItemToAction(BiConsumer<InventoryRegenBox, InventoryClickEvent> action) {
        this.action = action;
    }

    public void run(InventoryRegenBox box, InventoryClickEvent event) {
        action.accept(box, event);
    }
}
