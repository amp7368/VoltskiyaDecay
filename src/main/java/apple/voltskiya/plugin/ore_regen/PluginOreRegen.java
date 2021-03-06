package apple.voltskiya.plugin.ore_regen;

import apple.voltskiya.plugin.VoltskiyaModule;
import apple.voltskiya.plugin.VoltskiyaPlugin;
import apple.voltskiya.plugin.ore_regen.gui.InventoryRegenListener;
import apple.voltskiya.plugin.ore_regen.gui.RegenCommand;


public class PluginOreRegen extends VoltskiyaModule {
    private PluginOreRegen instance;

    @Override
    public void enable() {
        instance = this;
        new InventoryRegenListener();
        VoltskiyaPlugin.get().getCommandManager().registerCommand(new RegenCommand());
    }

    @Override
    public String getName() {
        return "Regen";
    }
}