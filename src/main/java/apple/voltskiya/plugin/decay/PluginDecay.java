package apple.voltskiya.plugin.decay;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.decay.destroy.DecayHeartbeat;
import apple.voltskiya.plugin.decay.record.DecayBlockPlaceListener;
import apple.voltskiya.plugin.decay.sql.VerifyDecayDB;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginDecay extends VoltskiyaModule {
    public static final long DECAY_INTERVAL = 20;
    public static final float DECAY_PERCENTAGE = .001f;
    public static final float DECAY_INTENSITY = .001f;

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
        new DecayHeartbeat();
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
