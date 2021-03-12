package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.Permissions;
import apple.voltskiya.plugin.decay.destroy.DecayHeartbeat;
import apple.voltskiya.plugin.ore_regen.PluginOreRegen;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

@CommandAlias("decay")
@CommandPermission(Permissions.DECAY)
public class DecayCommand extends BaseCommand {
    @Subcommand("heartbeat")
    public class Heartbeat extends BaseCommand {
        @Subcommand("start")
        public void heartbeatStart() {
            DecayHeartbeat.startBeating();
        }

        @Subcommand("stop")
        public void heartbeatStop() {
            DecayHeartbeat.stopBeating();
        }
    }

    @Subcommand("intensity")
    public class Intensity extends BaseCommand {
        @Subcommand("set")
        public void setIntensity(Player player, float val) {
            player.sendMessage(String.valueOf(PluginOreRegen.DECAY_INTENSITY = val));
        }

        @Subcommand("get")
        public void getIntensity(Player player) {
            player.sendMessage(String.valueOf(PluginOreRegen.DECAY_INTENSITY));
        }
    }

    @Subcommand("interval")
    public class Interval extends BaseCommand {
        @Subcommand("set")
        public void setInterval(Player player, long val) {
            player.sendMessage(String.valueOf(PluginOreRegen.DECAY_INTERVAL = val));
        }

        @Subcommand("get")
        public void getInterval(Player player) {
            player.sendMessage(String.valueOf(PluginOreRegen.DECAY_INTERVAL));
        }
    }

}
