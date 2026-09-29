package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.InvUIAdminPresetEditGUI;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class EditPresetItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.ANVIL)
        .setLegacyName("§e§lEdit Presets")
        .addLegacyLoreLines(
            "§7Click to edit existing",
            "§7presets"
        )
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    if (!player.hasPermission("chatcolor.admin")) {
      player.sendMessage(Component.text("You don't have permission to edit presets!", NamedTextColor.RED));
      return;
    }

    player.closeInventory();
    InvUIAdminPresetEditGUI.open(player);
  }
}
