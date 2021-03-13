package apple.voltskiya.plugin.ore_regen.regen;


import apple.voltskiya.plugin.ore_regen.PluginOreRegen;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionManager {
    private final static Random random = new Random();
    private static Map<Long, RegenSectionInfo> sectionInfos = new HashMap<>();
    private static long totalBlocks = 0;
    private final static Object SECTION_INFOS_SYNC = new Object();

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
        synchronized (SECTION_INFOS_SYNC) {
        }
    }

    public static void updateSectionInfo(long toolUid, Map<Material, Integer> blocks) {
        synchronized (SECTION_INFOS_SYNC) {
            final RegenSectionInfo sectionInfo = sectionInfos.get(toolUid);
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
            } else {
                totalBlocks -= sectionInfo.getTotalActualBlocks();
                sectionInfo.update(blocks);
                totalBlocks += sectionInfo.getTotalActualBlocks();
            }
        }
    }

    public static void randomOre() {
        Map<RegenSectionInfo, Double> sadnesses = new HashMap<>();
        synchronized (SECTION_INFOS_SYNC) {
            if (sectionInfos.isEmpty()) return;
            double totalSadness = 0;
            int sadnessCount = 0;
            for (RegenSectionInfo sectionInfo : sectionInfos.values()) {
                sadnessCount += sectionInfo.oreSadnessCount();
                final double sadness = sectionInfo.oreSadness();
                totalSadness += sadness;
                sadnesses.put(sectionInfo, sadness);
            }
            final int sadness = Math.min(PluginOreRegen.REGEN_MAX_COUNT, (int) (totalSadness * PluginOreRegen.ORE_REGEN_MULTIPLIER * sadnessCount *
                    Math.pow(random.nextDouble(), 1 / PluginOreRegen.ORE_REGEN_INTENSITY)
            ));
            double[] sadnessChoices = new double[sadness];
            for (int i = 0; i < sadnessChoices.length; i++) {
                sadnessChoices[i] = random.nextDouble() * totalSadness;
            }
            Arrays.sort(sadnessChoices);

            Iterator<Map.Entry<RegenSectionInfo, Double>> iterator = sadnesses.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Map.Entry<RegenSectionInfo, Double> currentSection = iterator.next();
            double offset = 0;
            for (int i = 0; i < sadnessChoices.length; i++) {
                double currentSadnessValue = sadnessChoices[i];
                if (currentSadnessValue - offset > currentSection.getValue()) {
                    // go to the next sad ppl
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                    } else break;
                    i--;
                } else {
                    // stay here and make ppl more sad
                    currentSection.getKey().randomOreTodoIncrement();
                }
                offset += currentSadnessValue;
            }
        }
        for (RegenSectionInfo sectionInfo : sadnesses.keySet()) {
            sectionInfo.oreRandomExecute();
        }
    }

    public static void randomAir() {
        Map<RegenSectionInfo, Double> sadnesses = new HashMap<>();
        synchronized (SECTION_INFOS_SYNC) {
            if (sectionInfos.isEmpty()) return;
            double totalSadness = 0;
            for (RegenSectionInfo sectionInfo : sectionInfos.values()) {
                final double sadness = sectionInfo.airSadness();
                totalSadness += sadness;
                sadnesses.put(sectionInfo, sadness);
            }
            double[] sadnessChoices = new double[
                    Math.min(PluginOreRegen.REGEN_MAX_COUNT, (int) (totalSadness * PluginOreRegen.AIR_REGEN_MULTIPLIER *
                            Math.pow(random.nextDouble(), -PluginOreRegen.AIR_REGEN_INTENSITY)
                    ))];
            for (int i = 0; i < sadnessChoices.length; i++) {
                sadnessChoices[i] = random.nextDouble() * totalSadness;
            }
            Arrays.sort(sadnessChoices);

            Iterator<Map.Entry<RegenSectionInfo, Double>> iterator = sadnesses.entrySet().iterator();
            Map.Entry<RegenSectionInfo, Double> currentSection = iterator.next();
            double offset = 0;
            for (int i = 0; i < sadnessChoices.length; i++) {
                double currentSadnessValue = sadnessChoices[i];
                if (currentSadnessValue - offset > currentSection.getValue()) {
                    // go to the next sad ppl
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                    } else break;
                    i--;
                } else {
                    // stay here and make ppl more sad
                    currentSection.getKey().randomAirTodoIncrement();
                }
                offset += currentSadnessValue;
            }
        }
        for (RegenSectionInfo sectionInfo : sadnesses.keySet()) {
            sectionInfo.airRandomExecute();
        }
    }
}
