package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.gui.InvUIAdminPresetCreateGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class CreatePresetItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.CRAFTING_TABLE)
        .setLegacyName("§a§lCreate Preset")
        .addLegacyLoreLines(
            "§7Click to create a new",
            "§7global preset"
        );
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
    if (!player.hasPermission("chatcolor.admin")) {
      player.sendMessage(Component.text("You don't have permission to create presets!", NamedTextColor.RED));
      return;
    }

    player.closeInventory();
    InvUIAdminPresetCreateGUI.openForCreation(player);
  }
}
