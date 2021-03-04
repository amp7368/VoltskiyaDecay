package apple.voltskiya.plugin.decay;


import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecayModifiers {
    private static final String BLOCK_STREGTHS_FILE = "blockStrengths.yml";

    private static final String BLOCKS_LIST = "blocks_list";
    private static Map<Material, Material> decaySequence = new HashMap<>();
    private static Map<Material, Integer> resistance = new HashMap<>();

    static {
        File dataFolder = PluginDecay.get().getDataFolder();
        File blockStrengths = new File(dataFolder, BLOCK_STREGTHS_FILE);
        if (!blockStrengths.exists()) {
            try {
                blockStrengths.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(blockStrengths);
        List<?> list = yamlConfiguration.getList(BLOCKS_LIST);
        int a = 3;
    }
    public static Material getNextDecay(Material material){
        return decaySequence.getOrDefault(material,Material.AIR);
    }
    public static int getResistance(Material material){
        return resistance.getOrDefault(material,PluginDecay.DEFAULT_RESISTANCE);
    }
}
