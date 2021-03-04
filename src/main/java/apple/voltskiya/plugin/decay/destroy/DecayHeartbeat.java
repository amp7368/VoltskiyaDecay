package apple.voltskiya.plugin.decay.destroy;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.sql.DBPlayerBlock;
import apple.voltskiya.plugin.decay.DataPlayerBlock;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static apple.voltskiya.plugin.decay.PluginDecay.DECAY_INTERVAL;

public class DecayHeartbeat {

    private static List<DataPlayerBlock> decayActions = new ArrayList<>();
    private static boolean isDecayCompleted = true;
    private static boolean isHeartBeating = false;

    public synchronized static void startBeating() {
        if (!isHeartBeating) {
            isHeartBeating = true;
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), DecayHeartbeat::tick, DECAY_INTERVAL);
        }
    }

    public synchronized static void stopBeating() {
        isHeartBeating = false;
    }

    private static synchronized void tick() {
        new GetRandomAction().start(); // finish whenever. no rush
        List<DataPlayerBlock> actionsToDo = getDecayed();
        for (DataPlayerBlock action : actionsToDo) {
            action.decay();
        }
        if (isHeartBeating) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), DecayHeartbeat::tick, DECAY_INTERVAL);
        }
    }

    private static synchronized List<DataPlayerBlock> getDecayed() {
        isDecayCompleted = true;
        return decayActions;
    }

    private static synchronized void addDecayed(List<DataPlayerBlock> toDecay) {
        if (isDecayCompleted) {
            decayActions = toDecay;
            isDecayCompleted = false;
        } else {
            decayActions.addAll(toDecay);
        }
    }


    private static class GetRandomAction extends Thread {
        @Override
        public void run() {
            try {
                List<DataPlayerBlock> tickMeBlocks = DBPlayerBlock.getRandom();
                List<DataPlayerBlock> decayActions = new ArrayList<>();
                for (DataPlayerBlock block : tickMeBlocks) {
                    if (block.isDecay()) {
                        decayActions.add(block);
                    }
                }
                addDecayed(decayActions);
            } catch (SQLException throwables) {
                //todo
                throwables.printStackTrace();
            }
        }
    }
}
