package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.brush.ActiveBrush;
import apple.voltskiya.plugin.ore_regen.brush.BrushExecution;
import apple.voltskiya.plugin.ore_regen.brush.BrushUsageListener;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenListener;
import apple.voltskiya.plugin.ore_regen.gui.RegenCommand;
import apple.voltskiya.plugin.ore_regen.sql.VerifyRegenDB;


public class PluginOreRegen extends VoltskiyaModule {
    private PluginOreRegen instance;

    @Override
    public void enable() {
        instance = this;
        VerifyRegenDB.initialize();
        new InventoryRegenListener();
        new BrushUsageListener();
        VoltskiyaPlugin.get().getCommandManager().registerCommand(new RegenCommand());
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
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("complete todo");
                BrushExecution.completeTodo();
            }
        }).start();
    }

    @Override
    public String getName() {
        return "Regen";
    }
}
