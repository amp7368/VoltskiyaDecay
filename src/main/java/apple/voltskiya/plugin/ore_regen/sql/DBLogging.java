package apple.voltskiya.plugin.ore_regen.sql;

import apple.voltskiya.plugin.decay.PluginDecay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DBLogging {
    private static final BufferedWriter writer;
    public static String DBRegen = "[DBRegen]";

    static {
        BufferedWriter writer1;
        try {
            writer1 = new BufferedWriter(new FileWriter(new File(PluginDecay.get().getDataFolder(), "DBLog.txt")));
        } catch (IOException e) {
            writer1 = null;
            e.printStackTrace();
        }
        writer = writer1;
    }

    public static void log(String prefix, String log) {
        try {
            writer.write(prefix + " " + log + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
