package io.imadam.betterchatcolours.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuListenerTest {

  private MenuTestSupport server;
  private MenuListener listener;
  private Player player;

  /** Records every click it receives. */
  private static class RecordingItem extends MenuItem {
    final Material material;
    final List<ClickType> clicks = new ArrayList<>();

    RecordingItem(Material material) {
      this.material = material;
    }

    @Override
    public ItemStack getItem(Player player) {
      return new ItemBuilder(material).setLegacyName(material.name()).build();
    }

    @Override
    public void handleClick(ClickType clickType, Player player) {
      clicks.add(clickType);
    }
  }

  @BeforeEach
  void setUp() {
    server = new MenuTestSupport();
    listener = server.listener();
    player = server.player();
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  private Menu openMenu(RecordingItem button, List<? extends MenuItem> content) {
    Menu menu = Menu.builder()
        .setStructure(
            "# # # # # # # # #",
            "# x x x x x x x #",
            "# < # # b # # > #")
        .addIngredient('#', MenuItem.simple(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setLegacyName(" ")))
        .addIngredient('b', button)
        .setContentSlots('x')
        .setContent(content)
        .build();
    menu.open(player, Component.text("Test"));
    return menu;
  }

  @Test
  void clickOnButtonIsCancelledAndRunsItsActionOnTheNextTick() {
    RecordingItem button = new RecordingItem(Material.ARROW);
    Menu menu = openMenu(button, List.of());

    InventoryClickEvent event = server.click(menu, player, 22, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    listener.onInventoryClick(event);

    verify(event).setCancelled(true);
    assertTrue(button.clicks.isEmpty(), "action must not run inside the click event");
    server.runTasks();
    assertEquals(List.of(ClickType.LEFT), button.clicks);
  }

  @Test
  void clickTypeIsPassedThrough() {
    RecordingItem button = new RecordingItem(Material.ARROW);
    Menu menu = openMenu(button, List.of());

    for (ClickType type : List.of(ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.DROP)) {
      listener.onInventoryClick(server.click(menu, player, 22, type, InventoryAction.NOTHING));
      server.runTasks();
    }
    assertEquals(List.of(ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.DROP), button.clicks);
  }

  @Test
  void secondClickBeforeTheActionRunsIsIgnored() {
    RecordingItem button = new RecordingItem(Material.ARROW);
    Menu menu = openMenu(button, List.of());

    InventoryClickEvent first = server.click(menu, player, 22, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    InventoryClickEvent second = server.click(menu, player, 22, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    listener.onInventoryClick(first);
    listener.onInventoryClick(second);
    verify(second).setCancelled(true);
    server.runTasks();
    assertEquals(1, button.clicks.size());

    listener.onInventoryClick(server.click(menu, player, 22, ClickType.LEFT, InventoryAction.PICKUP_ALL));
    server.runTasks();
    assertEquals(2, button.clicks.size());
  }

  @Test
  void clickOnEmptyContentSlotIsCancelledAndDoesNothing() {
    RecordingItem button = new RecordingItem(Material.ARROW);
    Menu menu = openMenu(button, List.of());

    InventoryClickEvent event = server.click(menu, player, 10, ClickType.LEFT, InventoryAction.NOTHING);
    listener.onInventoryClick(event);

    verify(event).setCancelled(true);
    assertTrue(server.tasks.isEmpty());
  }

  @Test
  void plainClickInThePlayersOwnInventoryIsCancelled() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());

    InventoryClickEvent event = server.click(menu, player, 40, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    listener.onInventoryClick(event);

    verify(event).setCancelled(true);
    assertTrue(server.tasks.isEmpty());
  }

  @Test
  void shiftClickAndCollectFromThePlayersInventoryAreCancelled() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());

    InventoryClickEvent shift = server.click(menu, player, 40, ClickType.SHIFT_LEFT,
        InventoryAction.MOVE_TO_OTHER_INVENTORY);
    InventoryClickEvent collect = server.click(menu, player, 40, ClickType.DOUBLE_CLICK,
        InventoryAction.COLLECT_TO_CURSOR);
    listener.onInventoryClick(shift);
    listener.onInventoryClick(collect);

    verify(shift).setCancelled(true);
    verify(collect).setCancelled(true);
    assertTrue(server.tasks.isEmpty());
  }

  @Test
  void cancellationIsEnforcedAgainAfterOtherPlugins() {
    RecordingItem button = new RecordingItem(Material.ARROW);
    Menu menu = openMenu(button, List.of());

    InventoryClickEvent menuSlot = server.click(menu, player, 22, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    InventoryClickEvent shift = server.click(menu, player, 40, ClickType.SHIFT_LEFT,
        InventoryAction.MOVE_TO_OTHER_INVENTORY);
    InventoryClickEvent ownSlot = server.click(menu, player, 40, ClickType.LEFT, InventoryAction.PICKUP_ALL);
    listener.enforceInventoryClick(menuSlot);
    listener.enforceInventoryClick(shift);
    listener.enforceInventoryClick(ownSlot);

    verify(menuSlot).setCancelled(true);
    verify(shift).setCancelled(true);
    verify(ownSlot).setCancelled(true);
    assertTrue(server.tasks.isEmpty(), "the enforcing handler never runs item actions");
  }

  @Test
  void clickOutsideTheWindowIsCancelled() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());

    InventoryClickEvent event = server.click(menu, player, -999, ClickType.LEFT, InventoryAction.NOTHING);
    listener.onInventoryClick(event);

    verify(event).setCancelled(true);
    assertTrue(server.tasks.isEmpty());
  }

  @Test
  void everyDragIsCancelledWhileAMenuIsOpen() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());

    var intoMenu = server.drag(menu.getInventory(), Set.of(26, 30));
    var ownInventory = server.drag(menu.getInventory(), Set.of(30, 31));
    listener.onInventoryDrag(intoMenu);
    listener.onInventoryDrag(ownInventory);

    verify(intoMenu).setCancelled(true);
    verify(ownInventory).setCancelled(true);
  }

  @Test
  void everyKindOfClickIsCancelledInBothInventories() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());

    for (int slot : new int[] {0, 22, 26, 27, 40, 62, -1, -999}) {
      for (ClickType type : ClickType.values()) {
        for (InventoryAction action : InventoryAction.values()) {
          InventoryClickEvent first = server.click(menu, player, slot, type, action);
          InventoryClickEvent last = server.click(menu, player, slot, type, action);
          listener.onInventoryClick(first);
          listener.enforceInventoryClick(last);
          verify(first).setCancelled(true);
          verify(last).setCancelled(true);
          server.runTasks();
        }
      }
    }
  }

  @Test
  void closeAllClosesOpenMenusAndLeavesOtherInventoriesAlone() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());
    var menuView = mock(org.bukkit.inventory.InventoryView.class);
    when(menuView.getTopInventory()).thenReturn(menu.getInventory());
    when(player.getOpenInventory()).thenReturn(menuView);

    Player other = mock(Player.class);
    Inventory chest = mock(Inventory.class);
    when(chest.getHolder(false)).thenReturn(mock(InventoryHolder.class));
    var chestView = mock(org.bukkit.inventory.InventoryView.class);
    when(chestView.getTopInventory()).thenReturn(chest);
    when(other.getOpenInventory()).thenReturn(chestView);

    server.bukkit.when(org.bukkit.Bukkit::getOnlinePlayers).thenAnswer(inv -> List.of(player, other));
    MenuListener.closeAll();

    verify(player).closeInventory();
    verify(other, never()).closeInventory();
  }

  @Test
  void otherInventoriesAreIgnored() {
    Inventory chest = mock(Inventory.class);
    when(chest.getHolder(false)).thenReturn(mock(InventoryHolder.class));
    when(chest.getSize()).thenReturn(27);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    var view = mock(org.bukkit.inventory.InventoryView.class);
    when(view.getTopInventory()).thenReturn(chest);
    when(event.getView()).thenReturn(view);
    when(event.getRawSlot()).thenReturn(3);

    listener.onInventoryClick(event);
    listener.onInventoryDrag(server.drag(chest, Set.of(3)));

    verify(event, never()).setCancelled(true);
  }

  @Test
  void contentIsLaidOutInPagesAndClicksReachTheItemOnTheCurrentPage() {
    List<RecordingItem> content = IntStream.range(0, 9)
        .mapToObj(i -> new RecordingItem(Material.values()[i + 1]))
        .toList();
    Menu menu = openMenu(new RecordingItem(Material.ARROW), content);

    assertEquals(2, menu.getPageCount());
    assertEquals(0, menu.getPage());
    assertEquals(content.get(0).material, server.material(menu, 10));
    assertEquals(content.get(6).material, server.material(menu, 16));
    assertEquals(Material.BLACK_STAINED_GLASS_PANE, server.material(menu, 0));
    assertEquals(Material.ARROW, server.material(menu, 22));

    menu.setPage(1);
    assertEquals(content.get(7).material, server.material(menu, 10));
    assertEquals(content.get(8).material, server.material(menu, 11));
    assertNull(server.material(menu, 12));

    listener.onInventoryClick(server.click(menu, player, 11, ClickType.RIGHT, InventoryAction.PICKUP_HALF));
    server.runTasks();
    assertEquals(List.of(ClickType.RIGHT), content.get(8).clicks);
    assertTrue(content.get(1).clicks.isEmpty());

    menu.setPage(7);
    assertEquals(1, menu.getPage(), "page is clamped to the last page");
    menu.setPage(-3);
    assertEquals(0, menu.getPage());
  }

  @Test
  void emptyContentHasNoPages() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());
    assertEquals(0, menu.getPageCount());
    menu.setPage(1);
    assertEquals(0, menu.getPage());
  }

  @Test
  void menuIsItsInventorysHolder() {
    Menu menu = openMenu(new RecordingItem(Material.ARROW), List.of());
    assertSame(menu, menu.getInventory().getHolder(false));
    assertEquals(27, menu.getInventory().getSize());
    verify(player).openInventory(menu.getInventory());
  }
}
