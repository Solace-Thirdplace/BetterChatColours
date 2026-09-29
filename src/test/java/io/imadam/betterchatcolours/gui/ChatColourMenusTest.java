package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.BetterChatColours;
import io.imadam.betterchatcolours.data.GlobalPresetData;
import io.imadam.betterchatcolours.data.GlobalPresetManager;
import io.imadam.betterchatcolours.data.UserDataManager;
import io.imadam.betterchatcolours.gui.menu.Menu;
import io.imadam.betterchatcolours.gui.menu.MenuListener;
import io.imadam.betterchatcolours.gui.menu.MenuTestSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives the real plugin menus through {@link MenuListener} the way a player would: open with the
 * command's entry point, click a slot, let the next tick run, and check what happened.
 */
class ChatColourMenusTest {

  private MenuTestSupport server;
  private MockedStatic<JavaPlugin> javaPlugin;
  private MenuListener listener;
  private GlobalPresetManager presets;
  private UserDataManager userData;
  private final Map<String, GlobalPresetData> presetMap = new LinkedHashMap<>();

  @BeforeEach
  void setUp() {
    server = new MenuTestSupport();
    listener = server.listener();
    BetterChatColours plugin = mock(BetterChatColours.class);
    presets = mock(GlobalPresetManager.class);
    userData = mock(UserDataManager.class);
    when(plugin.getGlobalPresetManager()).thenReturn(presets);
    when(plugin.getUserDataManager()).thenReturn(userData);
    when(presets.getAllPresets()).thenReturn(presetMap);
    javaPlugin = mockStatic(JavaPlugin.class);
    javaPlugin.when(() -> JavaPlugin.getPlugin(BetterChatColours.class)).thenReturn(plugin);
  }

  @AfterEach
  void tearDown() {
    javaPlugin.close();
    server.close();
  }

  private void preset(String name, String permission, String... colors) {
    presetMap.put(name, new GlobalPresetData(name, new ArrayList<>(List.of(colors)), permission));
  }

  private void click(Player player, int slot, ClickType clickType) {
    listener.onInventoryClick(server.click(server.lastMenu(), player, slot, clickType, InventoryAction.PICKUP_ALL));
    server.runTasks();
  }

