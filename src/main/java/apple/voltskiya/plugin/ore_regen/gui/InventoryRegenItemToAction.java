package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.BiConsumer;

public enum InventoryRegenItemToAction {
    NUFFIN(InventoryRegenBox::nuffin),
    ALLOW(InventoryRegenBox::dealWithAllow),
    BACK(InventoryRegenBox::dealWithBack),
    NEXT(InventoryRegenBox::dealWithForward),
    DISCARD(InventoryRegenBox::dealWithDiscard),
    SAVE(InventoryRegenBox::dealWithSave),
    POWERTOOL(InventoryRegenBox::dealWithChangeTool);
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
