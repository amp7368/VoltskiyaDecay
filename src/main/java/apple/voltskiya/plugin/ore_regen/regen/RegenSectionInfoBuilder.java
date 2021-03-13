package apple.voltskiya.plugin.ore_regen.regen;

import apple.voltskiya.plugin.ore_regen.sql.DBUtils;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionInfoBuilder {
    private final Map<Integer, Integer> actualBlockCount = new HashMap<>();
    private final Map<Integer, Integer> hostBlocks = new HashMap<>();
    private final Map<Integer, List<Integer>> veinSizes = new HashMap<>();
    private final Map<Integer, Integer> desiredBlockDistribution = new HashMap<>();
    private final long uid;

    public RegenSectionInfoBuilder(long uid) {
        this.uid = uid;
    }

    public RegenSectionInfo build() throws SQLException {
        int totalVeinSizesCount = 0;
        for (List<Integer> sizes : veinSizes.values())
            // we're only counting them. not summing because a vein with 4 ore will be
            // just as likely as a vein with 1 ore
            totalVeinSizesCount += sizes.size();

        Map<Material, VeinProbability> veinSizesProbability = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> veinEntry : veinSizes.entrySet()) {
            veinSizesProbability.put(
                    DBUtils.getBlockName(veinEntry.getKey()),
                    new VeinProbability(veinEntry.getValue(), totalVeinSizesCount)
            );
        }
        int totalDistributionSum = 0;
        for (Integer i : desiredBlockDistribution.values()) totalDistributionSum += i;
        Map<Material, Double> desiredBlockDistributionPerc = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : desiredBlockDistribution.entrySet()) {
            desiredBlockDistributionPerc.put(
                    DBUtils.getBlockName(entry.getKey()), ((double) entry.getValue()) / totalDistributionSum);
        }
        Map<Material, Integer> actualBlockCountReal = new HashMap<>();
        for (Map.Entry<Integer, Integer> block : actualBlockCount.entrySet()) {
            actualBlockCountReal.put(DBUtils.getBlockName(block.getKey()), block.getValue());
        }
        for (Material material : veinSizesProbability.keySet())
            actualBlockCountReal.putIfAbsent(material, 0);

        Map<Material, Double> hostBlockDistribution = new HashMap<>();
        totalDistributionSum = 0;
        for (Integer i : hostBlocks.values()) totalDistributionSum += i;
        for (Map.Entry<Integer, Integer> entry : hostBlocks.entrySet()) {
            hostBlockDistribution.put(DBUtils.getBlockName(entry.getKey()), ((double) entry.getValue()) / totalDistributionSum);
        }

        return new RegenSectionInfo(actualBlockCountReal, hostBlockDistribution, veinSizesProbability, desiredBlockDistributionPerc, uid);
    }

    public void addActualBlockCount(int blockName, int blockCount) {
        actualBlockCount.put(blockName, blockCount);
    }

    public void addHostBlock(int blockName, int blockCount) {
        hostBlocks.put(blockName, blockCount);
    }

    public void addVeinSizes(int blockName, int desiredCount) {
        final int material = blockName;
        veinSizes.putIfAbsent(material, new ArrayList<>());
        veinSizes.get(material).add(desiredCount);
    }

    public void addDesiredOreCount(int blockName, int desiredCount) {
        desiredBlockDistribution.put(blockName, desiredCount);
    }
}
