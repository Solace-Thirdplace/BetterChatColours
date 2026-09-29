package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.InvUIAdminPresetCreateGUI;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class CreatePresetItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.CRAFTING_TABLE)
        .setLegacyName("§a§lCreate Preset")
        .addLegacyLoreLines(
            "§7Click to create a new",
            "§7global preset"
        )
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    if (!player.hasPermission("chatcolor.admin")) {
      player.sendMessage(Component.text("You don't have permission to create presets!", NamedTextColor.RED));
      return;
    }

    player.closeInventory();
    InvUIAdminPresetCreateGUI.openForCreation(player);
  }
}
