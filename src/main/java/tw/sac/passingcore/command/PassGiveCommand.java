package tw.sac.passingcore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tw.sac.passingcore.item.CustomItemDefinition;
import tw.sac.passingcore.item.CustomItemRegistry;

public final class PassGiveCommand implements CommandExecutor, TabCompleter {

    private final CustomItemRegistry itemRegistry;

    public PassGiveCommand(CustomItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        GiveRequest request = parseRequest(sender, label, args);
        if (request == null) {
            return true;
        }

        ItemStack item = request.item().createItem(1);
        request.target().getInventory().addItem(item).values()
                .forEach(leftover -> request.target().getWorld().dropItemNaturally(request.target().getLocation(), leftover));
        request.target().sendMessage(Component.text("你獲得了 ", NamedTextColor.GREEN)
                .append(request.item().displayName()));

        if (!request.target().equals(sender)) {
            sender.sendMessage(Component.text("已給予 ", NamedTextColor.GREEN)
                    .append(Component.text(request.target().getName(), NamedTextColor.WHITE))
                    .append(Component.text(" "))
                    .append(request.item().displayName()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if (sender instanceof Player) {
                suggestions.addAll(itemRegistry.itemIds());
            }
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
            return matching(suggestions, args[0]);
        }

        if (args.length == 2 && shouldCompleteItems(sender, args[0])) {
            return matching(itemRegistry.itemIds(), args[1]);
        }

        return List.of();
    }

    private GiveRequest parseRequest(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return null;
        }

        if (args.length > 2) {
            sendUsage(sender, label);
            return null;
        }

        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage(Component.text("伺服器端必須指定玩家：/" + label + " <player> <item>", NamedTextColor.RED));
            return null;
        }

        Player target;
        String itemId;
        Optional<CustomItemDefinition> selfItem = sender instanceof Player && args.length == 1
                ? itemRegistry.find(args[0])
                : Optional.empty();

        if (selfItem.isPresent()) {
            target = (Player) sender;
            itemId = args[0];
        } else {
            if (args.length < 2) {
                sender.sendMessage(Component.text("未知物品：" + args[0], NamedTextColor.RED));
                sendUsage(sender, label);
                return null;
            }

            target = Bukkit.getPlayerExact(args[0]);
            itemId = args[1];
            if (target == null) {
                sender.sendMessage(Component.text("找不到在線玩家：" + args[0], NamedTextColor.RED));
                return null;
            }
        }

        Optional<CustomItemDefinition> item = itemRegistry.find(itemId);
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("未知物品：" + itemId, NamedTextColor.RED));
            sender.sendMessage(Component.text("可用物品：" + String.join(", ", itemRegistry.itemIds()), NamedTextColor.GRAY));
            return null;
        }

        return new GiveRequest(target, item.get());
    }

    private boolean shouldCompleteItems(CommandSender sender, String firstArgument) {
        return !(sender instanceof Player) || itemRegistry.find(firstArgument).isEmpty();
    }

    private List<String> matching(List<String> values, String input) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void sendUsage(CommandSender sender, String label) {
        if (sender instanceof Player) {
            sender.sendMessage(Component.text("用法：/" + label + " <item> 或 /" + label + " <player> <item>",
                    NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("用法：/" + label + " <player> <item>", NamedTextColor.YELLOW));
    }

    private record GiveRequest(Player target, CustomItemDefinition item) {
    }
}
