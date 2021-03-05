package apple.voltskiya.plugin.decay;


import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DecayModifiers {
    private static final String BLOCK_STRENGTHS_FILE = "blockStrengths.yml";

    private static final String BLOCKS_LIST = "blocks_list";
    private static final Map<Material, Material> decaySequence = new HashMap<>();
    private static final Map<Material, Integer> resistance = new HashMap<>();

    static {
        File dataFolder = PluginDecay.get().getDataFolder();
        File blockStrengths = new File(dataFolder, BLOCK_STRENGTHS_FILE);
        if (!blockStrengths.exists()) {
            try {
                blockStrengths.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(blockStrengths);
        @Nullable List<?> list = yamlConfiguration.getList(BLOCKS_LIST);
        for (Object sectionObj : list) {
            LinkedHashMap<?, ?> sectionList = (LinkedHashMap<?, ?>) sectionObj;
            for (Object element : sectionList.values()) {
                LinkedHashMap<?, ?> section = (LinkedHashMap<?, ?>) element;
                Material block = Material.getMaterial(section.get("block").toString());
                int thisResistance = (int) section.get("resistance");
                resistance.put(block, thisResistance);
                Object nextObj = section.get("next");
                if (nextObj != null) {
                    Material next = Material.getMaterial(nextObj.toString());
                    if (!next.isAir()) {
                        decaySequence.put(block, next);
                    }
                }
            }
        }
    }

    public static Material getNextDecay(Material material) {
        return decaySequence.getOrDefault(material, Material.AIR);
    }

    public static int getResistance(Material material) {
        if (material.isAir()) return 0;
        return resistance.getOrDefault(material, PluginDecay.DEFAULT_RESISTANCE);
    }
}
