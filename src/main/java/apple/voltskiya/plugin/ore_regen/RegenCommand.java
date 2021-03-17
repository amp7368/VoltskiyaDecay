package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.Permissions;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.Coords;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenBox;
import apple.voltskiya.plugin.ore_regen.gui.RegenConfigInstance;
import apple.voltskiya.plugin.ore_regen.regen.RegenHeartbeat;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static apple.voltskiya.plugin.ore_regen.PluginOreRegen.*;


@CommandAlias("regen")
@CommandPermission(Permissions.DECAY)
public class RegenCommand extends BaseCommand {
    public RegenCommand() {
        VoltskiyaPlugin.get().getCommandManager().registerCommand(this);
    }

    @Subcommand("heartbeat")
    public class HeartbeatCommand extends BaseCommand {
        @Subcommand("start")
        public void heartbeatStart() {
            RegenHeartbeat.startBeating();
        }

        @Subcommand("stop")
        public void heartbeatStop() {
            RegenHeartbeat.stopBeating();
        }
    }

    @Subcommand("ore")
    public class OreCommand extends BaseCommand {
        @Subcommand("multiplier")
        public class MultiplierCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(ORE_REGEN_MULTIPLIER = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(ORE_REGEN_MULTIPLIER));
            }
        }

        @Subcommand("intensity")
        public class IntensityCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(ORE_REGEN_INTENSITY = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(ORE_REGEN_INTENSITY));
            }
        }

        @Subcommand("randomness")
        public class RandomnessCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(ORE_REGEN_RANDOMNESS = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(ORE_REGEN_RANDOMNESS));
            }
        }
    }

    @Subcommand("air")
    public class AirCommand extends BaseCommand {
        @Subcommand("multiplier")
        public class MultiplierCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(AIR_REGEN_MULTIPLIER = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(AIR_REGEN_MULTIPLIER));
            }
        }

        @Subcommand("intensity")
        public class IntensityCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(AIR_REGEN_INTENSITY = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(AIR_REGEN_INTENSITY));
            }
        }

        @Subcommand("randomness")
        public class RandomnessCommand extends BaseCommand {
            @Subcommand("set")
            public void setIntensity(Player player, double val) {
                player.sendMessage(String.valueOf(AIR_REGEN_RANDOMNESS = val));
            }

            @Subcommand("get")
            public void getIntensity(Player player) {
                player.sendMessage(String.valueOf(AIR_REGEN_RANDOMNESS));
            }
        }
    }

    @Subcommand("settings")
    public class Settings extends BaseCommand {
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

        @Subcommand("maxcount")
        public class MaxCount extends BaseCommand {
            @Subcommand("set")
            public void setInterval(Player player, int val) {
                player.sendMessage(String.valueOf(REGEN_MAX_COUNT = val));
            }

            @Subcommand("get")
            public void getInterval(Player player) {
                player.sendMessage(String.valueOf(REGEN_MAX_COUNT));
            }
        }
    }

    @Subcommand("highlight")
    public void highlight(int toolUid) {
        try {
            List<Coords> coords = DBRegen.getAllBlocks(toolUid);
            for (Coords coord : coords) {
                Bukkit.getWorld(coord.worldUID).getBlockAt(coord.x, coord.y, coord.z).setType(Material.EMERALD_BLOCK);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
                for (Coords coord : coords) {
                    Bukkit.getWorld(coord.worldUID).getBlockAt(coord.x, coord.y, coord.z).setType(coord.lastBlock);
                }
            }, 300);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Subcommand("brush")
    public class RegenBrushCommand extends BaseCommand {
        @Subcommand("destory")
        public void destory(Player player) {
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
            new Thread(() -> {
                brush.destoryBlocks();
                player.sendMessage("I've destroyed it D:");
            }).start();
        }

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
                    brush.markAll(marking, () -> player.sendMessage(
                            marking ? "I've marked all your painted locations" :
                                    "I've painted and fully unmarked your marked locations"
                            )
                    )
            ).start();
        }
    }
}
