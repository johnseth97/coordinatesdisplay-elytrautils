package com.johnseth97.devflags.screen;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.johnseth97.devflags.DebugRendererState;
import com.johnseth97.devflags.DevFlagDescriptions;
import com.johnseth97.devflags.DevFlagsConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class DevFlagsConfigScreen extends Screen {

  private final Screen parent;

  private static final int BTN_W = 200;
  private static final int BTN_H = 20;
  private static final int BTN_GAP = 4;
  private static final int BTN_STRIDE = BTN_H + BTN_GAP;
  private static final int COLS = 2;

  private static final List<String> RENDERERS_FLAGS = Arrays.asList(
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
    "MC_DEBUG_SCULK_CATALYST"
  );

  private static final List<String> GAMEPLAY_FLAGS = Arrays.asList(
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
    "MC_DEBUG_VERBOSE_SERVER_EVENTS"
  );

  private static final List<String> MISC_FLAGS = Arrays.asList(
    "MC_DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND",
    "MC_DEBUG_NAMED_RUNNABLES",
    "MC_DEBUG_VALIDATE_RESOURCE_PATH_CASE",
    "MC_DEBUG_ACTIVE_TEXT_AREAS",
    "MC_DEBUG_RENDER_UI_LAYERING_RECTANGLES",
    "MC_DEBUG_UI_NARRATION",
    "MC_DEBUG_SHUFFLE_UI_RENDERING_ORDER",
    "MC_DEBUG_SHUFFLE_MODELS",
    "MC_DEBUG_PREFER_WAYLAND",
    "MC_DEBUG_DEFAULT_SKIN_OVERRIDE",
    "MC_DEBUG_DONT_SAVE_WORLD",
    "MC_DEBUG_CHAT_DISABLED",
    "MC_DEBUG_FORCE_TELEMETRY"
  );

  private static final List<String> WORLD_GENERATION_FLAGS = Arrays.asList(
    "MC_DEBUG_LARGE_DRIPSTONE",
    "MC_DEBUG_CARVERS",
    "MC_DEBUG_ORE_VEINS",
    "MC_DEBUG_AQUIFERS",
    "MC_DEBUG_FEATURE_COUNT",
    "MC_DEBUG_DISABLE_LIQUID_SPREADING",
    "MC_DEBUG_DISABLE_FLUID_GENERATION",
    "MC_DEBUG_DISABLE_AQUIFERS",
    "MC_DEBUG_DISABLE_SURFACE",
    "MC_DEBUG_DISABLE_CARVERS",
    "MC_DEBUG_DISABLE_STRUCTURES",
    "MC_DEBUG_DISABLE_FEATURES",
    "MC_DEBUG_DISABLE_ORE_VEINS",
    "MC_DEBUG_DISABLE_BLENDING",
    "MC_DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION"
  );

  private static final Map<String, String> DEBUG_RENDERER_KEYS = Map.ofEntries(
    Map.entry("MC_DEBUG_PATHFINDING", "PathfindingRenderer"),
    Map.entry("MC_DEBUG_NEIGHBORSUPDATE", "NeighborsUpdateRenderer"),
    Map.entry("MC_DEBUG_STRUCTURES", "StructureRenderer"),
    Map.entry("MC_DEBUG_GAME_EVENT_LISTENERS", "GameEventListenerRenderer"),
    Map.entry("MC_DEBUG_VILLAGE_SECTIONS", "VillageSectionsDebugRenderer"),
    Map.entry("MC_DEBUG_BRAIN", "BrainDebugRenderer"),
    Map.entry("MC_DEBUG_POI", "PoiDebugRenderer"),
    Map.entry("MC_DEBUG_BEES", "BeeDebugRenderer"),
    Map.entry("MC_DEBUG_RAIDS", "RaidDebugRenderer"),
    Map.entry("MC_DEBUG_GOAL_SELECTOR", "GoalSelectorDebugRenderer"),
    Map.entry(
      "MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER",
      "RedstoneWireOrientationsRenderer"
    ),
    Map.entry("MC_DEBUG_SHAPES", "ShapeRenderer"),
    Map.entry(
      "MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES",
      "LocalEntityHitboxRenderer"
    ),
    Map.entry(
      "MC_DEBUG_ENTITY_BLOCK_INTERSECTION",
      "EntityBlockIntersectionRenderer"
    ),
    Map.entry("MC_DEBUG_BLOCK_BREAK", "BlockBreakRenderer"),
    Map.entry("MC_DEBUG_BREEZE_MOB", "BreezeDebugRenderer"),
    Map.entry("MC_DEBUG_SCULK_CATALYST", "SculkCatalystDebugRenderer"),
    Map.entry("MC_DEBUG_LARGE_DRIPSTONE", "LargeDripstoneDebugRenderer"),
    Map.entry("MC_DEBUG_CARVERS", "CarverDebugRenderer"),
    Map.entry("MC_DEBUG_ORE_VEINS", "OreVeinDebugRenderer"),
    Map.entry("MC_DEBUG_AQUIFERS", "AquiferDebugRenderer"),
    Map.entry("MC_DEBUG_FEATURE_COUNT", "FeatureCountRenderer")
  );

  private enum Tab {
    RENDERERS,
    GAMEPLAY,
    MISC,
    WORLD_GENERATION,
  }

  private Tab currentTab = Tab.RENDERERS;

  public DevFlagsConfigScreen(Screen parent) {
    super(Component.literal("Dev Flags Config"));
    this.parent = parent;
  }

  public DevFlagsConfigScreen() {
    this(null);
  }

  @Override
  public void onClose() {
    if (this.minecraft != null) {
      this.minecraft.setScreen(this.parent);
    }
  }

  @Override
  protected void init() {
    int tabCount = Tab.values().length;
    int tabW = 80;
    int tabH = 20;
    int tabGap = 4;
    int tabsWidth = tabCount * tabW + (tabCount - 1) * tabGap;
    int tabStartX = this.width / 2 - tabsWidth / 2;

    Tab[] tabs = Tab.values();
    String[] tabLabels = { "Renderers", "Gameplay", "Misc", "World Gen" };
    for (int i = 0; i < tabs.length; i++) {
      final Tab tab = tabs[i];
      addRenderableWidget(
        Button.builder(Component.literal(tabLabels[i]), b -> {
          currentTab = tab;
          rebuildScreen();
        })
          .pos(tabStartX + i * (tabW + tabGap), 20)
          .size(tabW, tabH)
          .build()
      );
    }

    List<String> keys = currentTabKeys();
    int rows = (int) Math.ceil((double) keys.size() / COLS);
    int startX = this.width / 2 - (COLS * BTN_W + (COLS - 1) * BTN_GAP) / 2;
    int startY = 48;

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      int col = i % COLS;
      int row = i / COLS;
      int x = startX + col * (BTN_W + BTN_GAP);
      int y = startY + row * BTN_STRIDE;

      Button button = Button.builder(flagLabel(key), b -> {
        boolean next = !DevFlagsConfig.isEnabled(key);

        DevFlagsConfig.setFlag(key, next);
        DevFlagsConfig.save();

        String rendererClassName = DEBUG_RENDERER_KEYS.get(key);
        if (rendererClassName != null) {
          DebugRendererState.setEnabled(rendererClassName, next);
        }

        b.setMessage(flagLabel(key));
      })
        .pos(x, y)
        .size(BTN_W, BTN_H)
        .build();

      button.setTooltip(Tooltip.create(DevFlagDescriptions.get(key)));
      addRenderableWidget(button);
    }

    int closeY = startY + rows * BTN_STRIDE + 8;
    addRenderableWidget(
      Button.builder(Component.literal("Close"), b -> onClose())
        .pos(this.width / 2 - 50, closeY)
        .size(100, BTN_H)
        .build()
    );
  }

  @Override
  public void extractRenderState(
    GuiGraphicsExtractor g,
    int mouseX,
    int mouseY,
    float partial
  ) {
    g.fill(0, 0, this.width, this.height, 0xB0000000);
    g.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
    g.centeredText(
      this.font,
      Component.literal("Changes take effect after restart"),
      this.width / 2,
      this.height - 8,
      0x888888
    );
    super.extractRenderState(g, mouseX, mouseY, partial);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private void rebuildScreen() {
    clearWidgets();
    init();
  }

  private List<String> currentTabKeys() {
    switch (currentTab) {
      case RENDERERS:
        return RENDERERS_FLAGS;
      case GAMEPLAY:
        return GAMEPLAY_FLAGS;
      case MISC:
        return MISC_FLAGS;
      case WORLD_GENERATION:
        return WORLD_GENERATION_FLAGS;
      default:
        return RENDERERS_FLAGS;
    }
  }

  private static Component flagLabel(String key) {
    boolean on = DevFlagsConfig.isEnabled(key);
    String name = friendlyName(key);
    MutableComponent status = Component.literal(
      on ? " [ON]" : " [OFF]"
    ).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    return Component.literal(name).append(status);
  }

  private static String friendlyName(String key) {
    String stripped = key.startsWith("MC_DEBUG_")
      ? key.substring("MC_DEBUG_".length())
      : key;
    String[] parts = stripped.split("_");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) sb.append(' ');
      String p = parts[i];
      if (!p.isEmpty()) {
        sb.append(Character.toUpperCase(p.charAt(0)));
        sb.append(p.substring(1).toLowerCase());
      }
    }
    return sb.toString();
  }
}
