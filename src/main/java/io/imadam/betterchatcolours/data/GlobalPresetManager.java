package io.imadam.betterchatcolours.data;

import io.imadam.betterchatcolours.BetterChatColours;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GlobalPresetManager {
  private final BetterChatColours plugin;
  // Read by placeholders on chat and region threads. A reload builds a new map and swaps it in, so
  // a placeholder never sees it empty and unequips a player's preset as missing.
  private volatile Map<String, GlobalPresetData> presets;
  private final File presetsFile;

  public GlobalPresetManager(BetterChatColours plugin) {
    this.plugin = plugin;
    this.presets = new ConcurrentHashMap<>();
    this.presetsFile = new File(plugin.getDataFolder(), "presets.yml");
  }

  // Holds the same lock as savePresets: a save truncates presets.yml before writing, and a reload
  // that read it then would get an empty file and write the defaults over every custom preset.
  public synchronized void loadPresets() {
    Map<String, GlobalPresetData> loaded = new ConcurrentHashMap<>();
    boolean createDefaults;
    try {
      createDefaults = readPresets(loaded);
    } finally {
      presets = loaded;
    }
    if (createDefaults) {
      savePresets();
    }
  }

  /** Fills {@code presets} from presets.yml; returns true when the defaults were used instead. */
  private boolean readPresets(Map<String, GlobalPresetData> presets) {
    if (!presetsFile.exists()) {
      createDefaultPresets(presets);
      return true;
    }

    try {
      FileConfiguration config = YamlConfiguration.loadConfiguration(presetsFile);

      if (config.getConfigurationSection("presets") == null) {
        createDefaultPresets(presets);
        return true;
      }

      for (String presetName : config.getConfigurationSection("presets").getKeys(false)) {
        String path = "presets." + presetName;
        List<String> colors = config.getStringList(path + ".colors");
        String permission = config.getString(path + ".permission", "");

        if (!colors.isEmpty()) {
          presets.put(presetName, new GlobalPresetData(presetName, colors, permission));
        }
      }

      plugin.getLogger().info("Loaded " + presets.size() + " global presets");
      return false;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Error loading presets", e);
      createDefaultPresets(presets);
      return true;
    }
  }

  // Admins on different region threads can edit at the same moment; one write at a time
  public synchronized void savePresets() {
    try {
      FileConfiguration config = new YamlConfiguration();

      for (GlobalPresetData preset : presets.values()) {
        String path = "presets." + preset.getName();
        config.set(path + ".colors", preset.getColors());
        config.set(path + ".permission", preset.getPermission());
      }

      config.save(presetsFile);

    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error saving presets", e);
    }
  }

  private void createDefaultPresets(Map<String, GlobalPresetData> presets) {
    // Standard Minecraft Colors (single colors)
    presets.put("black", new GlobalPresetData("black", List.of("#000000"), "chatcolor.preset.black"));
    presets.put("dark_blue", new GlobalPresetData("dark_blue", List.of("#0000AA"), "chatcolor.preset.dark_blue"));
    presets.put("dark_green", new GlobalPresetData("dark_green", List.of("#00AA00"), "chatcolor.preset.dark_green"));
    presets.put("dark_aqua", new GlobalPresetData("dark_aqua", List.of("#00AAAA"), "chatcolor.preset.dark_aqua"));
    presets.put("dark_red", new GlobalPresetData("dark_red", List.of("#AA0000"), "chatcolor.preset.dark_red"));
    presets.put("dark_purple", new GlobalPresetData("dark_purple", List.of("#AA00AA"), "chatcolor.preset.dark_purple"));
    presets.put("gold", new GlobalPresetData("gold", List.of("#FFAA00"), "chatcolor.preset.gold"));
    presets.put("gray", new GlobalPresetData("gray", List.of("#AAAAAA"), "chatcolor.preset.gray"));
    presets.put("dark_gray", new GlobalPresetData("dark_gray", List.of("#555555"), "chatcolor.preset.dark_gray"));
    presets.put("blue", new GlobalPresetData("blue", List.of("#5555FF"), "chatcolor.preset.blue"));
    presets.put("green", new GlobalPresetData("green", List.of("#55FF55"), "chatcolor.preset.green"));
    presets.put("aqua", new GlobalPresetData("aqua", List.of("#55FFFF"), "chatcolor.preset.aqua"));
    presets.put("red", new GlobalPresetData("red", List.of("#FF5555"), "chatcolor.preset.red"));
    presets.put("light_purple",
        new GlobalPresetData("light_purple", List.of("#FF55FF"), "chatcolor.preset.light_purple"));
    presets.put("yellow", new GlobalPresetData("yellow", List.of("#FFFF55"), "chatcolor.preset.yellow"));
    presets.put("white", new GlobalPresetData("white", List.of("#FFFFFF"), "chatcolor.preset.white"));

    // Premium Gradient Presets
    List<String> rainbowColors = List.of("#ff0000", "#ff8000", "#ffff00", "#80ff00", "#00ff00", "#00ff80", "#00ffff",
        "#0080ff", "#0000ff", "#8000ff", "#ff00ff", "#ff0080");
    presets.put("rainbow", new GlobalPresetData("rainbow", rainbowColors, "chatcolor.preset.rainbow"));

    List<String> oceanColors = List.of("#006994", "#0080ff", "#00bfff");
    presets.put("ocean", new GlobalPresetData("ocean", oceanColors, "chatcolor.preset.ocean"));

    // Additional Premium Gradients
    List<String> crystalColors = List.of("#e6e6fa", "#dda0dd", "#da70d6", "#ba55d3", "#9370db");
    presets.put("crystal", new GlobalPresetData("crystal", crystalColors, "chatcolor.preset.crystal"));

    List<String> lavaColors = List.of("#b22222", "#dc143c", "#ff4500", "#ff6347", "#ffd700");
    presets.put("lava", new GlobalPresetData("lava", lavaColors, "chatcolor.preset.lava"));

    List<String> iceColors = List.of("#f0f8ff", "#e0ffff", "#b0e0e6", "#87ceeb", "#87cefa");
    presets.put("ice", new GlobalPresetData("ice", iceColors, "chatcolor.preset.ice"));

    List<String> shadowColors = List.of("#2f4f4f", "#696969", "#708090", "#778899", "#b0c4de");
    presets.put("shadow", new GlobalPresetData("shadow", shadowColors, "chatcolor.preset.shadow"));

    List<String> emeraldColors = List.of("#00ff00", "#32cd32", "#90ee90", "#98fb98", "#f0fff0");
    presets.put("emerald", new GlobalPresetData("emerald", emeraldColors, "chatcolor.preset.emerald"));

    List<String> rubyColors = List.of("#8b0000", "#dc143c", "#ff1493", "#ff69b4", "#ffb6c1");
    presets.put("ruby", new GlobalPresetData("ruby", rubyColors, "chatcolor.preset.ruby"));

    List<String> sapphireColors = List.of("#000080", "#0000cd", "#4169e1", "#6495ed", "#87ceeb");
    presets.put("sapphire", new GlobalPresetData("sapphire", sapphireColors, "chatcolor.preset.sapphire"));

    plugin.getLogger()
        .info("Created " + presets.size() + " default presets (16 standard colors + 9 premium gradients)");
  }

  // Change and save under one lock, so a reload running at the same time cannot drop the change
  public synchronized void addPreset(String name, List<String> colors, String permission) {
    presets.put(name, new GlobalPresetData(name, colors, permission));
    savePresets();
  }

  public void addPreset(String name, List<String> colors) {
    // Automatically generate permission as chatcolor.preset.{name}
    String permission = "chatcolor.preset." + name.toLowerCase();
    addPreset(name, colors, permission);
  }

  public String generateUniquePresetName(String baseName) {
    String name = baseName;
    int counter = 1;

    while (presetExists(name)) {
      name = baseName + "_" + counter;
      counter++;
    }

    return name;
  }

  public synchronized void removePreset(String name) {
    if (name != null) {
      presets.remove(name);
    }
    savePresets();
  }

  public GlobalPresetData getPreset(String name) {
    return name == null ? null : presets.get(name);
  }

  public Map<String, GlobalPresetData> getAllPresets() {
    return new HashMap<>(presets);
  }

  public List<GlobalPresetData> getAvailablePresets(Player player) {
    List<GlobalPresetData> available = new ArrayList<>();

    for (GlobalPresetData preset : presets.values()) {
      String permission = preset.getPermission();
      if (permission.isEmpty() || player.hasPermission(permission)) {
        available.add(preset);
      }
    }

    return available;
  }

  public boolean presetExists(String name) {
    return name != null && presets.containsKey(name);
  }
}
