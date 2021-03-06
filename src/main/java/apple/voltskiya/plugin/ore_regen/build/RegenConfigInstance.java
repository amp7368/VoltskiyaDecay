package apple.voltskiya.plugin.ore_regen.build;

import java.util.HashMap;
import java.util.Map;

public class RegenConfigInstance {
    private final Map<String, Integer> hostBlockToCount;
    private final Map<String, Integer> veinSizeBlockToCount;
    private final Map<String, Integer> densityDistributionBlockToCount;

    public RegenConfigInstance() {
        this.hostBlockToCount = new HashMap<>();
        this.veinSizeBlockToCount = new HashMap<>();
        this.densityDistributionBlockToCount = new HashMap<>();
    }

    public RegenConfigInstance(Map<String, Integer> hostBlockToCount, Map<String, Integer> veinSizeBlockToCount, Map<String, Integer> densityDistributionBlockToCount) {
        this.hostBlockToCount = hostBlockToCount;
        this.veinSizeBlockToCount = veinSizeBlockToCount;
        this.densityDistributionBlockToCount = densityDistributionBlockToCount;
    }

    public RegenConfigInstance add(RegenConfigInstance other) {
        final Map<String, Integer> hostBlockToCount = new HashMap<>(this.hostBlockToCount);
        final Map<String, Integer> veinSizeBlockToCount = new HashMap<>(this.veinSizeBlockToCount);
        final Map<String, Integer> densityDistributionBlockToCount = new HashMap<>(this.densityDistributionBlockToCount);
        hostBlockToCount.putAll(other.hostBlockToCount);
        veinSizeBlockToCount.putAll(other.veinSizeBlockToCount);
        densityDistributionBlockToCount.putAll(other.densityDistributionBlockToCount);
        return new RegenConfigInstance(hostBlockToCount, veinSizeBlockToCount, densityDistributionBlockToCount);
    }
}
