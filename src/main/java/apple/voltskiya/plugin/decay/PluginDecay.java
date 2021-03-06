package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.destroy.DecayCommand;
import apple.voltskiya.plugin.decay.destroy.DecayHeartbeat;
import apple.voltskiya.plugin.decay.record.DecayBlockPlaceListener;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;


public class PluginDecay extends VoltskiyaModule {
    public static final long DECAY_INTERVAL = 20;
    public static final float DECAY_PERCENTAGE = .01f;
    public static final int DEFAULT_RESISTANCE = 10;
    public static final int SUPPORT_RADIUS = 7;
    public static float DECAY_INTENSITY = .002f;

    private static PluginDecay instance;

    public static VoltskiyaModule get() {
        return instance;
    }

    @Override
    public void enable() {
        instance = this;
        VerifyDecayDB.initialize();
        new DecayBlockPlaceListener();
        DecayHeartbeat.stopBeating();
        DecayHeartbeat.startBeating();
        VoltskiyaPlugin.get().getCommandManager().registerCommand(new DecayCommand());
    }

    @Override
    public String getName() {
        return "Decay";
    }
}
