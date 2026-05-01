package tw.sac.passingcore.item;

import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class CustomItemListener implements Listener {

    private final CustomItemRegistry itemRegistry;

    public CustomItemListener(CustomItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        itemRegistry.normalizeInventory(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!itemRegistry.isCustomItem(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(Result.DENY);
        event.setUseInteractedBlock(Result.DENY);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!itemRegistry.isCustomItem(event.getPlayer().getInventory().getItem(event.getHand()))) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (itemRegistry.isCustomItem(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (itemRegistry.isCustomItem(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (containsCustomItem(event.getInventory().getMatrix())) {
            event.getInventory().setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (containsCustomItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (containsCustomItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (containsCustomItem(event.getInventory().getContents())) {
            event.setResult(ItemStack.empty());
        }
    }

    private boolean containsCustomItem(ItemStack[] items) {
        for (ItemStack item : items) {
            if (itemRegistry.isCustomItem(item)) {
                return true;
            }
        }
        return false;
    }
}
