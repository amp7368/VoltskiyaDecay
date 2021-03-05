package apple.voltskiya.plugin.decay.destroy;

import apple.voltskiya.plugin.Permissions;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

import static apple.voltskiya.plugin.decay.PluginDecay.DECAY_INTENSITY;

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
        public void setIntensity(float val) {
            System.out.println(val);
            DECAY_INTENSITY = val;
        }
    }
}
