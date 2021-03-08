package apple.voltskiya.plugin.ore_regen.regen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VeinProbability {
    private final Map<Integer, Double> sizeToProbability = new HashMap<>();
    private final double myTotalProbability;
    private final static Random random = new Random();

    public VeinProbability(List<Integer> sizes, int totalVeinSizesCount) {
        Map<Integer, Integer> sizesToCount = new HashMap<>();
        for (Integer size : sizes) sizesToCount.compute(size, (k, v) -> v == null ? 1 : v + 1);
        int totalCount = 0;
        for (Map.Entry<Integer, Integer> sizeToCount : sizesToCount.entrySet()) {
            sizeToProbability.put(sizeToCount.getKey(), ((double) sizeToCount.getValue()) / totalVeinSizesCount);
            totalCount += sizeToCount.getValue();
        }
        myTotalProbability = ((double) totalCount) / totalVeinSizesCount;
    }

    public double getTotalProbability() {
        return myTotalProbability;
    }

    public int choose() {
        double choice = random.nextDouble();
        for (Map.Entry<Integer, Double> entry : sizeToProbability.entrySet()) {
            if (choice < entry.getValue()) {
                return entry.getKey();
            } else {
                choice -= entry.getValue();
            }
        }
        throw new IllegalStateException("VeinProbability should've been a better choice");
    }

    public double getAvgSize() {
        double size = 0;
        for (Map.Entry<Integer, Double> entry : sizeToProbability.entrySet()) {
            size += entry.getKey() * entry.getValue();
        }
        return size / getTotalProbability();
    }
}
