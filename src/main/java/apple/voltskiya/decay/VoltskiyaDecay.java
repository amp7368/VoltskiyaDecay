package apple.voltskiya.decay;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VoltskiyaDecay extends JavaPlugin {
    private static VoltskiyaDecay instance;
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        setupACF();
        registerDecay();
    }

    private void registerDecay() {

    }

    public static VoltskiyaDecay get() {
        return instance;
    }

    private void setupACF() {
        commandManager = new PaperCommandManager(this);
    }
}
