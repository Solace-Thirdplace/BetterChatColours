package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CloseMenuItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.BARRIER)
        .setLegacyName("§c✕ Close Menu")
        .addLegacyLoreLines("§7Close this menu")
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    player.closeInventory();
  }
}
