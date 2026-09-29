package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.gui.MainMenuGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class BackToMainItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.ARROW)
        .setLegacyName("§e← Back to Main Menu")
        .addLegacyLoreLines("§7Return to the main menu");
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
    player.closeInventory();
    // Small delay to ensure inventory closes before opening new one
    Bukkit.getScheduler().runTaskLater(
        JavaPlugin.getPlugin(BetterChatColours.class),
        () -> MainMenuGUI.open(player),
        1L
    );
  }
}
