package apple.voltskiya.plugin.ore_regen.regen;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import apple.voltskiya.plugin.utils.Pair;
import apple.voltskiya.plugin.utils.Triple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionInfo {
    private Map<Material, Integer> actualBlockCount;
    private long totalActualBlocks;
    private final Set<Material> hostBlocks;
    private final Map<Material, VeinProbability> veinSizesProbability;
    private Map<Material, Double> desiredBlockDistributionPerc;
    private final long uid;
    private int randomTodo = 0;
    private final static Random random = new Random();

    public RegenSectionInfo(Map<Material, Integer> actualBlockCount,
                            int totalActualBlocks,
                            Set<Material> hostBlocks,
                            Map<Material, VeinProbability> veinSizesProbability,
                            Map<Material, Double> desiredBlockDistributionPerc,
                            long uid) {
        this.actualBlockCount = actualBlockCount;
        this.totalActualBlocks = totalActualBlocks;
        this.hostBlocks = hostBlocks;
        this.veinSizesProbability = veinSizesProbability;
        this.desiredBlockDistributionPerc = desiredBlockDistributionPerc;
        this.uid = uid;
    }

    public long getTotalActualBlocks() {
        return totalActualBlocks;
    }

    /**
     * get a number that explains how far from perfect this section is
     */
    public double sadness() {
        double sadness = 0;
        for (Material material : desiredBlockDistributionPerc.keySet()) {
            if (!hostBlocks.contains(material)) {
                if (totalActualBlocks != 0) {
                    sadness += (desiredBlockDistributionPerc.get(material) - (((double) actualBlockCount.getOrDefault(material, 0)) / totalActualBlocks));
                }
            }
        }

        return Math.max(0, sadness);
    }

    public int sadnessCount() {
        double count = 0;
        for (Material material : desiredBlockDistributionPerc.keySet()) {
            if (!hostBlocks.contains(material) && veinSizesProbability.containsKey(material)) {
                count += Math.max(
                        (desiredBlockDistributionPerc.get(material) * totalActualBlocks -
                                actualBlockCount.getOrDefault(material, 0)) / veinSizesProbability.get(material).getAvgSize(),
                        0);
            }
        }
        return (int) (count);
    }


    public long getUid() {
        return uid;
    }

    @Override
    public int hashCode() {
        return (int) (uid % Integer.MAX_VALUE);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RegenSectionInfo && this.uid == ((RegenSectionInfo) obj).uid;
    }

    public void update(Material blockName, int change) {
        this.actualBlockCount.compute(blockName, (k, v) -> v == null ? change : v + change);
    }

    public void randomTodoIncrement() {
        this.randomTodo++;
    }

    public void randomExecute() {
        double[] oreTypeChoices = new double[randomTodo];
        for (int i = 0; i < oreTypeChoices.length; i++) {
            oreTypeChoices[i] = random.nextDouble();
        }

        OreVein[] oreChoices = new OreVein[randomTodo];
        int index = 0;
        final List<Pair<Material, Double>> weightedDesire = getWeightedDesire();

        for (double oreTypeChoice : oreTypeChoices) {
            for (Pair<Material, Double> myDesire : weightedDesire) {
                oreTypeChoice -= myDesire.getValue();
                if (oreTypeChoice < 0) {
                    oreChoices[index++] = new OreVein(myDesire.getKey());
                    break;
                }
            }
        }

        this.randomTodo = 0;
        try {
            DBRegen.regen(uid, oreChoices);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        for (OreVein ore : oreChoices) {
            if (ore != null) ore.populateInit();
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
            for (OreVein ore : oreChoices) {
                if (ore != null) ore.populate();
            }
        }, 0);

    }

    private List<Pair<Material, Double>> getWeightedDesire() {
        List<Pair<Material, Double>> weightedDesire = new ArrayList<>();
        Map<Material, Pair<Double, Double>> desireAndReality = new HashMap<>();
        for (Map.Entry<Material, Double> entry : desiredBlockDistributionPerc.entrySet()) {
            if (!hostBlocks.contains(entry.getKey())) {
                desireAndReality.put(
                        entry.getKey(),
                        new Pair<>(
                                ((double) actualBlockCount.getOrDefault(entry.getKey(), 0)) / totalActualBlocks,
                                entry.getValue()
                        )
                );
            }
        }
        for (Map.Entry<Material, Pair<Double, Double>> entry : desireAndReality.entrySet()) {
            weightedDesire.add(new Pair<>(entry.getKey(), entry.getValue().getValue() - entry.getValue().getKey()));
        }
        weightedDesire.sort((o1, o2) -> {
            double diff = o1.getValue() - o2.getValue();
            if (diff > 0) return 1;
            else if (diff == 0) return 0;
            return -1;
        });
        // normalize back to having a total of 1 desire
        double total = 0;
        for (Pair<Material, Double> desire : weightedDesire) total += desire.getValue();
        for (Pair<Material, Double> desire : weightedDesire) desire.setValue(desire.getValue() / total);
        return weightedDesire;
    }

    public void update(Map<Material, Integer> blocks) {
        this.totalActualBlocks = 0;
        for (Integer count : blocks.values()) {
            this.totalActualBlocks += count;
        }
        this.actualBlockCount=blocks;
    }

    public class OreVein {
        private int veinSize;
        private final Material blockType;
        private Material oldBlockType = Material.AIR;
        private int x = 0;
        private int y = 0;
        private int z = 0;
        private UUID worldUid = null;
        private boolean complete = false;
        private final List<Triple<Integer, Integer, Integer>> populateMe = new ArrayList<>();

        public OreVein(Material blockType) {
            this.blockType = blockType;
            VeinProbability probability = veinSizesProbability.get(blockType);
            veinSize = probability.choose();
        }

        public void setCoords(int x, int y, int z, UUID worldUid, Material oldBlockType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldUid = worldUid;
            this.oldBlockType = oldBlockType;
            complete = true;
        }

        public void populateInit() {
            if (!complete) return;

            int radius = 0;
            List<Triple<Integer, Integer, Integer>> xyz = sphere(radius);

            while (veinSize-- != 0) {
                Triple<Integer, Integer, Integer> c = xyz.remove(random.nextInt(xyz.size()));
                c.setX(c.getX() + x);
                c.setY(c.getY() + y);
                c.setZ(c.getZ() + z);
                try {
                    DBRegen.setBlock(uid, c.getX(), c.getY(), c.getZ(), worldUid, blockType, oldBlockType);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                populateMe.add(c);
                if (xyz.isEmpty()) xyz = sphere(++radius);
            }
        }

        public void populate() {
            World world = Bukkit.getWorld(worldUid);
            if (world == null) return;
            for (Triple<Integer, Integer, Integer> c : populateMe) {
                System.out.println("POPULATE AT " + c.getX() + " " + c.getY() + " " + c.getZ());
                world.getBlockAt(c.getX(), c.getY(), c.getZ()).setType(blockType);
            }
        }

        private List<Triple<Integer, Integer, Integer>> sphere(double radius) {
            if (radius == 0) return new ArrayList<>(1) {{
                add(new Triple<>(0, 0, 0));
            }};
            radius += 0.5;
            Set<Triple<Integer, Integer, Integer>> xyz = new HashSet<>();
            double resolution = 180d / (radius * radius);
            for (double xIncrement = 0; xIncrement < 360; xIncrement += resolution) {
                for (double yIncrement = 0; yIncrement < 360; yIncrement += resolution) {
                    for (double zIncrement = 0; zIncrement < 360; zIncrement += resolution) {
                        xyz.add(
                                new Triple<>(
                                        (int) (Math.sin(Math.toRadians(xIncrement)) * radius),
                                        (int) (Math.sin(Math.toRadians(yIncrement)) * radius),
                                        (int) (Math.sin(Math.toRadians(zIncrement)) * radius)
                                )
                        );
                    }
                }
            }
            return new ArrayList<>(xyz);
        }
    }
}
