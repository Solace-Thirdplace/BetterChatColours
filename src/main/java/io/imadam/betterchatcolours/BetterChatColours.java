package io.imadam.betterchatcolours;

import io.imadam.betterchatcolours.commands.ChatColorsCommand;
import io.imadam.betterchatcolours.data.GlobalPresetManager;
import io.imadam.betterchatcolours.data.UserDataManager;
import io.imadam.betterchatcolours.gui.menu.MenuListener;
import io.imadam.betterchatcolours.placeholders.ChatColorsExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterChatColours extends JavaPlugin {

  private static BetterChatColours instance;
  private GlobalPresetManager globalPresetManager;
  private UserDataManager userDataManager;

  @Override
  public void onEnable() {
    instance = this;

    // Create data directory
    if (!getDataFolder().exists()) {
      getDataFolder().mkdirs();
    }

    // Initialize managers
    globalPresetManager = new GlobalPresetManager(this);
    userDataManager = new UserDataManager(this);

    // Load data
    globalPresetManager.loadPresets();
    userDataManager.loadUserData();

    // Register commands (main command and aliases)
    ChatColorsCommand commandExecutor = new ChatColorsCommand(this);
    getCommand("chatcolors").setExecutor(commandExecutor);
    
    // Register aliases explicitly to ensure they work
    if (getCommand("cc") != null) {
      getCommand("cc").setExecutor(commandExecutor);
    }
    if (getCommand("chatcolor") != null) {
      getCommand("chatcolor").setExecutor(commandExecutor);
    }

    // Register listeners
    getServer().getPluginManager().registerEvents(new io.imadam.betterchatcolours.gui.ChatInputManager(), this);
    getServer().getPluginManager().registerEvents(new io.imadam.betterchatcolours.listeners.PermissionListener(), this);
    getServer().getPluginManager().registerEvents(new MenuListener(this), this);

    // Register PlaceholderAPI expansion if available
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
      new ChatColorsExpansion(this).register();
      getLogger().info("PlaceholderAPI integration enabled!");
    } else {
      getLogger().warning("PlaceholderAPI not found! Some features may not work.");
    }

    getLogger().info("BetterChatColours has been enabled!");
  }

  @Override
  public void onDisable() {
    // A menu left open after the listener is gone is an ordinary chest
    MenuListener.closeAll();

    // Save data
    if (globalPresetManager != null) {
      globalPresetManager.savePresets();
    }
    if (userDataManager != null) {
      userDataManager.saveUserData();
    }

    getLogger().info("BetterChatColours has been disabled!");
  }

  public static BetterChatColours getInstance() {
    return instance;
  }

  public GlobalPresetManager getGlobalPresetManager() {
    return globalPresetManager;
  }

  public UserDataManager getUserDataManager() {
    return userDataManager;
  }

  public void reload() {
    getLogger().info("Reloading BetterChatColours...");

    // Reload data
    globalPresetManager.loadPresets();
    userDataManager.loadUserData();

    getLogger().info("BetterChatColours reloaded successfully!");
  }
}
