package apple.voltskiya.plugin.ore_regen.gui;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import org.bukkit.NamespacedKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegenConfigInstance {
    public static final NamespacedKey POWERTOOL_UID_KEY = new NamespacedKey(VoltskiyaPlugin.get(), "powertool_regen_uid");
    private final Map<String, Integer> hostBlockToCount;
    private final Map<String, List<Integer>> veinSizeBlockToCount;
    private final Map<String, Integer> densityDistributionBlockToCount;
    public BrushType brushType = null;
    public int brushRadius = 0;

    public RegenConfigInstance() {
        this.hostBlockToCount = new HashMap<>();
        this.veinSizeBlockToCount = new HashMap<>();
        this.densityDistributionBlockToCount = new HashMap<>();
    }

    public RegenConfigInstance(Map<String, Integer> hostBlockToCount, Map<String, List<Integer>> veinSizeBlockToCount, Map<String, Integer> densityDistributionBlockToCount) {
        this.hostBlockToCount = hostBlockToCount;
        this.veinSizeBlockToCount = veinSizeBlockToCount;
        this.densityDistributionBlockToCount = densityDistributionBlockToCount;
    }

    public RegenConfigInstance add(RegenConfigInstance other) {
        final Map<String, Integer> hostBlockToCount = new HashMap<>(this.hostBlockToCount);
        final Map<String, List<Integer>> veinSizeBlockToCount = new HashMap<>(this.veinSizeBlockToCount);
        final Map<String, Integer> densityDistributionBlockToCount = new HashMap<>(this.densityDistributionBlockToCount);
        for (Map.Entry<String, Integer> kv : other.hostBlockToCount.entrySet()) {
            hostBlockToCount.compute(kv.getKey(), (o1, o2) -> o2 == null ? kv.getValue() : kv.getValue() + o2);
        }
        for (Map.Entry<String, List<Integer>> kv : other.veinSizeBlockToCount.entrySet()) {
            veinSizeBlockToCount.compute(kv.getKey(), (k, v) -> {
                if (v == null) return kv.getValue();
                else v.addAll(kv.getValue());
                return v;
            });
        }
        for (Map.Entry<String, Integer> kv : other.densityDistributionBlockToCount.entrySet()) {
            densityDistributionBlockToCount.compute(kv.getKey(), (o1, o2) -> o2 == null ? kv.getValue() : kv.getValue() + o2);
        }
        return new RegenConfigInstance(hostBlockToCount, veinSizeBlockToCount, densityDistributionBlockToCount);
    }

    public enum BrushType {
        SPHERE,
        CUBE,
        CORNER_SELECT
    }

    public Set<Map.Entry<String, Integer>> getHostBlockToCount() {
        return hostBlockToCount.entrySet();
    }

    public Set<Map.Entry<String, List<Integer>>> getVeinSizeBlockToCount() {
        return veinSizeBlockToCount.entrySet();
    }

    public Set<Map.Entry<String, Integer>> getDensityDistributionBlockToCount() {
        return densityDistributionBlockToCount.entrySet();
    }
}
