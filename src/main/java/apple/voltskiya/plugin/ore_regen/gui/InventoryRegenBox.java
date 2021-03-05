package apple.voltskiya.plugin.ore_regen.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class InventoryRegenBox implements InventoryHolder {
    private final InventoryRegenBox previous;
    private InventoryRegenBox next = null;

    private final int pageNumber;

    private final Inventory inventory = Bukkit.createInventory(this, 54);

    public InventoryRegenBox() {
        this.previous = null;
        this.pageNumber = 1;
        this.next = null;
        initialize();
    }

    public InventoryRegenBox(InventoryRegenBox previous, int pageNumber) {
        this.previous = previous;
        this.pageNumber = pageNumber;
        this.next = null;
        initialize();
    }

    private void initialize() {
        // fill the background with filler
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, InventoryRegenItems.filler());

        // first row
        inventory.setItem(0, InventoryRegenItems.back(pageNumber == 1 ? "None" : String.valueOf(pageNumber - 1)));
        inventory.setItem(1, InventoryRegenItems.nuffin());
        inventory.setItem(2, InventoryRegenItems.nuffin());
        inventory.setItem(3, InventoryRegenItems.nuffin());
        inventory.setItem(4, InventoryRegenItems.here(pageNumber));
        inventory.setItem(5, InventoryRegenItems.nuffin());
        inventory.setItem(6, InventoryRegenItems.nuffin());
        inventory.setItem(7, InventoryRegenItems.nuffin());
        inventory.setItem(8, InventoryRegenItems.next(next != null, pageNumber + 1));

        // second row
        inventory.setItem(9, InventoryRegenItems.host());
        inventory.setItem(17, InventoryRegenItems.nuffin());
        inventory.setItem(26, InventoryRegenItems.nuffin());
        inventory.setItem(35, InventoryRegenItems.nuffin());
        inventory.setItem(44, InventoryRegenItems.nuffin());
        inventory.setItem(53, InventoryRegenItems.nuffin());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void nuffin(InventoryClickEvent event) {
    }

    public void dealWithBack(InventoryClickEvent event) {
        event.getWhoClicked().sendMessage("back");
    }

    private void close() {
        inventory.clear();
        if (next != null) next.close();
    }
}
