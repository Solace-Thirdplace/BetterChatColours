package io.imadam.betterchatcolours.gui.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Stands in for the server in menu tests: Bukkit.createInventory returns a map-backed inventory,
 * ItemStack.of returns a mock that remembers its material, name and lore, and scheduled tasks are
 * queued until {@link #runTasks()}.
 */
public class MenuTestSupport implements AutoCloseable {

  public final MockedStatic<Bukkit> bukkit;
  public final MockedStatic<ItemStack> itemStacks;
  public final List<Runnable> tasks = new ArrayList<>();
  public final Map<ItemStack, Material> materials = new IdentityHashMap<>();
  public final Map<ItemStack, Component> names = new IdentityHashMap<>();
  public final Map<ItemStack, List<? extends Component>> lore = new IdentityHashMap<>();
  public final List<Inventory> opened = new ArrayList<>();

  public MenuTestSupport() {
    bukkit = mockStatic(Bukkit.class);
    bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), any(Component.class)))
        .thenAnswer(inv -> inventory(inv.getArgument(0), inv.getArgument(1)));
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(inv -> {
      tasks.add(inv.getArgument(1));
      return null;
    });
    when(scheduler.runTaskLater(any(), any(Runnable.class), org.mockito.ArgumentMatchers.anyLong()))
        .thenAnswer(inv -> {
          tasks.add(inv.getArgument(1));
          return null;
        });
    bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

    itemStacks = mockStatic(ItemStack.class);
    itemStacks.when(() -> ItemStack.of(any(Material.class))).thenAnswer(inv -> itemStack(inv.getArgument(0)));
  }

  /** The listener under test, running "next tick" tasks through the same queue. */
  public MenuListener listener() {
    return new MenuListener(tasks::add);
  }

  public void runTasks() {
    while (!tasks.isEmpty()) {
      tasks.remove(0).run();
    }
  }

  public Player player(String... permissions) {
    Player player = mock(Player.class);
    UUID id = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(id);
    Set<String> granted = Set.of(permissions);
    when(player.hasPermission(any(String.class))).thenAnswer(inv -> granted.contains(inv.<String>getArgument(0)));
    when(player.openInventory(any(Inventory.class))).thenAnswer(inv -> {
      opened.add(inv.getArgument(0));
      return null;
    });
    return player;
  }

  /** The menu most recently opened for any player. */
  public Menu lastMenu() {
    return (Menu) opened.get(opened.size() - 1).getHolder(false);
  }

  public InventoryClickEvent click(Menu menu, Player player, int rawSlot, ClickType clickType,
      InventoryAction action) {
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(menu.getInventory());
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getView()).thenReturn(view);
    when(event.getRawSlot()).thenReturn(rawSlot);
    when(event.getClick()).thenReturn(clickType);
    when(event.getAction()).thenReturn(action);
    when(event.getWhoClicked()).thenReturn(player);
    return event;
  }

  public InventoryDragEvent drag(Inventory top, Set<Integer> rawSlots) {
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(top);
    InventoryDragEvent event = mock(InventoryDragEvent.class);
    when(event.getView()).thenReturn(view);
    when(event.getRawSlots()).thenReturn(rawSlots);
    return event;
  }

  public Material material(Menu menu, int slot) {
    ItemStack stack = menu.getInventory().getItem(slot);
    return stack == null ? null : materials.get(stack);
  }

  public Component name(Menu menu, int slot) {
    ItemStack stack = menu.getInventory().getItem(slot);
    return stack == null ? null : names.get(stack);
  }

  public List<? extends Component> lore(Menu menu, int slot) {
    ItemStack stack = menu.getInventory().getItem(slot);
    return stack == null ? null : lore.get(stack);
  }

  private Inventory inventory(InventoryHolder holder, int size) {
    Inventory inventory = mock(Inventory.class);
    ItemStack[] contents = new ItemStack[size];
    when(inventory.getSize()).thenReturn(size);
    when(inventory.getHolder()).thenReturn(holder);
    when(inventory.getHolder(org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(holder);
    org.mockito.Mockito.doAnswer(inv -> {
      contents[inv.<Integer>getArgument(0)] = inv.getArgument(1);
      return null;
    }).when(inventory).setItem(anyInt(), any());
    when(inventory.getItem(anyInt())).thenAnswer(inv -> contents[inv.<Integer>getArgument(0)]);
    return inventory;
  }

  private ItemStack itemStack(Material material) {
    ItemStack stack = mock(ItemStack.class);
    ItemMeta meta = mock(ItemMeta.class);
    materials.put(stack, material);
    when(stack.getItemMeta()).thenReturn(meta);
    org.mockito.Mockito.doAnswer(inv -> {
      names.put(stack, inv.getArgument(0));
      return null;
    }).when(meta).itemName(any());
    org.mockito.Mockito.doAnswer(inv -> {
      lore.put(stack, inv.getArgument(0));
      return null;
    }).when(meta).lore(any());
    return stack;
  }

  @Override
  public void close() {
    itemStacks.close();
    bukkit.close();
  }
}
