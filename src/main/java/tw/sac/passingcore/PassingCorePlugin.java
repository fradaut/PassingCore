package tw.sac.passingcore;

import java.util.Objects;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import tw.sac.passingcore.command.PassGiveCommand;
import tw.sac.passingcore.item.CustomItemListener;
import tw.sac.passingcore.item.CustomItemRegistry;

public final class PassingCorePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        CustomItemRegistry itemRegistry = CustomItemRegistry.createDefault(this);
        PassGiveCommand passGiveCommand = new PassGiveCommand(itemRegistry);
        PluginCommand command = Objects.requireNonNull(getCommand("passgive"),
                "passgive command is missing from plugin.yml");

        command.setExecutor(passGiveCommand);
        command.setTabCompleter(passGiveCommand);
        getServer().getPluginManager().registerEvents(new CustomItemListener(itemRegistry), this);
    }
}
