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

    private static final int DEFAULT_AMOUNT = 1;
    private static final int MIN_AMOUNT = 1;
    private static final int MAX_AMOUNT = 64;

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

        ItemStack item = request.item().createItem(request.amount());
        request.target().getInventory().addItem(item).values()
                .forEach(leftover -> request.target().getWorld().dropItemNaturally(request.target().getLocation(), leftover));
        request.target().sendMessage(Component.text("你獲得了 ", NamedTextColor.GREEN)
                .append(Component.text(request.amount() + "x ", NamedTextColor.WHITE))
                .append(request.item().displayName()));

        if (!request.target().equals(sender)) {
            sender.sendMessage(Component.text("已給予 ", NamedTextColor.GREEN)
                    .append(Component.text(request.target().getName(), NamedTextColor.WHITE))
                    .append(Component.text(" "))
                    .append(Component.text(request.amount() + "x ", NamedTextColor.WHITE))
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

        if ((args.length == 2 && sender instanceof Player && itemRegistry.find(args[0]).isPresent())
                || (args.length == 3 && shouldCompleteAmounts(sender, args[0], args[1]))) {
            return matching(List.of("1", "8", "16", "32", "64"), args[args.length - 1]);
        }

        return List.of();
    }

    private GiveRequest parseRequest(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return null;
        }

        if (args.length > 3) {
            sendUsage(sender, label);
            return null;
        }

        if (!(sender instanceof Player) && args.length < 2) {
            sender.sendMessage(Component.text("伺服器端必須指定玩家：/" + label + " <player> <item>", NamedTextColor.RED));
            return null;
        }

        ParsedArguments parsedArguments = parseArguments(sender, label, args);
        if (parsedArguments == null) {
            return null;
        }

        int amount = parsedArguments.amount();
        if (amount < MIN_AMOUNT || amount > MAX_AMOUNT) {
            sender.sendMessage(Component.text("數量必須介於 1 到 64。", NamedTextColor.RED));
            return null;
        }

        Optional<CustomItemDefinition> item = itemRegistry.find(parsedArguments.itemId());
        if (item.isEmpty()) {
            sender.sendMessage(Component.text("未知物品：" + parsedArguments.itemId(), NamedTextColor.RED));
            sender.sendMessage(Component.text("可用物品：" + String.join(", ", itemRegistry.itemIds()), NamedTextColor.GRAY));
            return null;
        }

        return new GiveRequest(parsedArguments.target(), item.get(), amount);
    }

    private ParsedArguments parseArguments(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return parseTargetedArguments(sender, args, 1);
        }

        Optional<CustomItemDefinition> firstArgumentItem = itemRegistry.find(args[0]);
        if (firstArgumentItem.isPresent()) {
            int amount = DEFAULT_AMOUNT;
            if (args.length == 2) {
                Optional<Integer> parsedAmount = parseAmount(args[1]);
                if (parsedAmount.isEmpty()) {
                    sender.sendMessage(Component.text("數量必須是數字：" + args[1], NamedTextColor.RED));
                    return null;
                }
                amount = parsedAmount.get();
            } else if (args.length > 2) {
                sendUsage(sender, label);
                return null;
            }

            return new ParsedArguments(player, args[0], amount);
        }

        return parseTargetedArguments(sender, args, 2);
    }

    private ParsedArguments parseTargetedArguments(CommandSender sender, String[] args, int minimumArguments) {
        if (args.length < minimumArguments) {
            sender.sendMessage(Component.text("未知物品：" + args[0], NamedTextColor.RED));
            return null;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("找不到在線玩家：" + args[0], NamedTextColor.RED));
            return null;
        }

        int amount = DEFAULT_AMOUNT;
        if (args.length == 3) {
            Optional<Integer> parsedAmount = parseAmount(args[2]);
            if (parsedAmount.isEmpty()) {
                sender.sendMessage(Component.text("數量必須是數字：" + args[2], NamedTextColor.RED));
                return null;
            }
            amount = parsedAmount.get();
        }

        return new ParsedArguments(target, args[1], amount);
    }

    private boolean shouldCompleteItems(CommandSender sender, String firstArgument) {
        return !(sender instanceof Player) || itemRegistry.find(firstArgument).isEmpty();
    }

    private boolean shouldCompleteAmounts(CommandSender sender, String firstArgument, String secondArgument) {
        if (!(sender instanceof Player)) {
            return itemRegistry.find(secondArgument).isPresent();
        }
        return itemRegistry.find(firstArgument).isPresent() || itemRegistry.find(secondArgument).isPresent();
    }

    private Optional<Integer> parseAmount(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
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
            sender.sendMessage(Component.text("用法：/" + label + " <item> [amount] 或 /" + label + " <player> <item> [amount]",
                    NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("用法：/" + label + " <player> <item> [amount]", NamedTextColor.YELLOW));
    }

    private record GiveRequest(Player target, CustomItemDefinition item, int amount) {
    }

    private record ParsedArguments(Player target, String itemId, int amount) {
    }
}
