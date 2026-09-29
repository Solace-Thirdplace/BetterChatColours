package io.imadam.betterchatcolours.gui.items;

import io.imadam.betterchatcolours.BetterChatColours;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class UnequipPresetItem extends AbstractItem {

  @Override
  public ItemProvider getItemProvider(Player player) {
    return new ItemBuilder(Material.BARRIER)
        .setLegacyName("§c§lUnequip Current Preset")
        .addLegacyLoreLines(
            "§7Click to remove your current",
            "§7chatcolor preset and return",
            "§7to default chat appearance.",
            "",
            "§e§lClick to unequip!"
        );
  }

  @Override
  public void handleClick(ClickType clickType, Player player, Click click) {
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
