package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;

public class InventoryRegenBox implements InventoryHolder {
    public static final int POWERTOOL_TYPE_INDEX = 2;
    private static final int POWERTOOL_RADIUS_INDEX = 6;
    @Nullable
    private final InventoryRegenBox previous;
    @Nullable
    private InventoryRegenBox next;

    private final int pageNumber;

    private final Inventory inventory = Bukkit.createInventory(this, 54);

    public InventoryRegenBox() {
        this.previous = null;
        this.pageNumber = 1;
        this.next = null;
        initialize();
    }

    public InventoryRegenBox(@NotNull InventoryRegenBox previous, int pageNumber) {
        this.previous = previous;
        this.pageNumber = pageNumber;
        this.next = null;
        initialize();
        copySettings(previous);
    }

    private void copySettings(InventoryRegenBox other) {
        for (int i = 1; i < 8; i++) {
            if (i == 4) continue;
            inventory.setItem(i, other.inventory.getItem(i));
        }
    }

    private void sendSettingsPrev(InventoryRegenBox other) {
        if (previous != null) previous.copySettings(other);
    }

    private void sendSettingsNext(InventoryRegenBox other) {
        if (next != null) next.copySettings(other);
    }

    public InventoryRegenBox(InventoryRegenBox other) {
        this.previous = other.previous;
        this.pageNumber = other.pageNumber;
        this.next = other.next;
        this.inventory.setContents(other.getInventory().getContents());
    }

    private void initialize() {
        // fill the background with filler
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, InventoryRegenItems.filler());

        // first row
        inventory.setItem(0, InventoryRegenItems.back(pageNumber == 1 ? "None" : String.valueOf(pageNumber - 1)));
        inventory.setItem(1, InventoryRegenItems.nuffin());
        inventory.setItem(POWERTOOL_TYPE_INDEX, InventoryRegenItems.toolType());
        inventory.setItem(3, InventoryRegenItems.nuffin());
        inventory.setItem(4, InventoryRegenItems.here(pageNumber));
        inventory.setItem(5, InventoryRegenItems.nuffin());
        inventory.setItem(POWERTOOL_RADIUS_INDEX, InventoryRegenItems.radius());
        inventory.setItem(7, InventoryRegenItems.nuffin());
        inventory.setItem(8, InventoryRegenItems.next(next != null, pageNumber + 1));

        // second row
        inventory.setItem(9, InventoryRegenItems.host());
        inventory.setItem(17, InventoryRegenItems.nuffin());

        // third row
        inventory.setItem(18, InventoryRegenItems.vein());
        inventory.setItem(26, InventoryRegenItems.nuffin());

        // fourth row
        inventory.setItem(27, InventoryRegenItems.density());
        inventory.setItem(35, InventoryRegenItems.saveConfig());

        // fifth row
        inventory.setItem(36, InventoryRegenItems.nuffin());
        inventory.setItem(44, InventoryRegenItems.nuffin());

        // sixth row
        inventory.setItem(45, InventoryRegenItems.nuffin());
        inventory.setItem(53, InventoryRegenItems.nuffin());

        for (int i = 36; i < 54; i++) inventory.setItem(i, InventoryRegenItems.nuffin());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void nuffin(InventoryClickEvent ignored) {
    }

