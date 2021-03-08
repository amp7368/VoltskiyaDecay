package apple.voltskiya.plugin.ore_regen.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static apple.voltskiya.plugin.ore_regen.gui.InventoryRegenItemToAction.*;

public class InventoryRegenItems {

    public static final Material FILLER_MATERIAL = Material.WHITE_STAINED_GLASS_PANE;

    private static ItemStack createItem(Material material,
                                        int count,
                                        @Nullable String name,
                                        @Nullable List<String> lore,
                                        InventoryRegenItemToAction action) {
        ItemStack item = new ItemStack(material, count);
        if (name != null) {
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(name, null, TextDecoration.BOLD));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action.name());
            item.setItemMeta(meta);
        }
        if (lore != null) item.setLore(lore);
        return item;
    }


    public static ItemStack setAction(ItemStack item, InventoryRegenItemToAction action) {
        item = new ItemStack(item);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action.name());
        item.setItemMeta(meta);
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
        }}, NEXT);
    }

    public static ItemStack filler() {
        return createItem(Material.WHITE_STAINED_GLASS_PANE, 1, ".", null, ALLOW);
    }

    public static ItemStack host() {
        return createItem(Material.STONE_BRICK_WALL,
                1,
                "Host blocks",
                Collections.singletonList(
                        "Add blocks to the right of this to define what blocks currently exist"
                ),
                NUFFIN);
    }

    public static ItemStack vein() {
        return createItem(Material.DIAMOND,
                1,
                "Vein sizes",
                Arrays.asList(
                        "Add ores to the right of this to define the sizes of ore veins",
                        "If there are three stacks of size 4, and one stack of size 6,",
                        "then 75% of the ore spawns will be of size 4, and 25% of the ore spawns will be of size 6"
                ),
                NUFFIN);
    }

    public static ItemStack density() {
        return createItem(Material.GRAY_GLAZED_TERRACOTTA,
                1,
                "Density Distribution",
                Arrays.asList(
                        "Add ores and background blocks to the right of this to define the densities of the different block types",
                        "If there is 97 stone blocks, 2 iron ore blocks, and 1 diamond ore block",
                        "then the area will return back to 97% of the area selected by the background will be stone,",
                        "2% will be iron ore, and 1% will be diamond ore"
                ),
                NUFFIN
        );
    }

    public static ItemStack discard() {
        return createItem(Material.RED_TERRACOTTA,
                3,
                "Discard this session",
                Collections.singletonList(
                        "To discard this session, continue to click discard"
                ),
                DISCARD
        );
    }

    public static ItemStack toolType() {
        return createItem(
                Material.STICK,
                1,
                "Powertool",
                Collections.singletonList("The item the powertool will take the form of"),
                POWERTOOL
        );
    }
    public static ItemStack radius() {
        return createItem(
                Material.COMPARATOR,
                5,
                "Radius",
                Arrays.asList(
                        "The radius of the brush",
                    "Left click to decrement",
                        "Right click to increment"
                ),RADIUS
        );
    }
    public static ItemStack saveConfig() {
        return createItem(
                Material.GREEN_TERRACOTTA,
                1,
                "Save this as a powertool",
                null,
                SAVE
        );
    }
}
