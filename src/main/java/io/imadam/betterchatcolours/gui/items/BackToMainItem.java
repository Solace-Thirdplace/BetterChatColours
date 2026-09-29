package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.gui.MainMenuGUI;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class BackToMainItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.ARROW)
        .setLegacyName("§e← Back to Main Menu")
        .addLegacyLoreLines("§7Return to the main menu")
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    player.closeInventory();
    // Small delay to ensure inventory closes before opening new one
    Bukkit.getScheduler().runTaskLater(
        JavaPlugin.getPlugin(BetterChatColours.class),
        () -> MainMenuGUI.open(player),
        1L
    );
  }
}
