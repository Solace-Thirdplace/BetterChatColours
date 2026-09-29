package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.PresetSelectionGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class SelectPresetsItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.ENDER_CHEST)
        .setLegacyName("§d§lSelect Presets")
        .addLegacyLoreLines(
            "§7Click to browse available",
            "§7color presets");
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
    PresetSelectionGUI.open(player);
  }
}
