package io.imadam.betterchatcolours.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Routes clicks in open {@link Menu}s to their items.
 *
 * <p>Menu items are display-only: every click in the menu's own slots is cancelled, and so is
 * anything in the player's inventory that would move items into the menu or pull them out of it
 * (shift-click, collect-to-cursor, and drags that touch a menu slot). Other clicks in the player's
 * own inventory are left alone. The cancellation is applied first and again after other plugins
 * have handled the event, so menu items cannot be taken even if another plugin un-cancels it.
 *
 * <p>The item's click action runs on the next server tick, because Bukkit does not allow an
 * inventory to be closed or opened from inside an {@link InventoryClickEvent}. Until it has run,
 * further clicks in the same menu are ignored, so one click cannot trigger an action twice.
 */
public class MenuListener implements Listener {

  private final Consumer<Runnable> nextTick;

  public MenuListener(Plugin plugin) {
    this(task -> plugin.getServer().getScheduler().runTask(plugin, task));
  }

  MenuListener(Consumer<Runnable> nextTick) {
    this.nextTick = nextTick;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!(top.getHolder(false) instanceof Menu menu)) {
      return;
    }
    if (!isBlocked(event, top)) {
      return;
    }
    event.setCancelled(true);

    int rawSlot = event.getRawSlot();
    if (rawSlot >= top.getSize() || !(event.getWhoClicked() instanceof Player player) || menu.isClickPending()) {
      return;
    }
    if (menu.getItem(rawSlot) == null) {
      return;
    }

    ClickType clickType = event.getClick();
    menu.setClickPending(true);
    nextTick.accept(() -> {
      menu.setClickPending(false);
      menu.handleClick(rawSlot, clickType, player);
    });
  }

  /**
   * Cancels again after other plugins have seen the click, in case one of them un-cancelled it.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void enforceInventoryClick(InventoryClickEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (top.getHolder(false) instanceof Menu && isBlocked(event, top)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    enforceInventoryDrag(event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void enforceInventoryDrag(InventoryDragEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!(top.getHolder(false) instanceof Menu)) {
      return;
    }
    for (int rawSlot : event.getRawSlots()) {
      if (rawSlot < top.getSize()) {
        event.setCancelled(true);
        return;
      }
    }
  }

  /**
   * Whether a click in an open menu has to be cancelled: any click on a menu slot, and any click in
   * the player's inventory that would move items into or out of the menu.
   */
  private static boolean isBlocked(InventoryClickEvent event, Inventory top) {
    int rawSlot = event.getRawSlot();
    if (rawSlot >= 0 && rawSlot < top.getSize()) {
      return true;
    }
    InventoryAction action = event.getAction();
    return action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR;
  }
}
