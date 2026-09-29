package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class UnequipPresetItem extends MenuItem {

  @Override
  public ItemStack getItem(Player player) {
    return new ItemBuilder(Material.BARRIER)
        .setLegacyName("§c§lUnequip Current Preset")
        .addLegacyLoreLines(
            "§7Click to remove your current",
            "§7chatcolor preset and return",
            "§7to default chat appearance.",
            "",
            "§e§lClick to unequip!"
        )
        .build();
  }

  @Override
  public void handleClick(ClickType clickType, Player player) {
    BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);

    // Remove the equipped preset
    plugin.getUserDataManager().setEquippedPreset(player.getUniqueId(), null);

    // Send confirmation message
    player.sendMessage("§a§lSuccess! §7Your chatcolor preset has been unequipped.");
    player.sendMessage("§7Your messages will now appear in default colors.");

    // Close the GUI
    player.closeInventory();
  }
}