  private static String plain(Component component) {
    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  @Test
  void adminMainMenuHasSelectCreateAndEdit() {
    Player admin = server.player("chatcolor.admin");
    MainMenuGUI.open(admin);

    Menu menu = server.lastMenu();
    assertEquals(27, menu.getSize());
    assertEquals(Material.ENDER_CHEST, server.material(menu, 11));
    assertEquals(Material.CRAFTING_TABLE, server.material(menu, 13));
    assertEquals(Material.ANVIL, server.material(menu, 15));
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, server.material(menu, 0));
    assertEquals("Select Presets", plain(server.name(menu, 11)));
    server.bukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(27),
        eq(LegacyComponentSerializer.legacySection().deserialize("§8Chat Colors - Admin Menu"))));
  }

  @Test
  void playerMainMenuHasOnlySelect() {
    Player player = server.player();
    MainMenuGUI.open(player);

    Menu menu = server.lastMenu();
    assertEquals(Material.ENDER_CHEST, server.material(menu, 13));
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, server.material(menu, 11));
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, server.material(menu, 15));

    click(player, 13, ClickType.LEFT);
    assertEquals(54, server.lastMenu().getSize(), "Select Presets opens the preset list");
  }

  @Test
  void presetListIsFilteredSortedAndClickingEquips() {
    preset("ice", "", "#00FFFF", "#FFFFFF");
    preset("zzz", "", "#FF0000", "#00FF00", "#0000FF");
    preset("fire", "chatcolor.preset.fire", "#FF0000", "#FFA500", "#FFFF00");
    preset("locked", "chatcolor.preset.locked", "#111111", "#222222", "#333333", "#444444");
    Player player = server.player("chatcolor.preset.fire");

    PresetSelectionGUI.open(player);
    Menu menu = server.lastMenu();

    verify(userData).checkAndUnequipInvalidPreset(player);
    assertEquals("fire", plain(server.name(menu, 10)));
    assertEquals("zzz", plain(server.name(menu, 11)));
    assertEquals("ice", plain(server.name(menu, 12)));
    assertNull(server.material(menu, 13));
    assertEquals(Material.RED_CONCRETE, server.material(menu, 10));
    assertEquals(List.of("Colors: 3", "", "Click to equip this preset"),
        server.lore(menu, 10).stream().map(ChatColourMenusTest::plain).toList());
    server.bukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54),
        eq(LegacyComponentSerializer.legacySection().deserialize("§8Available Presets §7(Page 1/1)"))));

    click(player, 11, ClickType.LEFT);
    verify(userData).setEquippedPreset(player.getUniqueId(), "zzz");
    verify(player).closeInventory();
  }

  @Test
  void unequipButtonClearsThePreset() {
    Player player = server.player();
    PresetSelectionGUI.open(player);

    assertEquals(Material.BARRIER, server.material(server.lastMenu(), 46));
    click(player, 46, ClickType.LEFT);

    verify(userData).setEquippedPreset(eq(player.getUniqueId()), isNull());
    verify(player).closeInventory();
  }

  @Test
  void pageButtonsMoveThroughThePresetList() {
    for (int i = 0; i < 30; i++) {
      preset(String.format("p%02d", i), "", "#FF0000");
    }
    Player player = server.player();
    PresetSelectionGUI.open(player);
    Menu menu = server.lastMenu();
    server.bukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54),
        eq(LegacyComponentSerializer.legacySection().deserialize("§8Available Presets §7(Page 1/2)"))));
    assertEquals("Go to page 2/2", plain(server.lore(menu, 50).get(0)));
    assertEquals("You can't go further back", plain(server.lore(menu, 48).get(0)));

    click(player, 50, ClickType.LEFT);
    assertSame(menu, server.lastMenu(), "page change redraws the open menu");
    assertEquals(1, menu.getPage());
    assertEquals("p28", plain(server.name(menu, 10)));
    assertEquals("p29", plain(server.name(menu, 11)));
    assertNull(server.material(menu, 12));
    assertEquals("Go to page 1/2", plain(server.lore(menu, 48).get(0)));
    assertEquals("There are no more pages", plain(server.lore(menu, 50).get(0)));

    click(player, 50, ClickType.LEFT);
    assertEquals(1, menu.getPage());
    click(player, 48, ClickType.LEFT);
    assertEquals(0, menu.getPage());
    assertEquals("p00", plain(server.name(menu, 10)));
  }

  @Test
  void bottomRightButtonClosesForPlayersAndGoesBackForAdmins() {
    Player player = server.player();
    PresetSelectionGUI.open(player);
    assertEquals(Material.BARRIER, server.material(server.lastMenu(), 52));
    click(player, 52, ClickType.LEFT);
    verify(player).closeInventory();

    Player admin = server.player("chatcolor.admin");
    PresetSelectionGUI.open(admin);
    Menu list = server.lastMenu();
    assertEquals(Material.ARROW, server.material(list, 52));
    click(admin, 52, ClickType.LEFT);
    verify(admin).closeInventory();
    assertNotSame(list, server.lastMenu());
    assertEquals(27, server.lastMenu().getSize(), "back opens the main menu");
  }

  @Test
  void editListRightClickDeletesAndLeftClickOpensTheEditor() {
    preset("fire", "chatcolor.preset.fire", "#FF0000", "#FFA500");
    preset("ice", "", "#00FFFF");
    Player admin = server.player("chatcolor.admin");

    InvUIAdminPresetEditGUI.open(admin);
    Menu menu = server.lastMenu();
    assertEquals("fire", plain(server.name(menu, 10)));
    assertEquals(Material.ARROW, server.material(menu, 49));
    assertEquals(Material.RED_STAINED_GLASS_PANE, server.material(menu, 46));
    assertEquals(Material.GREEN_STAINED_GLASS_PANE, server.material(menu, 52));

    click(admin, 11, ClickType.RIGHT);
    verify(presets).removePreset("ice");
    verify(presets, never()).removePreset("fire");

    InvUIAdminPresetEditGUI.open(admin);
    click(admin, 10, ClickType.LEFT);
    Menu editor = server.lastMenu();
    assertEquals(36, editor.getSize());
    assertEquals(Material.RED_DYE, server.material(editor, 10));
    assertEquals(Material.ORANGE_DYE, server.material(editor, 11));
    server.bukkit.verify(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(36),
        eq(LegacyComponentSerializer.legacySection().deserialize("Edit Preset: fire"))));
  }

  @Test
  void presetEditorRemovesColoursAndSaves() {
    Player admin = server.player("chatcolor.admin");
    List<String> colors = List.of("#FF0000", "#0000FF", "#00FF00");
    InvUIAdminPresetCreateGUI.openForEditing(admin, "sea", colors);
    Menu editor = server.lastMenu();
    assertEquals(Material.LIME_DYE, server.material(editor, 28));
    assertEquals(Material.PAPER, server.material(editor, 30));
    assertEquals(Material.NAME_TAG, server.material(editor, 32));
    assertEquals(Material.BARRIER, server.material(editor, 34));

    click(admin, 11, ClickType.RIGHT);
    Menu reopened = server.lastMenu();
    assertNotSame(editor, reopened);
    assertEquals("Color 2", plain(server.name(reopened, 11)));
    assertEquals("Hex: #00FF00", plain(server.lore(reopened, 11).get(0)));
    assertNull(server.material(reopened, 12));

    click(admin, 32, ClickType.LEFT);
    verify(presets).addPreset("sea", List.of("#FF0000", "#00FF00"));
    verify(admin).closeInventory();
    assertEquals(54, server.lastMenu().getSize(), "saving an edited preset returns to the edit list");
  }

  @Test
  void addColourClosesTheMenuAndAsksInChat() {
    Player admin = server.player("chatcolor.admin");
    InvUIAdminPresetCreateGUI.openForEditing(admin, "sea", List.of("#FF0000"));
    int opened = server.opened.size();

    click(admin, 28, ClickType.LEFT);

    verify(admin).closeInventory();
    verify(admin, org.mockito.Mockito.atLeastOnce()).sendMessage(any(Component.class));
    assertEquals(opened, server.opened.size());
    ChatInputManager.cancelSession(admin);
  }

  @Test
  void glassPanesDoNothing() {
    Player player = server.player();
    MainMenuGUI.open(player);
    int opened = server.opened.size();

    click(player, 0, ClickType.LEFT);

    assertEquals(opened, server.opened.size());
    verify(player, never()).closeInventory();
  }
}
