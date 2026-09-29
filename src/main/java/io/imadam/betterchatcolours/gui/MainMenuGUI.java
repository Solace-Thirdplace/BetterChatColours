package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.gui.items.CreatePresetItem;
import io.imadam.betterchatcolours.gui.items.EditPresetItem;
import io.imadam.betterchatcolours.gui.items.SelectPresetsItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.window.Window;

public class MainMenuGUI {

  public static void open(Player player) {
    boolean isAdmin = player.hasPermission("chatcolor.admin");

    Gui gui;
    if (isAdmin) {
      gui = Gui.builder()
          .setStructure(
              "# # # # # # # # #",
              "# # s # c # e # #",
              "# # # # # # # # #")
          .addIngredient('#', GUIUtils.createGlassPane())
          .addIngredient('s', new SelectPresetsItem())
          .addIngredient('c', new CreatePresetItem())
          .addIngredient('e', new EditPresetItem())
          .build();
    } else {
      gui = Gui.builder()
          .setStructure(
              "# # # # # # # # #",
              "# # # # s # # # #",
              "# # # # # # # # #")
          .addIngredient('#', GUIUtils.createGlassPane())
          .addIngredient('s', new SelectPresetsItem())
          .build();
    }

    Component title = LegacyComponentSerializer.legacySection()
        .deserialize(isAdmin ? "§8Chat Colors - Admin Menu" : "§8Chat Colors");

    Window window = Window.builder()
        .setViewer(player)
        .setTitle(title)
        .setUpperGui(gui)
        .build();

    window.open();
  }
}
