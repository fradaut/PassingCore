package tw.sac.passingcore;

import java.util.Objects;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tw.sac.passingcore.command.PassGiveCommand;
import tw.sac.passingcore.effect.FlightEffectService;
import tw.sac.passingcore.item.CustomItemListener;
import tw.sac.passingcore.item.CustomItemRegistry;
import tw.sac.passingcore.resourcepack.ResourcePackService;

public final class PassingCorePlugin extends JavaPlugin {

    private ResourcePackService resourcePackService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CustomItemRegistry itemRegistry = CustomItemRegistry.createDefault(this);
        FlightEffectService flightEffectService = new FlightEffectService(this);
        resourcePackService = new ResourcePackService(this);
        resourcePackService.start();
        PassGiveCommand passGiveCommand = new PassGiveCommand(itemRegistry);
        PluginCommand command = Objects.requireNonNull(getCommand("passgive"),
                "passgive command is missing from plugin.yml");

        command.setExecutor(passGiveCommand);
        command.setTabCompleter(passGiveCommand);
        getServer().getPluginManager().registerEvents(flightEffectService, this);
        getServer().getPluginManager().registerEvents(new CustomItemListener(itemRegistry, flightEffectService), this);
        getServer().getPluginManager().registerEvents(resourcePackService, this);
        flightEffectService.start();
    }

    @Override
    public void onDisable() {
        if (resourcePackService != null) {
            resourcePackService.close();
        }
    }
}
