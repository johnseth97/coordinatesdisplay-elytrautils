package com.johnseth97.devflags;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;

public final class DevFlagDescriptions {

  private static final Map<String, Component> DESCRIPTIONS = new HashMap<>();

  static {
    put(
      "MC_DEBUG_PATHFINDING",
      "Shows pathfinding debug visualizations for mobs."
    );
    put("MC_DEBUG_NEIGHBORSUPDATE", "Shows neighbor update debug information.");
    put(
      "MC_DEBUG_STRUCTURES",
      "Shows structure generation/debug visualization."
    );
    put(
      "MC_DEBUG_GAME_EVENT_LISTENERS",
      "Shows game event listener debug information."
    );
    put(
      "MC_DEBUG_VILLAGE_SECTIONS",
      "Shows village section/debug POI area data."
    );
    put("MC_DEBUG_BRAIN", "Shows mob brain/AI debug information.");
    put("MC_DEBUG_POI", "Shows point-of-interest debug information.");
    put("MC_DEBUG_BEES", "Shows bee AI, hive, and flower debug information.");
    put("MC_DEBUG_RAIDS", "Shows raid debug information.");
    put("MC_DEBUG_GOAL_SELECTOR", "Shows mob goal selector debug information.");
    put(
      "MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER",
      "Shows experimental redstone wire update-order debug information."
    );
    put("MC_DEBUG_SHAPES", "Shows collision/shape debug information.");
    put(
      "MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES",
      "Shows local server-side entity hitboxes."
    );
    put(
      "MC_DEBUG_ENTITY_BLOCK_INTERSECTION",
      "Shows entity/block intersection debug information."
    );
    put("MC_DEBUG_BLOCK_BREAK", "Shows block breaking debug information.");
    put("MC_DEBUG_BREEZE_MOB", "Shows Breeze mob debug information.");
    put("MC_DEBUG_SCULK_CATALYST", "Shows sculk catalyst debug information.");
    put(
      "MC_DEBUG_LARGE_DRIPSTONE",
      "Shows large dripstone feature debug information."
    );
    put("MC_DEBUG_CARVERS", "Shows cave carver debug information.");
    put("MC_DEBUG_ORE_VEINS", "Shows ore vein debug information.");
    put("MC_DEBUG_AQUIFERS", "Shows aquifer debug information.");
    put(
      "MC_DEBUG_FEATURE_COUNT",
      "Shows worldgen feature count debug information."
    );
  }

  private DevFlagDescriptions() {}

  private static void put(String key, String description) {
    DESCRIPTIONS.put(key, Component.literal(description));
  }

  public static Component get(String key) {
    return DESCRIPTIONS.getOrDefault(
      key,
      Component.literal("No description available.")
    );
  }
}
