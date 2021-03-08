package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.Permissions;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenBox;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.regen.RegenHeartbeat;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import static apple.voltskiya.plugin.decay.PluginDecay.REGEN_INTENITY;
import static apple.voltskiya.plugin.decay.PluginDecay.REGEN_INTERVAL;


@CommandAlias("regen")
@CommandPermission(Permissions.DECAY)
public class RegenCommand extends BaseCommand {
    public RegenCommand() {
        VoltskiyaPlugin.get().getCommandManager().registerCommand(this);
    }

    @Subcommand("heartbeat")
    public class Heartbeat extends BaseCommand {
        @Subcommand("start")
        public void heartbeatStart() {
            RegenHeartbeat.startBeating();
        }

        @Subcommand("stop")
        public void heartbeatStop() {
            RegenHeartbeat.stopBeating();
        }
    }

    @Subcommand("intensity")
    public class Intensity extends BaseCommand {
        @Subcommand("set")
        public void setIntensity(Player player, double val) {
            player.sendMessage(String.valueOf(REGEN_INTENITY = val));
        }

        @Subcommand("get")
        public void getIntensity(Player player) {
            player.sendMessage(String.valueOf(REGEN_INTENITY));
        }
    }

    @Subcommand("interval")
    public class Interval extends BaseCommand {
        @Subcommand("set")
        public void setInterval(Player player, long val) {
            player.sendMessage(String.valueOf(REGEN_INTERVAL = val));
        }

        @Subcommand("get")
        public void getInterval(Player player) {
            player.sendMessage(String.valueOf(REGEN_INTERVAL));
        }
    }

    @Subcommand("brush")
    public class RegenBrushCommand extends BaseCommand {
        @Subcommand("create")
        public void createRegenInv(Player player) {
            player.openInventory(new InventoryRegenBox().getInventory());
        }

        @Subcommand("mark")
        public void markEverything(Player player) {
            mark(player, true);
        }

        @Subcommand("unmark")
        public void unmarkEverything(Player player) {
            mark(player, false);
        }

        private void mark(Player player, boolean marking) {
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
            new Thread(() ->
                    brush.markAll(marking, () -> {
                                player.sendMessage(
                                        marking ? "I've marked all your painted locations" :
                                                "I've painted and fully unmarked your marked locations"
                                );
                            }
                    )
            ).start();
        }
    }
}
