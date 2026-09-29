package io.imadam.betterchatcolours.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A chest menu built on the plain Bukkit inventory API.
 *
 * <p>The layout is given as rows of nine characters (spaces are ignored). Each character is mapped
 * to an item with {@link Builder#addIngredient}. Slots marked with the content character hold the
 * content list, filled left to right and top to bottom, one page at a time.
 *
 * <p>Clicks are routed here by {@link MenuListener}.
 */
public class Menu implements InventoryHolder {

  private final int rows;
  private final MenuItem[] items;
  private final List<Integer> contentSlots;
  private final List<? extends MenuItem> content;
  private int page;
  private Inventory inventory;
  private Player viewer;
  private boolean clickPending;

  private Menu(int rows, MenuItem[] items, List<Integer> contentSlots, List<? extends MenuItem> content) {
    this.rows = rows;
    this.items = items;
    this.contentSlots = contentSlots;
    this.content = content;
    for (MenuItem item : items) {
      if (item != null) {
        item.bind(this);
      }
    }
    for (MenuItem item : content) {
      item.bind(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates the inventory, fills it, and opens it for the player.
   */
  public void open(Player player, Component title) {
    this.viewer = player;
    this.inventory = Bukkit.createInventory(this, rows * 9, title);
    render();
    player.openInventory(inventory);
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }

  public Player getViewer() {
    return viewer;
  }

  public int getSize() {
    return rows * 9;
  }

  public int getPage() {
    return page;
  }

  /**
   * Number of pages needed for the content. Zero when there is no content.
   */
  public int getPageCount() {
    if (contentSlots.isEmpty()) {
      return 0;
    }
    return Math.ceilDiv(content.size(), contentSlots.size());
  }

  /**
   * Moves to the given page, clamped to the valid range, and redraws the menu.
   */
  public void setPage(int page) {
    this.page = Math.max(0, Math.min(page, getPageCount() - 1));
    render();
  }

  /**
   * The item shown in the given slot on the current page, or null if the slot is empty.
   */
  public MenuItem getItem(int slot) {
    if (slot < 0 || slot >= items.length) {
      return null;
    }
    int contentIndex = contentSlots.indexOf(slot);
    if (contentIndex == -1) {
      return items[slot];
    }
    int index = page * contentSlots.size() + contentIndex;
    return index < content.size() ? content.get(index) : null;
  }

  /**
   * Redraws every slot. Does nothing before the menu is opened.
   */
  public void render() {
    if (inventory == null) {
      return;
    }
    for (int slot = 0; slot < items.length; slot++) {
      MenuItem item = getItem(slot);
      ItemStack stack = item != null ? item.getItem(viewer) : null;
      inventory.setItem(slot, stack);
    }
  }

  /**
   * Runs the click action of the item in the given slot.
   */
  public void handleClick(int slot, ClickType clickType, Player player) {
    MenuItem item = getItem(slot);
    if (item != null) {
      item.handleClick(clickType, player);
    }
  }

  boolean isClickPending() {
    return clickPending;
  }

  void setClickPending(boolean clickPending) {
    this.clickPending = clickPending;
  }

  public static class Builder {

    private String[] structure;
    private final Map<Character, MenuItem> ingredients = new HashMap<>();
    private char contentMarker;
    private boolean hasContentMarker;
    private List<? extends MenuItem> content = List.of();

    public Builder setStructure(String... rows) {
      this.structure = rows;
      return this;
    }

    public Builder addIngredient(char key, MenuItem item) {
      ingredients.put(key, item);
      return this;
    }

    /**
     * Marks the slots that hold the content list.
     */
    public Builder setContentSlots(char key) {
      this.contentMarker = key;
      this.hasContentMarker = true;
      return this;
    }

    public Builder setContent(List<? extends MenuItem> content) {
      this.content = content;
      return this;
    }

    public Menu build() {
      if (structure == null || structure.length < 1 || structure.length > 6) {
        throw new IllegalStateException("A menu needs between one and six rows");
      }
      MenuItem[] items = new MenuItem[structure.length * 9];
      List<Integer> contentSlots = new ArrayList<>();
      for (int row = 0; row < structure.length; row++) {
        String line = structure[row].replace(" ", "");
        if (line.length() != 9) {
          throw new IllegalStateException("Row " + row + " must have nine slots: " + structure[row]);
        }
        for (int column = 0; column < 9; column++) {
          char key = line.charAt(column);
          int slot = row * 9 + column;
          if (hasContentMarker && key == contentMarker) {
            contentSlots.add(slot);
          } else {
            items[slot] = ingredients.get(key);
          }
        }
      }
      return new Menu(structure.length, items, contentSlots, content);
    }
  }
}
