package io.imadam.betterchatcolours.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

/**
 * Routes clicks in open {@link Menu}s to their items.
 *
 * <p>Menu items are display-only. While a menu is open every click and every drag is cancelled,
 * in the menu and in the player's own inventory alike, so no item can move into the menu or out
 * of it whatever kind of click the client sends, including kinds a later Minecraft version adds.
 * The cancellation is applied first and again after other plugins have handled the event, so menu
 * items cannot be taken even if another plugin un-cancels it. {@link #closeAll()} shuts every open
 * menu when the plugin is disabled, because a menu left open with no listener is an ordinary chest.
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
    if (top.getHolder(false) instanceof Menu) {
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
    if (top.getHolder(false) instanceof Menu) {
      event.setCancelled(true);
    }
  }

  /**
   * Closes every open menu. Called when the plugin is disabled: once this listener is unregistered
   * nothing protects a menu that is still open, and its icons could be taken as items.
   */
  public static void closeAll() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) {
        player.closeInventory();
      }
    }
  }
}
