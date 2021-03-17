package apple.voltskiya.plugin.ore_regen.regen;

import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import apple.voltskiya.plugin.ore_regen.sql.DBUtils;
import apple.voltskiya.plugin.utils.Pair;
import apple.voltskiya.plugin.utils.Triple;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionInfo {
    private static final Object ORE_SYNC = new Object();
    private static final Object AIR_SYNC = new Object();
    private Map<Material, Integer> actualBlockCount;
    private long totalActualHostBlocks;
    private long totalActualOreBlocks;
    private final Map<Material, Double> hostBlocks;
    private final Map<Material, VeinProbability> veinSizesProbability;
    private final Map<Material, Double> desiredBlockDistributionPerc;
    private final long uid;
    private int randomOreTodo = 0;
    private int randomAirTodo = 0;
    private final static Random random = new Random();

    public RegenSectionInfo(Map<Material, Integer> actualBlockCount,
                            Map<Material, Double> hostBlocks,
                            Map<Material, VeinProbability> veinSizesProbability,
                            Map<Material, Double> desiredBlockDistributionPerc,
                            long uid) {
        this.actualBlockCount = actualBlockCount;
        this.hostBlocks = hostBlocks;
        this.veinSizesProbability = veinSizesProbability;
        this.desiredBlockDistributionPerc = desiredBlockDistributionPerc;
        this.uid = uid;
        this.totalActualHostBlocks = 0;
        this.totalActualOreBlocks = 0;
        for (Map.Entry<Material, Integer> count : actualBlockCount.entrySet()) {
            if (count.getKey() != Material.AIR) {
                if (hostBlocks.containsKey(count.getKey())) this.totalActualHostBlocks += count.getValue();
                else this.totalActualOreBlocks += count.getValue();
            }
        }
    }


    public synchronized void update(Map<Material, Integer> blocks) {
        this.totalActualHostBlocks = 0;
        for (Map.Entry<Material, Integer> count : blocks.entrySet()) {
            if (count.getKey() != Material.AIR) this.totalActualHostBlocks += count.getValue();
        }
        this.actualBlockCount = blocks;
    }

    public synchronized void randomOreTodoIncrement() {
        this.randomOreTodo++;
    }

    /**
     * get a number that explains how far from perfect this section is
     */
    public synchronized double oreSadness() {
        double sadness = 0;
        for (Material material : desiredBlockDistributionPerc.keySet()) {
            if (!hostBlocks.containsKey(material)) {
                if (totalActualHostBlocks + totalActualOreBlocks != 0) {
                    sadness += (((double) actualBlockCount.getOrDefault(material, 0) -
                            desiredBlockDistributionPerc.get(material)) / (totalActualHostBlocks + totalActualOreBlocks));
                }
            }
        }
        return Math.max(0, sadness);
    }

    public void oreRandomExecute() {
        synchronized (ORE_SYNC) {
            double[] oreTypeChoices = new double[randomOreTodo];
            for (int i = 0; i < oreTypeChoices.length; i++) {
                oreTypeChoices[i] = random.nextDouble();
            }

            OreVein[] oreChoices = new OreVein[randomOreTodo];
            int index = 0;
            final List<Pair<Material, Double>> weightedDesire = getWeightedDesire();

            for (double oreTypeChoice : oreTypeChoices) {
                for (Pair<Material, Double> myDesire : weightedDesire) {
                    oreTypeChoice -= myDesire.getValue();
                    if (oreTypeChoice < 0) {
                        oreChoices[index++] = new OreVein(myDesire.getKey(), true);
                        break;
                    }
                }
            }
            this.randomOreTodo = 0;
            try {
                DBRegen.findRegenOre(uid, oreChoices);
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
    }


    public synchronized void randomAirTodoIncrement() {
        this.randomAirTodo++;
    }

    public synchronized double airSadness() {
        final long realTotal = totalActualHostBlocks + totalActualOreBlocks + actualBlockCount.getOrDefault(Material.AIR, 0);
        if (realTotal == 0) return 0;
        return ((double) actualBlockCount.getOrDefault(Material.AIR, 0)) / realTotal;
    }

    public void airRandomExecute() {
        synchronized (AIR_SYNC) {
            double[] airTypeChoices = new double[randomAirTodo];
            for (int i = 0; i < airTypeChoices.length; i++) {
                airTypeChoices[i] = random.nextDouble();
            }

            OreVein[] oreChoices = new OreVein[randomAirTodo];
            int index = 0;
            final List<Pair<Material, Double>> weightedDesire = getHostBlockWeightedDesire();

            for (double oreTypeChoice : airTypeChoices) {
                for (Pair<Material, Double> myDesire : weightedDesire) {
                    oreTypeChoice -= myDesire.getValue();
                    if (oreTypeChoice < 0) {
                        oreChoices[index++] = new OreVein(myDesire.getKey(), false);
                        break;
                    }
                }
            }
            this.randomAirTodo = 0;
            try {
                DBRegen.findRegenAir(uid, oreChoices);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            for (OreVein ore : oreChoices) {
                if (ore != null) {
                    ore.populateInit();
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(VoltskiyaPlugin.get(), () -> {
                for (OreVein ore : oreChoices) {
                    if (ore != null) ore.populate();
                }
            }, 0);
        }
    }

    private synchronized List<Pair<Material, Double>> getWeightedDesire() {
        Map<Material, Pair<Double, Double>> desireAndReality = new HashMap<>();
        for (Map.Entry<Material, Double> entry : desiredBlockDistributionPerc.entrySet()) {
            if (!hostBlocks.containsKey(entry.getKey())) {
                desireAndReality.put(
                        entry.getKey(),
                        new Pair<>(
                                ((double) actualBlockCount.getOrDefault(entry.getKey(), 0)) / (totalActualHostBlocks + totalActualOreBlocks),
                                entry.getValue()
                        )
                );
            }
        }
        return convertDifferenceToWeighted(desireAndReality);
    }

    private synchronized List<Pair<Material, Double>> getHostBlockWeightedDesire() {
        Map<Material, Pair<Double, Double>> desireAndReality = new HashMap<>();
        for (Map.Entry<Material, Double> entry : hostBlocks.entrySet()) {
            desireAndReality.put(
                    entry.getKey(),
                    new Pair<>(
                            totalActualHostBlocks == 0 ?
                                    0 :
                                    ((double) actualBlockCount.getOrDefault(entry.getKey(), 0)) / totalActualHostBlocks,
                            entry.getValue()
                    )
            );
        }
        return convertDifferenceToWeighted(desireAndReality);
    }

    @NotNull
    private synchronized static List<Pair<Material, Double>> convertDifferenceToWeighted(Map<Material, Pair<Double, Double>> desireAndReality) {
        List<Pair<Material, Double>> weightedDesire = new ArrayList<>();
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
        for (Pair<Material, Double> desire : weightedDesire)
            total += Math.max(desire.getValue(), 0); //don't add negative desires
        for (Pair<Material, Double> desire : weightedDesire) {
            if (total == 0) desire.setValue(0d);
            else desire.setValue(desire.getValue() / total);
        }
        return weightedDesire;
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

    public class OreVein {
        private int veinSize;
        private int x = 0;
        private int y = 0;
        private int z = 0;
        private UUID worldUid = null;
        private final List<Triple<Integer, Integer, Integer>> populateMe = new ArrayList<>();
        private int myWorldUid = -1;
        private Material blockType;
        private int myBlockType = -1;
        private Material oldBlockType = null;
        private int myOldBlockType = -1;
        private final boolean isOre;
        private boolean complete = false;

        public OreVein(Material blockType, boolean isOre) {
            this.isOre = isOre;
            this.blockType = blockType;
            if (veinSizesProbability.containsKey(blockType)) {
                VeinProbability probability = veinSizesProbability.get(blockType);
                this.veinSize = probability.choose();
            } else veinSize = 1;
        }

        public void setCoords(int x, int y, int z, int worldUid, int oldBlockType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.myWorldUid = worldUid;
            this.myOldBlockType = oldBlockType;
            this.complete = true;
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
                    DBRegen.setBlockToRegen(uid, c.getX(), c.getY(), c.getZ(), worldUid, blockType, oldBlockType, isOre);
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
            if (!populateMe.isEmpty()) {
                System.out.println("Populate [" + blockType.name() + "]at " + x + ", " + y + ", " + z);
                for (Triple<Integer, Integer, Integer> c : populateMe) {
                    world.getBlockAt(c.getX(), c.getY(), c.getZ()).setType(blockType);
                }
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

        public void updateWorldAndBlock() throws SQLException {
            if (this.complete) {
                if (this.worldUid == null)
                    this.worldUid = DBUtils.getRealWorldUid(myWorldUid);
                else
                    this.myWorldUid = DBUtils.getMyWorldUid(worldUid.toString());
                if (this.blockType == null)
                    this.blockType = DBUtils.getBlockName(myBlockType);
                else
                    this.myBlockType = DBUtils.getMyBlockUid(blockType);
                if (this.oldBlockType == null)
                    this.oldBlockType = DBUtils.getBlockName(myOldBlockType);
                else
                    this.myOldBlockType = DBUtils.getMyBlockUid(oldBlockType);
            }
        }
    }
}
