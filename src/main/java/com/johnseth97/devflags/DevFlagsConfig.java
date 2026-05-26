package com.johnseth97.devflags;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class DevFlagsConfig {

  private static final List<String> KNOWN_KEYS = Arrays.asList(
    "MC_DEBUG_PATHFINDING",
    "MC_DEBUG_NEIGHBORSUPDATE",
    "MC_DEBUG_STRUCTURES",
    "MC_DEBUG_GAME_EVENT_LISTENERS",
    "MC_DEBUG_VILLAGE_SECTIONS",
    "MC_DEBUG_BRAIN",
    "MC_DEBUG_POI",
    "MC_DEBUG_BEES",
    "MC_DEBUG_RAIDS",
    "MC_DEBUG_GOAL_SELECTOR",
    "MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER",
    "MC_DEBUG_SHAPES",
    "MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES",
    "MC_DEBUG_ENTITY_BLOCK_INTERSECTION",
    "MC_DEBUG_BLOCK_BREAK",
    "MC_DEBUG_BREEZE_MOB",
    "MC_DEBUG_SCULK_CATALYST",
    "MC_DEBUG_LARGE_DRIPSTONE",
    "MC_DEBUG_CARVERS",
    "MC_DEBUG_ORE_VEINS",
    "MC_DEBUG_AQUIFERS",
    "MC_DEBUG_FEATURE_COUNT",
    "MC_DEBUG_DEV_COMMANDS",
    "MC_DEBUG_VERBOSE_COMMAND_ERRORS",
    "MC_DEBUG_UNLOCK_ALL_TRADES",
    "MC_DEBUG_IGNORE_LOCAL_MOB_CAP",
    "MC_DEBUG_OPEN_INCOMPATIBLE_WORLDS",
    "MC_DEBUG_ALLOW_LOW_SIM_DISTANCE",
    "MC_DEBUG_CHASE_COMMAND",
    "MC_DEBUG_BYPASS_REALMS_VERSION_CHECK",
    "MC_DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS",
    "MC_DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS",
    "MC_DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN",
    "MC_DEBUG_STRUCTURE_EDIT_MODE",
    "MC_DEBUG_SAVE_STRUCTURES_AS_SNBT",
    "MC_DEBUG_SHOW_SERVER_DEBUG_VALUES",
    "MC_DEBUG_MONITOR_TICK_TIMES",
    "MC_DEBUG_SUBTITLES",
    "MC_DEBUG_CURSOR_POS",
    "MC_DEBUG_SOCIAL_INTERACTIONS",
    "MC_DEBUG_WORLD_RECREATE",
    "MC_DEBUG_PANORAMA_SCREENSHOT",
    "MC_DEBUG_FORCE_ONBOARDING_SCREEN",
    "MC_DEBUG_DUMP_TEXTURE_ATLAS",
    "MC_DEBUG_SYNCHRONOUS_GL_LOGS",
    "MC_DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING",
    "MC_DEBUG_VERBOSE_SERVER_EVENTS",
    "MC_DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND",
    "MC_DEBUG_NAMED_RUNNABLES",
    "MC_DEBUG_VALIDATE_RESOURCE_PATH_CASE",
    "MC_DEBUG_ACTIVE_TEXT_AREAS",
    "MC_DEBUG_RENDER_UI_LAYERING_RECTANGLES",
    "MC_DEBUG_UI_NARRATION",
    "MC_DEBUG_SHUFFLE_UI_RENDERING_ORDER",
    "MC_DEBUG_SHUFFLE_MODELS",
    "MC_DEBUG_PREFER_WAYLAND",
    "MC_DEBUG_DEFAULT_SKIN_OVERRIDE"
  );

  private static final File CONFIG_FILE = new File(
    System.getProperty("user.dir"),
    "config/devflags.properties"
  );

  private static final Properties props = new Properties();

  static {
    load();
  }

  private DevFlagsConfig() {}

  private static void load() {
    if (!CONFIG_FILE.exists()) {
      for (String key : KNOWN_KEYS) {
        props.setProperty(key, "false");
      }
      save();
    } else {
      try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
        props.load(fis);
      } catch (IOException e) {
        for (String key : KNOWN_KEYS) {
          props.putIfAbsent(key, "false");
        }
      }
      boolean added = false;
      for (String key : KNOWN_KEYS) {
        if (!props.containsKey(key)) {
          props.setProperty(key, "false");
          added = true;
        }
      }
      if (added) {
        save();
      }
    }
  }

  public static boolean isEnabled(String key) {
    return Boolean.parseBoolean(props.getProperty(key, "false"));
  }

  public static void setFlag(String key, boolean value) {
    props.setProperty(key, Boolean.toString(value));
  }

  public static void save() {
    try {
      CONFIG_FILE.getParentFile().mkdirs();
      try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
        props.store(
          fos,
          "DevFlags configuration. Changes take effect after restart."
        );
      }
    } catch (IOException ignored) {}
  }
}
