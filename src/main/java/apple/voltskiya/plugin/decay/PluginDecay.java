package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.decay.destroy.DecayCommand;
import apple.voltskiya.plugin.decay.destroy.DecayHeartbeat;
import apple.voltskiya.plugin.decay.record.DecayBlockPlaceListener;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginDecay extends VoltskiyaModule {
    public static final long DECAY_INTERVAL = 20;
    public static final float DECAY_PERCENTAGE = .01f;
    public static final int DEFAULT_RESISTANCE = 10;
    public static final int SUPPORT_RADIUS = 20;
    public static float DECAY_INTENSITY = .002f;

    private static PluginDecay instance;
    private Logger logger;

    public static VoltskiyaModule get() {
        return instance;
    }

    @Override
    public void enable() {
        instance = this;
        VerifyDecayDB.initialize();
        new DecayBlockPlaceListener();
        DecayHeartbeat.startBeating();
        VoltskiyaPlugin.get().getCommandManager().registerCommand(new DecayCommand());
    }

    @Override
    public String getName() {
        return "Decay";
    }

    @Override
    public void log(Level level, String message) {
        logger.log(level, String.format(" [%s] %s", getName(), message));
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
