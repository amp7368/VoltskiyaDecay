package apple.voltskiya.plugin.ore_regen.gui;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

@CommandAlias("regen")
public class RegenCommand extends BaseCommand {
    @Subcommand("create")
    public static void createRegenInv(Player player){
        player.openInventory(new InventoryRegenBox().getInventory());
    }
}
