package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.destroy.DecayHeartbeat;
import apple.voltskiya.plugin.decay.record.DecayBlockPlaceListener;
import apple.voltskiya.plugin.decay.sql.PlayerBlockMonitor;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;


public class PluginDecay extends VoltskiyaModule {

    private static PluginDecay instance;

    public static VoltskiyaModule get() {
        return instance;
    }

    @Override
    public void enable() {
        instance = this;
//        VerifyDecayDB.initialize();
//        new DecayBlockPlaceListener();
//        DecayHeartbeat.startBeating();
//        PlayerBlockMonitor.tick();
//        VoltskiyaPlugin.get().getCommandManager().registerCommand(new DecayCommand());
    }

    @Override
    public String getName() {
        return "Decay";
    }
}
