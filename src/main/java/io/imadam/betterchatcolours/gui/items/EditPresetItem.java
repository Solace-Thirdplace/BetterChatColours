package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.InvUIAdminPresetEditGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class EditPresetItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.ANVIL)
        .setLegacyName("§e§lEdit Presets")
        .addLegacyLoreLines(
            "§7Click to edit existing",
            "§7presets"
        );
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
    if (!player.hasPermission("chatcolor.admin")) {
      player.sendMessage(Component.text("You don't have permission to edit presets!", NamedTextColor.RED));
      return;
    }

    player.closeInventory();
    InvUIAdminPresetEditGUI.open(player);
  }
}
