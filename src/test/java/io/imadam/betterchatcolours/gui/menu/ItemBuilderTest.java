package io.imadam.betterchatcolours.gui.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemBuilderTest {

  @Test
  void textWithoutFormattingIsWhiteAndNotItalic() {
    Component line = ItemBuilder.fromLegacy("Plain");
    assertEquals(NamedTextColor.WHITE, line.color());
    assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.ITALIC));
    assertEquals(TextDecoration.State.FALSE, line.decoration(TextDecoration.BOLD));
  }

  @Test
  void legacyFormattingIsKept() {
    Component name = ItemBuilder.fromLegacy("§e§lPreview");
    Component styled = name.children().isEmpty() ? name : name.children().get(0);
    assertEquals(NamedTextColor.YELLOW, styled.color());
    assertEquals(TextDecoration.State.TRUE, styled.decoration(TextDecoration.BOLD));
    assertEquals(TextDecoration.State.FALSE, name.decoration(TextDecoration.ITALIC));
  }

  @Test
  void buildSetsItemNameAndLore() {
    try (MenuTestSupport server = new MenuTestSupport()) {
      var builder = new ItemBuilder(org.bukkit.Material.PAPER)
          .setLegacyName("§e§lPreview")
          .addLegacyLoreLines("§7one", "", "two");
      var stack = builder.build();
      assertEquals(org.bukkit.Material.PAPER, server.materials.get(stack));
      assertEquals(builder.getName(), server.names.get(stack));
      assertEquals(builder.getLore(), server.lore.get(stack));
      assertEquals(3, server.lore.get(stack).size());
    }
  }
}
