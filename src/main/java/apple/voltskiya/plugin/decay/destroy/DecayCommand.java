package apple.voltskiya.plugin.decay.destroy;

import apple.voltskiya.plugin.decay.Permissions;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;

@CommandAlias("decay")
@CommandPermission(Permissions.DECAY)
public class DecayCommand extends BaseCommand {
    @Subcommand("heartbeat")
    public  class Heartbeat extends BaseCommand{
        @Subcommand("start")
        public void heartbeatStart() {
            DecayHeartbeat.startBeating();
        }

        @Subcommand("stop")
        public void heartbeatStop() {
            DecayHeartbeat.stopBeating();
        }
    }
}
