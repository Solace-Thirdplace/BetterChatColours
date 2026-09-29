package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.PresetSelectionGUI;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class SelectPresetsItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.ENDER_CHEST)
        .setLegacyName("§d§lSelect Presets")
        .addLegacyLoreLines(
            "§7Click to browse available",
            "§7color presets")
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    PresetSelectionGUI.open(player);
  }
}
