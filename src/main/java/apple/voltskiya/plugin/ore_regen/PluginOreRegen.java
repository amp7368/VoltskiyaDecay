package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.BrushExecution;
import apple.voltskiya.plugin.ore_regen.brush.BrushUsageListener;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenListener;
import apple.voltskiya.plugin.ore_regen.player_intervention.RegenPlayerInterventionListener;
import apple.voltskiya.plugin.ore_regen.player_intervention.RegenPlayerInterventionMonitor;
import apple.voltskiya.plugin.ore_regen.regen.RegenHeartbeat;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionManager;
import apple.voltskiya.plugin.ore_regen.sql.GhastFireball;
import apple.voltskiya.plugin.ore_regen.sql.VerifyRegenDB;


public class PluginOreRegen extends VoltskiyaModule {
    public static final int BRUSH_EXECUTE_INTERVAL = 20;
    public static final long INTERVENTION_EXECUTE_INTERVAL = 40;

    public static long UPDATE_DECAY_DB_INTERVAL = 2000;

    public static long DECAY_INTERVAL = 20;
    public static float DECAY_INTENSITY = .002f;
    public static final float DECAY_PERCENTAGE = .01f;
    public static final int DEFAULT_RESISTANCE = 10;
    public static final int SUPPORT_RADIUS = 7;

    public static int REGEN_MAX_COUNT = 100;
    public static long REGEN_INTERVAL = 20;

    public static double ORE_REGEN_MULTIPLIER = 1;
    public static double ORE_REGEN_INTENSITY = 3;
    public static double ORE_REGEN_RANDOMNESS = 2;
    public static double AIR_REGEN_MULTIPLIER = 10;
    public static double AIR_REGEN_INTENSITY = 3;
    public static double AIR_REGEN_RANDOMNESS = 2;

    @Override
    public void enable() {
        VerifyRegenDB.initialize();
        RegenSectionManager.initialize();
        new InventoryRegenListener();
        new BrushUsageListener();
        new RegenPlayerInterventionListener();
        new RegenCommand();
        RegenHeartbeat.startBeating();
        ActiveBrush.pruneHeartbeat();
        BrushExecution.heartbeat();
        RegenPlayerInterventionMonitor.heartbeat();
    }


    @Override
    public String getName() {
        return "Regen";
    }
}
