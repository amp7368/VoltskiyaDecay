package apple.voltskiya.plugin.ore_regen.regen;


import apple.voltskiya.plugin.ore_regen.PluginOreRegen;
import apple.voltskiya.plugin.ore_regen.sql.DBRegen;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.*;

public class RegenSectionManager {
    private final static Random random = new Random();
    private static Map<Long, RegenSectionInfo> sectionInfos = new HashMap<>();
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
                for (RegenSectionInfo sInfo : sectionInfos1) {
                    sectionInfos.put(sInfo.getUid(), sInfo);
                }
            } else {
                sectionInfo.update(blocks);
            }
        }
    }

    public static void randomOre() {
        Map<RegenSectionInfo, Double> sadnesses = new HashMap<>();
        synchronized (SECTION_INFOS_SYNC) {
            if (sectionInfos.isEmpty()) return;
            double totalSadness = 0;
            for (RegenSectionInfo sectionInfo : sectionInfos.values()) {
                final double sadness = sectionInfo.oreSadness();
                totalSadness += Math.max(0, sadness);
                sadnesses.put(sectionInfo, sadness);
            }
            final double r = random.nextDouble();
            //x^(1/3)*100*r^(r*2)
            int size = (int) Math.min(PluginOreRegen.REGEN_MAX_COUNT,
                    Math.pow(totalSadness, 1d / (PluginOreRegen.ORE_REGEN_INTENSITY))
                            * PluginOreRegen.ORE_REGEN_MULTIPLIER *
                            Math.pow(r, PluginOreRegen.ORE_REGEN_RANDOMNESS * r));
            double[] sadnessChoices = new double[size];
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
                if (currentSection.getValue() < 0) {
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                        i--;
                        continue;
                    } else break;
                }
                double currentSadnessValue = sadnessChoices[i];
                if (currentSadnessValue - offset <= currentSection.getValue()) {
                    // stay here and make ppl more sad
                    currentSection.getKey().randomOreTodoIncrement();
                } else {
                    // go to the next sad ppl
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                    } else break;
                    i--;
                    offset += currentSection.getValue();
                }
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
                final double sadness = Math.max(0, sectionInfo.airSadness());
                totalSadness += sadness;
                sadnesses.put(sectionInfo, sadness);
            }
            final double r = random.nextDouble();
            //x^(1/3)*100*r^(r*2)
            int size = (int) Math.min(PluginOreRegen.REGEN_MAX_COUNT,
                    Math.pow(totalSadness, 1d / (PluginOreRegen.AIR_REGEN_INTENSITY))
                            * PluginOreRegen.AIR_REGEN_MULTIPLIER *
                            Math.pow(r, PluginOreRegen.AIR_REGEN_RANDOMNESS * r));
            double[] sadnessChoices = new double[size];
            for (int i = 0; i < sadnessChoices.length; i++) {
                sadnessChoices[i] = random.nextDouble() * totalSadness;
            }
            Arrays.sort(sadnessChoices);

            Iterator<Map.Entry<RegenSectionInfo, Double>> iterator = sadnesses.entrySet().iterator();
            Map.Entry<RegenSectionInfo, Double> currentSection = iterator.next();
            double offset = 0;
            for (int i = 0; i < sadnessChoices.length; i++) {
                if (currentSection.getValue() < 0) {
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                        i--;
                        continue;
                    } else break;
                }
                double currentSadnessValue = sadnessChoices[i];
                if (currentSadnessValue - offset <= currentSection.getValue()) {
                    // stay here and make ppl more sad
                    currentSection.getKey().randomAirTodoIncrement();
                } else {
                    // go to the next sad ppl
                    if (iterator.hasNext()) {
                        currentSection = iterator.next();
                    } else break;
                    i--;
                    offset += currentSection.getValue();
                }
            }
        }
        for (RegenSectionInfo sectionInfo : sadnesses.keySet()) {
            sectionInfo.airRandomExecute();
        }
    }
}
