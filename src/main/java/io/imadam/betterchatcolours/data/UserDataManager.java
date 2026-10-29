package io.imadam.betterchatcolours.data;

import io.imadam.betterchatcolours.BetterChatColours;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class UserDataManager {
  private final BetterChatColours plugin;
  // Read by placeholders on chat and region threads, written from each player's own thread. A
  // reload builds a new map and swaps it in, so readers never see it half-loaded.
  private volatile Map<UUID, String> userEquippedPresets;
  private final File userDataFile;

  public UserDataManager(BetterChatColours plugin) {
    this.plugin = plugin;
    this.userEquippedPresets = new ConcurrentHashMap<>();
    this.userDataFile = new File(plugin.getDataFolder(), "userdata.yml");
  }

  public void loadUserData() {
    Map<UUID, String> loaded = new ConcurrentHashMap<>();
    try {
      readUserData(loaded);
    } finally {
      userEquippedPresets = loaded;
    }
  }

  private void readUserData(Map<UUID, String> userEquippedPresets) {
    if (!userDataFile.exists()) {
      return;
    }

    try {
      FileConfiguration config = YamlConfiguration.loadConfiguration(userDataFile);

      if (config.getConfigurationSection("users") == null) {
        return;
      }

      for (String uuidString : config.getConfigurationSection("users").getKeys(false)) {
        try {
          UUID uuid = UUID.fromString(uuidString);
          String preset = config.getString("users." + uuidString + ".equipped");

          if (preset != null && !preset.isEmpty()) {
            userEquippedPresets.put(uuid, preset);
          }
        } catch (IllegalArgumentException e) {
          plugin.getLogger().warning("Invalid UUID in userdata.yml: " + uuidString);
        }
      }

      plugin.getLogger().info("Loaded user data for " + userEquippedPresets.size() + " players");

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading user data", e);
    }
  }

  // Players on different region threads can equip at the same moment; one write at a time
  public synchronized void saveUserData() {
    try {
      FileConfiguration config = new YamlConfiguration();

      for (Map.Entry<UUID, String> entry : userEquippedPresets.entrySet()) {
        config.set("users." + entry.getKey().toString() + ".equipped", entry.getValue());
      }

      config.save(userDataFile);

    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error saving user data", e);
    }
  }

  public void setEquippedPreset(Player player, String presetName) {
    setEquippedPreset(player.getUniqueId(), presetName);
  }

  public void setEquippedPreset(UUID playerUuid, String presetName) {
    if (presetName == null || presetName.isEmpty()) {
      userEquippedPresets.remove(playerUuid);
    } else {
      userEquippedPresets.put(playerUuid, presetName);
    }
    saveUserData();
  }

  public String getEquippedPreset(Player player) {
    return getEquippedPreset(player.getUniqueId());
  }

  public String getEquippedPreset(UUID playerUuid) {
    return userEquippedPresets.get(playerUuid);
  }

  public boolean hasEquippedPreset(Player player) {
    return hasEquippedPreset(player.getUniqueId());
  }

  public boolean hasEquippedPreset(UUID playerUuid) {
    String preset = userEquippedPresets.get(playerUuid);
    return preset != null && !preset.isEmpty();
  }

  public void clearEquippedPreset(Player player) {
    clearEquippedPreset(player.getUniqueId());
  }

  public void clearEquippedPreset(UUID playerUuid) {
    userEquippedPresets.remove(playerUuid);
    saveUserData();
  }

  /**
   * Checks if a player still has permission for their equipped preset.
   * If not, automatically unequips it.
   * @param player The player to check
   * @return true if the preset was unequipped due to lack of permission
   */
  public boolean checkAndUnequipInvalidPreset(Player player) {
    String equippedPreset = getEquippedPreset(player);
    if (equippedPreset == null || equippedPreset.isEmpty()) {
      return false; // No preset equipped
    }

    GlobalPresetData preset = plugin.getGlobalPresetManager().getPreset(equippedPreset);
    if (preset == null) {
      // Preset no longer exists, unequip it
      clearEquippedPreset(player);
      return true;
    }

    // Check if player still has permission for this preset
    String permission = preset.getPermission();
    if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
      // Player lost permission, unequip the preset
      clearEquippedPreset(player);
      player.sendMessage("§c§lNotice: §7Your chatcolor preset '§e" + preset.getName() + "§7' has been unequipped because you no longer have permission to use it.");
      return true;
    }

    return false; // Preset is still valid
  }
}
