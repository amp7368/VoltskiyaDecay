package apple.voltskiya.plugin.decay.destroy;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.sql.DBPlayerBlock;
import apple.voltskiya.plugin.decay.sql.DataPlayerBlock;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static apple.voltskiya.plugin.decay.PluginDecay.DECAY_INTERVAL;

public class DecayHeartbeat {

    private static List<DataPlayerBlock> decayActions = new ArrayList<>();
    private static boolean isDecayCompleted = true;

    public DecayHeartbeat() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(VoltskiyaPlugin.get(), this::tick, DECAY_INTERVAL, Long.MAX_VALUE);
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

    private void tick() {
        new GetRandomAction().start(); // finish whenever. no rush
        List<DataPlayerBlock> actionsToDo = getDecayed();
        List<String> sqlToUpdate = new ArrayList<>();
        for (DataPlayerBlock action : actionsToDo) {
            action.decay();
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