    public void dealWithAllow(InventoryClickEvent event) {
        @Nullable ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            // clear whatever is in this slot
            inventory.setItem(event.getSlot(), InventoryRegenItems.filler());
        } else {
            // if same type, add clicked stuff
            // if different type, set clicked stuff
            // if shift click increment or decrement the current item
            ItemStack current = this.inventory.getItem(event.getSlot());
            if (event.isShiftClick()) {
                if (current != null) {
                    if (event.isLeftClick()) {
                        // decrement
                        current.setAmount(current.getAmount() - 1);
                    } else if (event.isRightClick()) {
                        // increment
                        current.setAmount(Math.min(current.getAmount() + 1, current.getMaxStackSize()));
                    } else {
                        // copy
                        event.getWhoClicked().setItemOnCursor(new ItemStack(current));
                    }
                }
            } else if (current == null || cursor.getType() != current.getType()) {
                //  set clicked stuff
                this.inventory.setItem(event.getSlot(), InventoryRegenItems.setAction(cursor, InventoryRegenItemToAction.ALLOW));
            } else if (cursor.getType() == current.getType()) {
                current.setAmount(Math.min(current.getAmount() + cursor.getAmount(), current.getMaxStackSize()));
            }
        }
    }

    public void dealWithBack(InventoryClickEvent event) {
        if (previous != null) event.getWhoClicked().openInventory(previous.getInventory());
    }

    public void dealWithForward(InventoryClickEvent event) {
        event.getWhoClicked().openInventory(Objects.requireNonNullElseGet(next,
                () -> next = new InventoryRegenBox(this, pageNumber + 1)).getInventory());
    }

    public void dealWithChangeTool(InventoryClickEvent event) {
        ItemStack itemThere = event.getCurrentItem();
        if (itemThere == null) {
            throw new IllegalStateException("ToolChange was attempted without something to attempt it.");
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            itemThere.setType(cursor.getType());
        }
        sendSettingsNext(this);
        sendSettingsPrev(this);
    }

    public void dealWithRadiusChange(InventoryClickEvent event) {
        ItemStack itemThere = event.getCurrentItem();
        if (itemThere == null) {
            throw new IllegalStateException("ToolChange was attempted without something to attempt it.");
        }
        if (event.isLeftClick()) {
            itemThere.setAmount(Math.max(1, itemThere.getAmount() - 1));
        } else if (event.isRightClick()) {
            itemThere.setAmount(Math.min(itemThere.getMaxStackSize(), itemThere.getAmount() + 1));
        }
        sendSettingsNext(this);
        sendSettingsPrev(this);

    }

    public void dealWithSave(InventoryClickEvent event) {
        RegenConfigInstance configPrev = previous == null ? new RegenConfigInstance() : previous.countPrevious();
        RegenConfigInstance configNext = countNext();
        RegenConfigInstance config = configPrev.add(configNext);

        // set info about the config
        ItemStack radiusItem = inventory.getItem(POWERTOOL_RADIUS_INDEX);
        if (radiusItem == null) throw new IllegalStateException("There is no radius specified for the brush");
        config.brushRadius = radiusItem.getAmount();

        config.brushType = RegenConfigInstance.BrushType.CUBE;

        ItemStack powertool = inventory.getItem(POWERTOOL_TYPE_INDEX);
        ItemStack item = new ItemStack(powertool == null ? Material.STICK : powertool.getType(), 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Regen Powertool - (rename me)"));
        try {

            meta.getPersistentDataContainer().set(
                    RegenConfigInstance.POWERTOOL_UID_KEY,
                    PersistentDataType.LONG,
                    DBRegen.saveConfig(config)
            );
            item.setItemMeta(meta);
            event.getWhoClicked().getInventory().addItem(item);
            event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
        } catch (SQLException throwables) {
            // todo
            throwables.printStackTrace();
        }
    }

    private RegenConfigInstance countNext() {
        RegenConfigInstance me = countThis();
        RegenConfigInstance config = next == null ? new RegenConfigInstance() : next.countNext();
        return me.add(config);
    }

    private RegenConfigInstance countPrevious() {
        RegenConfigInstance me = countThis();
        RegenConfigInstance config = previous == null ? new RegenConfigInstance() : previous.countPrevious();
        return me.add(config);
    }

    private RegenConfigInstance countThis() {
        Map<String, Integer> hostBlockToCount = new HashMap<>();
        for (int i = 10; i < 17; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                Material type = item.getType();
                if (type != InventoryRegenItems.FILLER_MATERIAL && !type.isAir() && type.isBlock())
                    hostBlockToCount.compute(type.name(), (o1, o2) -> o2 == null ? item.getAmount() : o2 + item.getAmount());
            }
        }
        Map<String, List<Integer>> veinSizeBlockToCount = new HashMap<>();
        for (int i = 19; i < 26; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                Material type = item.getType();
                if (type != InventoryRegenItems.FILLER_MATERIAL && !type.isAir() && type.isBlock()) {
                    veinSizeBlockToCount.putIfAbsent(type.name(), new ArrayList<>());
                    veinSizeBlockToCount.get(type.name()).add(item.getAmount());
                }
            }
        }
        Map<String, Integer> densityDistributionBlockToCount = new HashMap<>();
        for (int i = 28; i < 35; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                Material type = item.getType();
                if (type != InventoryRegenItems.FILLER_MATERIAL && !type.isAir() && type.isBlock())
                    densityDistributionBlockToCount.compute(type.name(), (o1, o2) -> o2 == null ? item.getAmount() : o2 + item.getAmount());
            }
        }
        return new RegenConfigInstance(hostBlockToCount, veinSizeBlockToCount, densityDistributionBlockToCount);
    }

    public void dealWithDiscard(InventoryClickEvent inventoryClickEvent) {
        ItemStack itemThere = inventoryClickEvent.getCurrentItem();
        if (itemThere == null) {
            throw new IllegalStateException("Discarding was attempted without something to attempt it.");
        } else {
            int amount = itemThere.getAmount();
            if (amount == 1) {
                for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) {
                    viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                }
            }
            itemThere.setAmount(amount - 1);
        }

    }

    @Nullable
    public InventoryRegenBox tryToClose() {
        ItemStack itemThere = inventory.getItem(53);
        if (itemThere == null) {
            return null;
        } else {
            String action = itemThere.getItemMeta().getPersistentDataContainer().get(InventoryRegenItemToAction.actionKey, PersistentDataType.STRING);
            if (action == null || !action.equals(InventoryRegenItemToAction.DISCARD.name())) {
                inventory.setItem(53, InventoryRegenItems.discard());
            }
            return new InventoryRegenBox(this);
        }
    }
}
