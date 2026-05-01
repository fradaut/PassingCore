package tw.sac.passingcore.item;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class CustomItemRegistry {

    private final Map<String, CustomItemDefinition> itemsById = new LinkedHashMap<>();
    private final Map<String, CustomItemDefinition> itemsByLookupKey = new LinkedHashMap<>();

    public static CustomItemRegistry createDefault(Plugin plugin) {
        CustomItemRegistry registry = new CustomItemRegistry();
        registry.register(new CustomItemDefinition.Builder(
                        "p_coin",
                        Material.PAPER,
                        Component.text("P Coin", NamedTextColor.GREEN)
                                .decoration(TextDecoration.BOLD, true)
                                .decoration(TextDecoration.ITALIC, false))
                .aliases("pcoin", "p-coin", "pc")
                .legacyIds("passing_core_token")
                .lore(Component.text("A decorative custom item.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false))
                .build(plugin));
        return registry;
    }

    public void register(CustomItemDefinition definition) {
        itemsById.put(definition.id(), definition);
        itemsByLookupKey.put(normalizeLookupKey(definition.id()), definition);
        for (String alias : definition.aliases()) {
            itemsByLookupKey.put(normalizeLookupKey(alias), definition);
        }
    }

    public Optional<CustomItemDefinition> find(String itemIdOrAlias) {
        return Optional.ofNullable(itemsByLookupKey.get(normalizeLookupKey(itemIdOrAlias)));
    }

    public List<String> itemIds() {
        return List.copyOf(itemsById.keySet());
    }

    public boolean isCustomItem(ItemStack item) {
        return findDefinition(item).isPresent();
    }

    public void normalizeInventory(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            Optional<CustomItemDefinition> definition = findDefinition(item);
            if (definition.isPresent()) {
                definition.get().applyTo(item);
                player.getInventory().setItem(slot, item);
            }
        }
    }

    private Optional<CustomItemDefinition> findDefinition(ItemStack item) {
        return itemsById.values().stream()
                .filter(definition -> definition.matches(item))
                .findFirst();
    }

    private String normalizeLookupKey(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < normalized.length() - 1) {
            return normalized.substring(namespaceSeparator + 1);
        }
        return normalized;
    }
}
