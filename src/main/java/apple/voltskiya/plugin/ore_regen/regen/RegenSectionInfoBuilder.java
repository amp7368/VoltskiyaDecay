package apple.voltskiya.plugin.ore_regen.regen;

import org.bukkit.Material;

import java.util.*;

public class RegenSectionInfoBuilder {
    private final Map<Material, Integer> actualBlockCount = new HashMap<>();
    private final Map<Material, Integer> hostBlocks = new HashMap<>();
    private final Map<Material, List<Integer>> veinSizes = new HashMap<>();
    private final Map<Material, Integer> desiredBlockDistribution = new HashMap<>();
    private final long uid;

    public RegenSectionInfoBuilder(long uid) {
        this.uid = uid;
    }

    public RegenSectionInfo build() {
        int totalActualBlocks = 0;
        for (Integer i : actualBlockCount.values()) totalActualBlocks += i;
        int totalVeinSizesCount = 0;
        for (List<Integer> sizes : veinSizes.values())
            // we're only counting them. not summing because a vein with 4 ore will be
            // just as likely as a vein with 1 ore
            totalVeinSizesCount += sizes.size();

        Map<Material, VeinProbability> veinSizesProbability = new HashMap<>();
        for (Map.Entry<Material, List<Integer>> veinEntry : veinSizes.entrySet()) {
            veinSizesProbability.put(
                    veinEntry.getKey(),
                    new VeinProbability(veinEntry.getValue(), totalVeinSizesCount)
            );
        }
        int totalDistributionSum = 0;
        for (Integer i : desiredBlockDistribution.values()) totalDistributionSum += i;
        Map<Material, Double> desiredBlockDistributionPerc = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : desiredBlockDistribution.entrySet()) {
            desiredBlockDistributionPerc.put(entry.getKey(), ((double) entry.getValue()) / totalDistributionSum);
        }
        for (Material material : veinSizesProbability.keySet())
            actualBlockCount.putIfAbsent(material, 0);

        Map<Material, Double> hostBlockDistribution = new HashMap<>();
        totalDistributionSum = 0;
        for (Integer i : hostBlocks.values()) totalDistributionSum += i;
        for (Map.Entry<Material, Integer> entry : hostBlocks.entrySet()) {
            hostBlockDistribution.put(entry.getKey(), ((double) entry.getValue()) / totalDistributionSum);
        }

        return new RegenSectionInfo(actualBlockCount, totalActualBlocks, hostBlockDistribution, veinSizesProbability, desiredBlockDistributionPerc, uid);
    }

    public void addActualBlockCount(String blockName, int blockCount) {
        actualBlockCount.put(Material.valueOf(blockName), blockCount);
    }

    public void addHostBlock(String blockName, int blockCount) {
        hostBlocks.put(Material.valueOf(blockName), blockCount);
    }

    public void addVeinSizes(String blockName, int desiredCount) {
        final Material material = Material.valueOf(blockName);
        veinSizes.putIfAbsent(material, new ArrayList<>());
        veinSizes.get(material).add(desiredCount);
    }

    public void addDesiredOreCount(String blockName, int desiredCount) {
        desiredBlockDistribution.put(Material.valueOf(blockName), desiredCount);
    }
}
