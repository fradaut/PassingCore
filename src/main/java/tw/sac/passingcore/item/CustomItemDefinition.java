package tw.sac.passingcore.item;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CustomItemDefinition {

    private final String id;
    private final List<String> aliases;
    private final Material material;
    private final Component displayName;
    private final List<Component> lore;
    private final NamespacedKey itemTagKey;
    private final NamespacedKey itemModelKey;
    private final List<NamespacedKey> legacyItemTagKeys;
    private final List<NamespacedKey> legacyItemModelKeys;

    public CustomItemDefinition(String id, List<String> aliases, Material material, Component displayName,
                                List<Component> lore, NamespacedKey itemTagKey, NamespacedKey itemModelKey,
                                List<NamespacedKey> legacyItemTagKeys, List<NamespacedKey> legacyItemModelKeys) {
        this.id = id;
        this.aliases = List.copyOf(aliases);
        this.material = material;
        this.displayName = displayName;
        this.lore = List.copyOf(lore);
        this.itemTagKey = itemTagKey;
        this.itemModelKey = itemModelKey;
        this.legacyItemTagKeys = List.copyOf(legacyItemTagKeys);
        this.legacyItemModelKeys = List.copyOf(legacyItemModelKeys);
    }

    public String id() {
        return id;
    }

    public List<String> aliases() {
        return aliases;
    }

    public Component displayName() {
        return displayName;
    }

    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(material, amount);
        applyTo(item);
        item.setAmount(amount);
        return item;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        return hasMatchingTag(meta) && hasMatchingModel(meta);
    }

    public void applyTo(ItemStack item) {
        item.editMeta(meta -> {
            meta.itemName(displayName);
            meta.displayName(displayName);
            meta.lore(lore);
            meta.setItemModel(itemModelKey);
            meta.getPersistentDataContainer().set(itemTagKey, PersistentDataType.BYTE, (byte) 1);
            legacyItemTagKeys.forEach(meta.getPersistentDataContainer()::remove);
        });
    }

    private boolean hasMatchingTag(ItemMeta meta) {
        if (meta.getPersistentDataContainer().has(itemTagKey, PersistentDataType.BYTE)) {
            return true;
        }

        for (NamespacedKey key : legacyItemTagKeys) {
            if (meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMatchingModel(ItemMeta meta) {
        NamespacedKey itemModel = meta.getItemModel();
        if (itemModelKey.equals(itemModel)) {
            return true;
        }

        for (NamespacedKey key : legacyItemModelKeys) {
            if (key.equals(itemModel)) {
                return true;
            }
        }
        return false;
    }

    public static final class Builder {

        private final String id;
        private final Material material;
        private final Component displayName;
        private final List<String> aliases = new ArrayList<>();
        private final List<Component> lore = new ArrayList<>();
        private final List<String> legacyIds = new ArrayList<>();

        public Builder(String id, Material material, Component displayName) {
            this.id = id;
            this.material = material;
            this.displayName = displayName;
        }

        public Builder aliases(String... aliases) {
            this.aliases.addAll(List.of(aliases));
            return this;
        }

        public Builder lore(Component... lore) {
            this.lore.addAll(List.of(lore));
            return this;
        }

        public Builder legacyIds(String... legacyIds) {
            this.legacyIds.addAll(List.of(legacyIds));
            return this;
        }

        CustomItemDefinition build(org.bukkit.plugin.Plugin plugin) {
            List<NamespacedKey> legacyTagKeys = legacyIds.stream()
                    .map(legacyId -> new NamespacedKey(plugin, legacyId))
                    .toList();
            List<NamespacedKey> legacyModelKeys = legacyIds.stream()
                    .map(legacyId -> new NamespacedKey(plugin, legacyId))
                    .toList();

            return new CustomItemDefinition(
                    id,
                    aliases,
                    material,
                    displayName,
                    lore,
                    new NamespacedKey(plugin, id),
                    new NamespacedKey(plugin, id),
                    legacyTagKeys,
                    legacyModelKeys);
        }
    }
}
