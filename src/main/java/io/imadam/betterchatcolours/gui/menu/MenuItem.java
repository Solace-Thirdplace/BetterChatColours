package io.imadam.betterchatcolours.gui.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/**
 * A button or decoration placed in a {@link Menu} slot.
 */
public abstract class MenuItem {

  private Menu menu;

  /**
   * Builds the item shown in the slot. Called every time the menu is rendered.
   */
  public abstract ItemStack getItem(Player player);

  /**
   * Called on the server thread when the viewer clicks this item. Does nothing by default.
   */
  public void handleClick(ClickType clickType, Player player) {
  }

  /**
   * The menu this item is placed in.
   */
  protected Menu getMenu() {
    return menu;
  }

  void bind(Menu menu) {
    this.menu = menu;
  }

  public static MenuItem simple(ItemBuilder builder) {
    return new MenuItem() {
      @Override
      public ItemStack getItem(Player player) {
        return builder.build();
      }
    };
  }
}
