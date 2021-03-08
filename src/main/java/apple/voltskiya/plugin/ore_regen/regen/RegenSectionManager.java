package apple.voltskiya.plugin.ore_regen.regen;


import apple.voltskiya.plugin.decay.PluginDecay;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionManager {
    private final static Random random = new Random();
    public static final int REGEN_MAX_COUNT = 100;
    private static Map<Long, RegenSectionInfo> sectionInfos = new HashMap<>();
    private static long totalBlocks = 0;

    static {
        Set<RegenSectionInfo> sectionInfos1;
        try {
            sectionInfos1 = DBRegen.getSections();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            sectionInfos1 = new HashSet<>();
        }
        for (RegenSectionInfo sectionInfo : sectionInfos1) {
            sectionInfos.put(sectionInfo.getUid(), sectionInfo);
        }
        for (RegenSectionInfo sectionInfo : sectionInfos.values())
            totalBlocks += sectionInfo.getTotalActualBlocks();
    }

    // make sure the static block is done
    public static void initialize() {

    }

    public synchronized static void updateSectionInfo(long uid, Material blockName, int change) {
        RegenSectionInfo sectionInfo = sectionInfos.get(uid);
        if (sectionInfo == null) {
            Set<RegenSectionInfo> sectionInfos1;
            try {
                sectionInfos1 = DBRegen.getSections();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                sectionInfos1 = new HashSet<>();
            }
            sectionInfos = new HashMap<>();
            totalBlocks = 0;
            for (RegenSectionInfo sInfo : sectionInfos1) {
                sectionInfos.put(sInfo.getUid(), sInfo);
                totalBlocks += sInfo.getTotalActualBlocks();
            }
        } else sectionInfo.update(blockName, change);
    }

    public static void updateSectionInfo(long toolUid, Map<Material, Integer> blocks) {
        final RegenSectionInfo sectionInfo = sectionInfos.get(toolUid);
        totalBlocks -= sectionInfo.getTotalActualBlocks();
        sectionInfo.update(blocks);
        totalBlocks += sectionInfo.getTotalActualBlocks();
    }

    public synchronized static void random() {
        if (sectionInfos.isEmpty()) return;
        Map<RegenSectionInfo, Double> sadnesses = new HashMap<>();
        double totalSadness = 0;
        int sadnessCount = 0;
        for (RegenSectionInfo sectionInfo : sectionInfos.values()) {
            sadnessCount += sectionInfo.sadnessCount();
            final double sadness = sectionInfo.sadness();
            System.out.println(sadness);
            totalSadness += sadness;
            sadnesses.put(sectionInfo, sadness);
        }
        double[] sadnessChoices = new double[
                Math.min(REGEN_MAX_COUNT, (int) (totalSadness * PluginDecay.REGEN_INTENITY * sadnessCount))
                ];
        for (int i = 0; i < sadnessChoices.length; i++) {
            sadnessChoices[i] = random.nextDouble() * totalSadness;
        }
        Arrays.sort(sadnessChoices);

        Iterator<Map.Entry<RegenSectionInfo, Double>> iterator = sadnesses.entrySet().iterator();
        Map.Entry<RegenSectionInfo, Double> currentSection = iterator.next();
        double offset = 0;
        int sadnessAddition = 0;
        for (int i = 0; i < sadnessChoices.length; i++) {
            if (currentSection.getKey().sadnessCount() - sadnessAddition <= 0) {
                if (iterator.hasNext()) {
                    sadnessAddition = 0;
                    currentSection = iterator.next();
                } else break;
                i--;
                continue;
            }
            double currentSadnessValue = sadnessChoices[i];
            if (currentSadnessValue - offset > currentSection.getValue()) {
                // go to the next sad ppl
                if (iterator.hasNext()) {
                    sadnessAddition = 0;
                    currentSection = iterator.next();
                } else break;
                i--;
            } else {
                // stay here and make ppl more sad
                sadnessAddition++;
                currentSection.getKey().randomTodoIncrement();
            }
        }
        for (RegenSectionInfo sectionInfo : sadnesses.keySet()) {
            sectionInfo.randomExecute();
        }

    }

}
