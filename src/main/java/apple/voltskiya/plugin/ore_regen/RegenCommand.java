package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.Permissions;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenBox;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

@CommandAlias("regen")
@CommandPermission(Permissions.DECAY)
public class RegenCommand extends BaseCommand {
    @Subcommand("create")
    public void createRegenInv(Player player) {
        player.openInventory(new InventoryRegenBox().getInventory());
    }

    @Subcommand("brush")
    public  class RegenBrushCommand extends BaseCommand {
        @Subcommand("mark")
        public void markEverything(Player player) {
            player.sendMessage("That functionality is not implemented");
        }

        @Subcommand("unmark")
        public void paintEverything(Player player) {
            ItemMeta meta = player.getInventory().getItemInMainHand().getItemMeta();
            if (meta == null) {
                player.sendMessage("There is nothing in your hand");
                return;
            }
            @Nullable Long powertoolUid = meta.getPersistentDataContainer().get(RegenConfigInstance.POWERTOOL_UID_KEY, PersistentDataType.LONG);
            if (powertoolUid == null) {
                player.sendMessage("You're not holding a valid powertool");
                return;
            }
            ActiveBrush brush = ActiveBrush.getBrush(powertoolUid);
            if (brush == null) {
                player.sendMessage("You're not holding a valid powertool");
                return;
            }
            brush.unmarkAll();
            player.sendMessage("I've painted and fully unmarked your marked locations");
        }
    }
}
