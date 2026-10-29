package io.imadam.betterchatcolours.listeners;

import io.imadam.betterchatcolours.BetterChatColours;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Listener to handle permission changes and ensure players
 * don't keep presets they no longer have permission for.
 */
public class PermissionListener implements Listener {

  private final BetterChatColours plugin;

  public PermissionListener() {
    this.plugin = JavaPlugin.getPlugin(BetterChatColours.class);
  }

  /**
   * Check permissions when player joins the server
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    
    // Check permissions after a short delay to ensure all permissions are loaded
    player.getScheduler().runDelayed(plugin,
        task -> plugin.getUserDataManager().checkAndUnequipInvalidPreset(player), null, 20L); // 1 second delay
  }

  /**
   * Check permissions after certain commands that might change permissions
   * This catches cases where permissions are changed via commands like /lp, /pex, etc.
   */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
    if (event.isCancelled()) {
      return;
    }

    String command = event.getMessage().toLowerCase();
    
    // Check if this looks like a permission-related command
    if (command.startsWith("/lp ") || command.startsWith("/luckperms ") ||
        command.startsWith("/pex ") || command.startsWith("/permissions ") ||
        command.startsWith("/group ") || command.startsWith("/user ") ||
        command.startsWith("/perms ")) {
      
      Player player = event.getPlayer();
      
      // Check permissions after a short delay to allow the command to process
      player.getScheduler().runDelayed(plugin,
          task -> plugin.getUserDataManager().checkAndUnequipInvalidPreset(player), null, 5L); // 0.25 second delay
    }
  }
}
