package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.data.GlobalPresetData;
import io.imadam.betterchatcolours.gui.items.BackToMainItem;
import io.imadam.betterchatcolours.gui.items.CloseMenuItem;
import io.imadam.betterchatcolours.gui.items.PresetItem;
import io.imadam.betterchatcolours.gui.items.UnequipPresetItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.stream.Collectors;

public class PresetSelectionGUI {

  public static void open(Player player) {
    BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
    boolean isAdmin = player.hasPermission("chatcolor.admin");

    // Check if player's current preset is still valid
    plugin.getUserDataManager().checkAndUnequipInvalidPreset(player);

    // Get available presets for this player
    List<GlobalPresetData> availablePresets = plugin.getGlobalPresetManager()
        .getAllPresets()
        .values()
        .stream()
        .filter(preset -> preset.getPermission() == null ||
            preset.getPermission().isEmpty() ||
            player.hasPermission(preset.getPermission()))
        .sorted((a, b) -> {
          // First sort by color count (descending - higher numbers first)
          int colorCountCompare = Integer.compare(b.getColors().size(), a.getColors().size());
          if (colorCountCompare != 0) {
            return colorCountCompare;
          }
          // Then sort alphabetically (ascending)
          return a.getName().compareToIgnoreCase(b.getName());
        })
        .collect(Collectors.toList());

    // Convert to PresetItems (as Items)
    List<Item> presetItems = availablePresets.stream()
        .map(PresetItem::new)
        .collect(Collectors.toList());

    PagedGui<Item> gui = PagedGui.itemsBuilder()
        .setStructure(
            "# # # # # # # # #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# x x x x x x x #",
            "# u # < # > # b #")
        .addIngredient('#', GUIUtils.createGlassPane())
        .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
        .addIngredient('u', new UnequipPresetItem()) // Unequip button
        .addIngredient('<', new PreviousPageItem()) // Previous page
        .addIngredient('>', new NextPageItem()) // Next page
        .addIngredient('b', isAdmin ? new BackToMainItem() : new CloseMenuItem())
        .setContent(presetItems)
        .build();

    // Create title with current page info
    Component title = LegacyComponentSerializer.legacySection()
        .deserialize("§8Available Presets §7(Page " + (gui.getPage() + 1) + "/" + Math.max(1, gui.getPageCount()) + ")");

    Window window = Window.builder()
        .setViewer(player)
        .setTitle(title)
        .setUpperGui(gui)
        .build();

    window.open();
  }

  // Pagination control items bound to the paged gui they are placed in
  private static class PreviousPageItem extends AbstractPagedGuiBoundItem {

    @Override
    public ItemProvider getItemProvider(Player player) {
      PagedGui<?> gui = getGui();
      return new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
          .setLegacyName("§e§lPrevious Page")
          .addLegacyLoreLines(
              gui.getPage() > 0
                  ? "§7Go to page " + gui.getPage() + "/" + gui.getPageCount()
                  : "§7You can't go further back"
          );
    }

    @Override
    public void handleClick(ClickType clickType, Player player, Click click) {
      PagedGui<?> gui = getGui();
      if (gui.getPage() > 0) {
        gui.setPage(gui.getPage() - 1);
      }
    }
  }

  private static class NextPageItem extends AbstractPagedGuiBoundItem {

    @Override
    public ItemProvider getItemProvider(Player player) {
      PagedGui<?> gui = getGui();
      return new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
          .setLegacyName("§e§lNext Page")
          .addLegacyLoreLines(
              gui.getPage() < gui.getPageCount() - 1
                  ? "§7Go to page " + (gui.getPage() + 2) + "/" + gui.getPageCount()
                  : "§7There are no more pages"
          );
    }

    @Override
    public void handleClick(ClickType clickType, Player player, Click click) {
      PagedGui<?> gui = getGui();
      if (gui.getPage() < gui.getPageCount() - 1) {
        gui.setPage(gui.getPage() + 1);
      }
    }
  }
}
