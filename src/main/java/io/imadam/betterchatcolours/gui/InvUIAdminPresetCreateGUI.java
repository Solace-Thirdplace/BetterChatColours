package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.gui.items.preset.AddColorItem;
import io.imadam.betterchatcolours.gui.menu.ItemBuilder;
import io.imadam.betterchatcolours.gui.menu.Menu;
import io.imadam.betterchatcolours.gui.menu.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class InvUIAdminPresetCreateGUI {

    public static void openForCreation(Player player) {
        // Request preset name via chat first, then open the color selection GUI
        ChatInputManager.requestPresetName(player,
                presetName -> openColorSelectionGUI(player, presetName, new ArrayList<>(), false),
                () -> MainMenuGUI.open(player));
    }

    public static void openForEditing(Player player, String presetName, List<String> colors) {
        openColorSelectionGUI(player, presetName, new ArrayList<>(colors), true);
    }

    public static void reopenColorSelectionGUI(Player player, String presetName, List<String> colors, boolean isEditMode) {
        openColorSelectionGUI(player, presetName, colors, isEditMode);
    }

    private static void openColorSelectionGUI(Player player, String presetName, List<String> colors, boolean isEditMode) {
        // Create color items for the dynamic content
        List<MenuItem> colorItems = new ArrayList<>();
        for (int i = 0; i < colors.size(); i++) {
            colorItems.add(new ColorSlotItem(presetName, colors, i, isEditMode));
        }

        // Paged menu for dynamic content
        Menu gui = Menu.builder()
                .setStructure(
                        "# # # # # # # # #",
                        "# x x x x x x x #",
                        "# x x x x x x x #",
                        "# a # p # s # c #")
                .addIngredient('#', GUIUtils.createGlassPane())
                .setContentSlots('x')
                .addIngredient('a', new AddColorItem(presetName, colors, isEditMode))
                .addIngredient('p', new PreviewItem(presetName, colors))
                .addIngredient('s', new SavePresetItem(presetName, colors, isEditMode))
                .addIngredient('c', new CancelItem(isEditMode))
                .setContent(colorItems)
                .build();

        Component title = LegacyComponentSerializer.legacySection()
                .deserialize((isEditMode ? "Edit Preset: " : "Create Preset: ") + presetName);

        gui.open(player, title);
    }

    // Inner classes for GUI items
    private static class PreviewItem extends MenuItem {
        private final String presetName;
        private final List<String> colors;

        public PreviewItem(String presetName, List<String> colors) {
            this.presetName = presetName;
            this.colors = colors;
        }

        @Override
        public ItemStack getItem(Player player) {
            ItemBuilder builder = new ItemBuilder(Material.PAPER)
                    .setLegacyName("§e§lPreview");

            if (!colors.isEmpty()) {
                String preview = createGradientPreview("Preview: " + presetName, colors);
                builder.addLegacyLoreLines("§7Gradient preview:", preview);
            } else {
                builder.addLegacyLoreLines("§7Add colors to see preview");
            }

            return builder.build();
        }

        private String createGradientPreview(String text, List<String> colors) {
            if (colors == null || colors.isEmpty()) {
                return "§e" + text;
            }

            try {
                // Create gradient tag like in GlobalPresetData
                StringBuilder gradient = new StringBuilder("<gradient:");
                for (int i = 0; i < colors.size(); i++) {
                    if (i > 0) gradient.append(":");
                    gradient.append(colors.get(i));
                }
                gradient.append(">");

                String gradientMessage = gradient.toString() + text + "</gradient>";
                var component = MiniMessage.miniMessage().deserialize(gradientMessage);
                return LegacyComponentSerializer.legacySection().serialize(component);
            } catch (Exception e) {
                return "§e" + text;
            }
        }

        @Override
        public void handleClick(ClickType clickType, Player player) {
            // Do nothing - this is just a preview
        }
    }

    private static class SavePresetItem extends MenuItem {
        private final String presetName;
        private final List<String> colors;
        private final boolean isEditMode;

        public SavePresetItem(String presetName, List<String> colors, boolean isEditMode) {
            this.presetName = presetName;
            this.colors = colors;
            this.isEditMode = isEditMode;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.NAME_TAG)
                    .setLegacyName("§a§lSave Preset")
                    .addLegacyLoreLines(
                            "§7Click to save preset with",
                            "§7auto-generated permission:",
                            "§bchatcolor.preset." + presetName.toLowerCase()
                    )
                    .build();
        }

        @Override
        public void handleClick(ClickType clickType, Player player) {
            if (colors.isEmpty()) {
                player.sendMessage(Component.text("Cannot save preset without colors!", NamedTextColor.RED));
                return;
            }

            BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
            plugin.getGlobalPresetManager().addPreset(presetName, colors);

            String permission = "chatcolor.preset." + presetName.toLowerCase();
            String action = isEditMode ? "updated" : "created";
            player.sendMessage(Component.text("Preset '" + presetName + "' " + action + " successfully! Permission: " + permission, NamedTextColor.GREEN));

            player.closeInventory();
            if (isEditMode) {
                InvUIAdminPresetEditGUI.open(player);
            }
        }
    }

    private static class CancelItem extends MenuItem {
        private final boolean isEditMode;

        public CancelItem(boolean isEditMode) {
            this.isEditMode = isEditMode;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.BARRIER)
                    .setLegacyName("§c§lCancel")
                    .addLegacyLoreLines("§7Click to cancel and return")
                    .build();
        }

        @Override
        public void handleClick(ClickType clickType, Player player) {
            player.closeInventory();
            if (isEditMode) {
                InvUIAdminPresetEditGUI.open(player);
            } else {
                MainMenuGUI.open(player);
            }
        }
    }

    private static class ColorSlotItem extends MenuItem {
        private final String presetName;
        private final List<String> colors;
        private final int index;
        private final boolean isEditMode;

        public ColorSlotItem(String presetName, List<String> colors, int index, boolean isEditMode) {
            this.presetName = presetName;
            this.colors = colors;
            this.index = index;
            this.isEditMode = isEditMode;
        }

        @Override
        public ItemStack getItem(Player player) {
            String hexColor = colors.get(index);
            Material dyeColor = GUIUtils.getClosestDyeColor(hexColor);

            return new ItemBuilder(dyeColor)
                    .setLegacyName("§f§lColor " + (index + 1))
                    .addLegacyLoreLines(
                            "§7Hex: §f" + hexColor,
                            "",
                            "§eLeft click: §7Edit color",
                            "§eRight click: §7Remove color"
                    )
                    .build();
        }

        @Override
        public void handleClick(ClickType clickType, Player player) {
            if (clickType.isLeftClick()) {
                // Edit color - close GUI first
                player.closeInventory();
                ChatInputManager.requestHexColorEdit(player, presetName, colors, index,
                        hexColor -> {
                            colors.set(index, hexColor);
                            reopenColorSelectionGUI(player, presetName, colors, isEditMode);
                        },
                        () -> reopenColorSelectionGUI(player, presetName, colors, isEditMode));
            } else if (clickType.isRightClick()) {
                // Remove color
                colors.remove(index);
                reopenColorSelectionGUI(player, presetName, colors, isEditMode);
            }
        }
    }
}
