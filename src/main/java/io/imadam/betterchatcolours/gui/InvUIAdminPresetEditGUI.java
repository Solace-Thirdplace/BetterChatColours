package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.data.GlobalPresetData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.gui.Markers;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.AbstractPagedGuiBoundItem;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.Window;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvUIAdminPresetEditGUI {

    public static void open(Player player) {
        BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
        Map<String, GlobalPresetData> allPresets = plugin.getGlobalPresetManager().getAllPresets();

        // Create preset items and sort by color count (descending) then alphabetically
        List<Item> presetItems = allPresets.values().stream()
                .sorted((a, b) -> {
                  // First sort by color count (descending - higher numbers first)
                  int colorCountCompare = Integer.compare(b.getColors().size(), a.getColors().size());
                  if (colorCountCompare != 0) {
                    return colorCountCompare;
                  }
                  // Then sort alphabetically (ascending)
                  return a.getName().compareToIgnoreCase(b.getName());
                })
                .map(EditablePresetItem::new)
                .collect(Collectors.toList());

        PagedGui<Item> gui = PagedGui.itemsBuilder()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# < # # b # # > #")
                .addIngredient('#', GUIUtils.createGlassPane())
                .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .addIngredient('<', new PreviousPageItem())
                .addIngredient('>', new NextPageItem())
                .addIngredient('b', new BackToMainItem())
                .setContent(presetItems)
                .build();

        // Create title with current page info
        Component title = LegacyComponentSerializer.legacySection()
                .deserialize("§8Edit Presets §7(Page " + (gui.getPage() + 1) + "/" + Math.max(1, gui.getPageCount()) + ")");

        Window window = Window.builder()
                .setViewer(player)
                .setTitle(title)
                .setUpperGui(gui)
                .build();

        window.open();
    }

    private static class EditablePresetItem extends AbstractItem {
        private final GlobalPresetData preset;

        public EditablePresetItem(GlobalPresetData preset) {
            this.preset = preset;
        }

        @Override
        public ItemProvider getItemProvider(Player player) {
            // Create gradient preview for display name using the same method as PresetItem
            String gradientName = applyGradientToText(preset.getName());

            // Get the material based on the first color in the gradient
            Material iconMaterial = getIconMaterial();

            return new ItemBuilder(iconMaterial)
                    .setLegacyName(gradientName)
                    .addLegacyLoreLines(
                            "§7Colors: §f" + preset.getColors().size(),
                            "§7Permission: §f" + preset.getPermission(),
                            "",
                            "§eLeft click: §7Edit preset",
                            "§eRight click: §7Delete preset"
                    );
        }

        private Material getIconMaterial() {
            if (preset.getColors() == null || preset.getColors().isEmpty()) {
                return Material.PAPER; // Fallback for presets with no colors
            }

            // Use the first color in the gradient to determine the icon
            String firstColor = preset.getColors().get(0);
            return GUIUtils.getClosestConcreteColor(firstColor);
        }

        private String applyGradientToText(String text) {
            if (preset.getColors() == null || preset.getColors().isEmpty()) {
                return "§e§l" + text;
            }

            try {
                String gradientMessage = preset.getGradientTag() + text + preset.getClosingTag();
                var component = MiniMessage.miniMessage().deserialize(gradientMessage);
                return LegacyComponentSerializer.legacySection().serialize(component);
            } catch (Exception e) {
                // Fallback to yellow text if gradient parsing fails
                return "§e§l" + text;
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player, Click click) {
            if (clickType.isLeftClick()) {
                // Edit the preset
                player.closeInventory();
                InvUIAdminPresetCreateGUI.openForEditing(player, preset.getName(), preset.getColors());
            } else if (clickType.isRightClick()) {
                // Delete the preset
                deletePreset(player, preset.getName());
            }
        }

        private void deletePreset(Player player, String presetName) {
            BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
            plugin.getGlobalPresetManager().removePreset(presetName);

            player.sendMessage(Component.text("Preset '" + presetName + "' deleted successfully!", NamedTextColor.GREEN));

            // Refresh the GUI
            player.closeInventory();
            InvUIAdminPresetEditGUI.open(player);
        }
    }

    private static class BackToMainItem extends AbstractItem {
        @Override
        public ItemProvider getItemProvider(Player player) {
            return new ItemBuilder(Material.ARROW)
                    .setLegacyName("§c§lBack to Main Menu")
                    .addLegacyLoreLines("§7Click to return to main menu");
        }

        @Override
        public void handleClick(ClickType clickType, Player player, Click click) {
            player.closeInventory();
            MainMenuGUI.open(player);
        }
    }

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
