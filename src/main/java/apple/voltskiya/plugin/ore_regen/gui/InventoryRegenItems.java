package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static apple.voltskiya.plugin.ore_regen.gui.InventoryRegenItemToAction.*;

public class InventoryRegenItems {

    private static ItemStack createItem(Material material,
                                        int count,
                                        @Nullable String name,
                                        @Nullable List<String> lore,
                                        InventoryRegenItemToAction action) {
        ItemStack item = new ItemStack(material, count);
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            meta.setLocalizedName(name);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action.name());
            item.setItemMeta(meta);
        }
        if (lore != null) item.setLore(lore);
        return item;
    }


    public static ItemStack nuffin() {
        return createItem(Material.BLACK_STAINED_GLASS_PANE, 1, ".", null, NUFFIN);
    }

    public static ItemStack back(String page) {
        return createItem(Material.ARROW, 1, "Page " + page, new ArrayList<String>() {{
            add("Go to the previous page");
        }}, BACK);
    }


    public static ItemStack here(int page) {
        return createItem(Material.NETHERITE_HELMET, 1, "Page " + page, null, NUFFIN);
    }

    public static ItemStack next(boolean isCreate, int page) {
        return createItem(Material.ARROW, 1, isCreate ? "Create page " + page : "Page " + page, new ArrayList<>() {{
            add("Go to the next page");
        }}, NUFFIN);
    }

    public static ItemStack filler() {
        return createItem(Material.WHITE_STAINED_GLASS_PANE, 1, ".", null, NUFFIN);
    }

    public static ItemStack host() {
        return createItem(Material.STONE_BRICK_WALL, 1, "Host blocks", new ArrayList<>() {{
            add("Add blocks to the right of this to define what blocks currently exist");
        }}, NUFFIN);
    }
}
