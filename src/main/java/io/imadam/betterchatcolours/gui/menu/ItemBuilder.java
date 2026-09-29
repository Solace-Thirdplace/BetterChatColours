package io.imadam.betterchatcolours.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds menu icons from legacy (section sign) text. The name is set as the item name and every
 * line is rendered white and non-italic unless the text sets its own colour or decoration, which
 * is how the menus looked when they were built with InvUI.
 */
public class ItemBuilder {

  private static final Style FORMATTING_TEMPLATE = Style.style()
      .color(NamedTextColor.WHITE)
      .decoration(TextDecoration.ITALIC, false)
      .decoration(TextDecoration.BOLD, false)
      .decoration(TextDecoration.STRIKETHROUGH, false)
      .decoration(TextDecoration.UNDERLINED, false)
      .decoration(TextDecoration.OBFUSCATED, false)
      .build();

  private final Material material;
  private Component name;
  private final List<Component> lore = new ArrayList<>();

  public ItemBuilder(Material material) {
    this.material = material;
  }

  public ItemBuilder setLegacyName(String name) {
    this.name = fromLegacy(name);
    return this;
  }

  public ItemBuilder addLegacyLoreLines(String... lines) {
    for (String line : lines) {
      lore.add(fromLegacy(line));
    }
    return this;
  }

  public Material getMaterial() {
    return material;
  }

  public Component getName() {
    return name;
  }

  public List<Component> getLore() {
    return lore;
  }

  public ItemStack build() {
    ItemStack item = ItemStack.of(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      if (name != null) {
        meta.itemName(name);
      }
      if (!lore.isEmpty()) {
        meta.lore(lore);
      }
      item.setItemMeta(meta);
    }
    return item;
  }

  static Component fromLegacy(String text) {
    Component component = LegacyComponentSerializer.legacySection().deserialize(text);
    return component.style(component.style().merge(FORMATTING_TEMPLATE, Style.Merge.Strategy.IF_ABSENT_ON_TARGET));
  }
}
