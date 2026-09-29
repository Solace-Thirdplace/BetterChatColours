package io.imadam.betterchatcolours.gui.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class CloseMenuItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.BARRIER)
        .setLegacyName("§c✕ Close Menu")
        .addLegacyLoreLines("§7Close this menu");
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
    player.closeInventory();
  }
}
