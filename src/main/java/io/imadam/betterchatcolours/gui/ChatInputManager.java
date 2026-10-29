package io.imadam.betterchatcolours.gui;

import io.imadam.betterchatcolours.BetterChatColours;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

  public enum InputType {
    PRESET_NAME,
    HEX_COLOR,
    PERMISSION
  }

  public static class InputSession {
    public final InputType type;
    public final Consumer<String> callback;
    public final Runnable cancelCallback;
    public final String presetName;
    public final List<String> colors;
    public final int colorIndex;

    public InputSession(InputType type, Consumer<String> callback, Runnable cancelCallback,
        String presetName, List<String> colors, int colorIndex) {
      this.type = type;
      this.callback = callback;
      this.cancelCallback = cancelCallback;
      this.presetName = presetName;
      this.colors = colors;
      this.colorIndex = colorIndex;
    }
  }

  // Written from the player's thread when a prompt opens and from the async chat thread when the
  // answer arrives
  private static final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

  public static void requestPresetName(Player player, Consumer<String> callback, Runnable cancelCallback) {
    activeSessions.put(player.getUniqueId(),
        new InputSession(InputType.PRESET_NAME, callback, cancelCallback, null, null, -1));

    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    player.sendMessage(Component.text("[INPUT] Enter the preset name in chat:", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("   • Type the name and press Enter", NamedTextColor.GRAY));
    player.sendMessage(Component.text("   • Type 'cancel' to return to the menu", NamedTextColor.GRAY));
    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
  }

  public static void requestHexColor(Player player, String presetName, List<String> colors,
      Consumer<String> callback, Runnable cancelCallback) {
    activeSessions.put(player.getUniqueId(),
        new InputSession(InputType.HEX_COLOR, callback, cancelCallback, presetName, colors, -1));

    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    player.sendMessage(Component.text("[COLOR] Enter a hex color in chat:", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("   • Format: #ff0000 or ff0000", NamedTextColor.GRAY));
    player.sendMessage(Component.text("   • Type 'cancel' to return to the menu", NamedTextColor.GRAY));
    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
  }

  public static void requestHexColorEdit(Player player, String presetName, List<String> colors, int colorIndex,
      Consumer<String> callback, Runnable cancelCallback) {
    activeSessions.put(player.getUniqueId(),
        new InputSession(InputType.HEX_COLOR, callback, cancelCallback, presetName, colors, colorIndex));

    String currentColor = colors.get(colorIndex);
    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    player
        .sendMessage(Component.text("[EDIT] Edit hex color (currently: " + currentColor + "):", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("   • Format: #ff0000 or ff0000", NamedTextColor.GRAY));
    player.sendMessage(Component.text("   • Type 'cancel' to return to the menu", NamedTextColor.GRAY));
    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
  }

  public static void requestPermission(Player player, String presetName, List<String> colors,
      Consumer<String> callback, Runnable cancelCallback) {
    activeSessions.put(player.getUniqueId(),
        new InputSession(InputType.PERMISSION, callback, cancelCallback, presetName, colors, -1));

    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    player.sendMessage(Component.text("[PERM] Enter permission node (optional):", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("   • Example: chatcolors.premium", NamedTextColor.GRAY));
    player.sendMessage(Component.text("   • Leave empty for no permission", NamedTextColor.GRAY));
    player.sendMessage(Component.text("   • Type 'cancel' to return to the menu", NamedTextColor.GRAY));
    player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
  }

  @EventHandler
  @SuppressWarnings("deprecation")
  public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (!activeSessions.containsKey(playerId)) {
      return;
    }

    // Cancel the chat event so the message doesn't appear in chat
    event.setCancelled(true);

    InputSession session = activeSessions.remove(playerId);
    String message = event.getMessage().trim();

    // Handle cancel
    if (message.equalsIgnoreCase("cancel")) {
      player.sendMessage(Component.text("[CANCELLED] Input cancelled.", NamedTextColor.RED));
      // Run cancel callback on the player's own thread (main thread on Paper, its region on Folia)
      BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
      player.getScheduler().run(plugin, task -> session.cancelCallback.run(), null);
      return;
    }

    // Validate input based on type
    String validatedInput = validateInput(session.type, message, player);
    if (validatedInput == null) {
      // Re-add session for retry
      activeSessions.put(playerId, session);
      return;
    }

    // Run callback on the player's own thread (main thread on Paper, its region on Folia)
    BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
    player.getScheduler().run(plugin, task -> session.callback.accept(validatedInput), null);
  }

  private String validateInput(InputType type, String input, Player player) {
    switch (type) {
      case PRESET_NAME:
        if (input.isEmpty()) {
          player.sendMessage(
              Component.text("[ERROR] Preset name cannot be empty! Try again or type 'cancel':", NamedTextColor.RED));
          return null;
        }
        if (input.length() > 32) {
          player.sendMessage(
              Component.text("[ERROR] Preset name too long (max 32 characters)! Try again or type 'cancel':",
                  NamedTextColor.RED));
          return null;
        }
        // Remove spaces and special characters for safety
        String safeName = input.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (!safeName.equals(input)) {
          player.sendMessage(Component.text("[SUCCESS] Preset name cleaned: " + safeName, NamedTextColor.GREEN));
        }

        // Check for uniqueness and generate unique name if needed
        BetterChatColours plugin = JavaPlugin.getPlugin(BetterChatColours.class);
        if (plugin.getGlobalPresetManager().presetExists(safeName)) {
          String uniqueName = plugin.getGlobalPresetManager().generateUniquePresetName(safeName);
          player.sendMessage(
              Component.text("[SUCCESS] Name already exists! Using unique name: " + uniqueName, NamedTextColor.YELLOW));
          return uniqueName;
        }

        return safeName;

      case HEX_COLOR:
        String hexColor = input.startsWith("#") ? input : "#" + input;
        if (!isValidHexColor(hexColor)) {
          player
              .sendMessage(Component.text("[ERROR] Invalid hex color! Use format #ff0000. Try again or type 'cancel':",
                  NamedTextColor.RED));
          return null;
        }
        return hexColor.toUpperCase();

      case PERMISSION:
        // Empty permission is valid (no permission required)
        if (input.isEmpty()) {
          return "";
        }
        // Basic validation for permission nodes
        if (!input.matches("[a-zA-Z0-9._-]+")) {
          player.sendMessage(Component.text(
              "[ERROR] Invalid permission format! Use only letters, numbers, dots, and hyphens. Try again or type 'cancel':",
              NamedTextColor.RED));
          return null;
        }
        return input.toLowerCase();

      default:
        return input;
    }
  }

  private boolean isValidHexColor(String hex) {
    if (hex == null || !hex.startsWith("#") || hex.length() != 7) {
      return false;
    }

    try {
      Integer.parseInt(hex.substring(1), 16);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static void cancelSession(Player player) {
    activeSessions.remove(player.getUniqueId());
  }
}
