package apple.voltskiya.decay;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public abstract class VoltskiyaModule {

    private boolean isEnabled;
    private YamlConfiguration configuration;

    void init() {
    }

    public abstract void enabled();

    public abstract String getName();

    boolean shouldEnable() {
        return true;
    }

    protected void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public File getDataFolder() {
        File dataFolder = new File(VoltskiyaDecay.get().getDataFolder(), getName().toLowerCase());
        if (!dataFolder.exists()) dataFolder.mkdir();
        return dataFolder;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoltskiyaModule) return obj.hashCode() == hashCode();
        return false;
    }
}
