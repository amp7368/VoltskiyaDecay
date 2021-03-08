package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.BrushExecution;
import apple.voltskiya.plugin.ore_regen.brush.BrushUsageListener;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenListener;
import apple.voltskiya.plugin.ore_regen.regen.RegenHeartbeat;
import apple.voltskiya.plugin.ore_regen.regen.RegenSectionManager;
import apple.voltskiya.plugin.ore_regen.sql.VerifyRegenDB;


public class PluginOreRegen extends VoltskiyaModule {
    public static final int BRUSH_EXECUTE_INTERVAL = 1000;
    private PluginOreRegen instance;

    @Override
    public void enable() {
        instance = this;
        VerifyRegenDB.initialize();
        RegenSectionManager.initialize();
        new InventoryRegenListener();
        new BrushUsageListener();
        VoltskiyaPlugin.get().getCommandManager().registerCommand(new RegenCommand());
        RegenHeartbeat.startBeating();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(ActiveBrush.PRUNE_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("prune");
                ActiveBrush.prune();
            }
        }).start();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(BRUSH_EXECUTE_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BrushExecution.completeTodo();
            }
        }).start();
    }

    @Override
    public String getName() {
        return "Regen";
    }
}
