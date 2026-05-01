package tw.sac.passingcore;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class PassingCorePlugin extends JavaPlugin implements Listener {

    private static final String ITEM_ID = "p_coin";
    private static final String LEGACY_ITEM_ID = "passing_core_token";
    private static final int DEFAULT_AMOUNT = 1;
    private static final int MAX_AMOUNT = 64;

    private NamespacedKey itemTagKey;
    private NamespacedKey itemModelKey;
    private NamespacedKey legacyItemTagKey;
    private NamespacedKey legacyItemModelKey;

    @Override
    public void onEnable() {
        this.itemTagKey = new NamespacedKey(this, ITEM_ID);
        this.itemModelKey = new NamespacedKey(this, ITEM_ID);
        this.legacyItemTagKey = new NamespacedKey(this, LEGACY_ITEM_ID);
        this.legacyItemModelKey = new NamespacedKey(this, LEGACY_ITEM_ID);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"passingitem".equalsIgnoreCase(command.getName())) {
            return false;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        int amount = parseAmount(args);
        if (amount < 1) {
            sender.sendMessage("Usage: /" + label + " [1-64]");
            return true;
        }

        normalizeDisplayItems(player);
        player.getInventory().addItem(createDisplayItem(amount)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        player.sendMessage(Component.text("你獲得了一個展示物品。", NamedTextColor.GREEN));
        return true;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        normalizeDisplayItems(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isDisplayItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(Result.DENY);
        event.setUseInteractedBlock(Result.DENY);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!isDisplayItem(event.getPlayer().getInventory().getItem(event.getHand()))) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (isDisplayItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDisplayItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (containsDisplayItem(event.getInventory().getMatrix())) {
            event.getInventory().setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (containsDisplayItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (containsDisplayItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (containsDisplayItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    private ItemStack createDisplayItem(int amount) {
        ItemStack item = new ItemStack(Material.PAPER, amount);
        normalizeDisplayItem(item);
        item.setAmount(amount);
        return item;
    }

    private void normalizeDisplayItems(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (isDisplayItem(item)) {
                normalizeDisplayItem(item);
                player.getInventory().setItem(slot, item);
            }
        }
    }

    private void normalizeDisplayItem(ItemStack item) {
        item.editMeta(meta -> {
            Component name = Component.text("P Coin", NamedTextColor.GREEN)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false);
            meta.itemName(name);
            meta.displayName(name);
            meta.lore(List.of(Component.text("A decorative custom item.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            meta.setItemModel(itemModelKey);
            meta.getPersistentDataContainer().set(itemTagKey, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().remove(legacyItemTagKey);
        });
    }

    private boolean isDisplayItem(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        boolean hasDisplayTag = meta.getPersistentDataContainer().has(itemTagKey, PersistentDataType.BYTE)
                || meta.getPersistentDataContainer().has(legacyItemTagKey, PersistentDataType.BYTE);
        NamespacedKey itemModel = meta.getItemModel();
        boolean hasDisplayModel = itemModelKey.equals(itemModel) || legacyItemModelKey.equals(itemModel);

        return hasDisplayTag && hasDisplayModel;
    }

    private boolean containsDisplayItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (isDisplayItem(item)) {
                return true;
            }
        }
        return false;
    }

    private int parseAmount(String[] args) {
        if (args.length == 0) {
            return DEFAULT_AMOUNT;
        }

        try {
            int amount = Integer.parseInt(args[0]);
            return amount >= 1 && amount <= MAX_AMOUNT ? amount : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
