package com.johnseth97.devflags.mixin;

import com.johnseth97.devflags.DevFlagsConfig;
import net.minecraft.SharedConstants;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {

    @Shadow
    public static boolean IS_RUNNING_IN_IDE;

    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void devflags$setDebugProperties(CallbackInfo ci) {
        System.setProperty("MC_DEBUG_ENABLED", "true");
        System.setProperty("MC_DEBUG_HOTKEYS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_PATHFINDING")) System.setProperty("MC_DEBUG_PATHFINDING", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_NEIGHBORSUPDATE")) System.setProperty("MC_DEBUG_NEIGHBORSUPDATE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER")) System.setProperty("MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_STRUCTURES")) System.setProperty("MC_DEBUG_STRUCTURES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_GAME_EVENT_LISTENERS")) System.setProperty("MC_DEBUG_GAME_EVENT_LISTENERS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_VILLAGE_SECTIONS")) System.setProperty("MC_DEBUG_VILLAGE_SECTIONS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_BRAIN")) System.setProperty("MC_DEBUG_BRAIN", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_POI")) System.setProperty("MC_DEBUG_POI", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_BEES")) System.setProperty("MC_DEBUG_BEES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_RAIDS")) System.setProperty("MC_DEBUG_RAIDS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_GOAL_SELECTOR")) System.setProperty("MC_DEBUG_GOAL_SELECTOR", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SHAPES")) System.setProperty("MC_DEBUG_SHAPES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES")) System.setProperty("MC_DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_ENTITY_BLOCK_INTERSECTION")) System.setProperty("MC_DEBUG_ENTITY_BLOCK_INTERSECTION", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_BLOCK_BREAK")) System.setProperty("MC_DEBUG_BLOCK_BREAK", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_BREEZE_MOB")) System.setProperty("MC_DEBUG_BREEZE_MOB", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SCULK_CATALYST")) System.setProperty("MC_DEBUG_SCULK_CATALYST", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_LARGE_DRIPSTONE")) System.setProperty("MC_DEBUG_LARGE_DRIPSTONE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_CARVERS")) System.setProperty("MC_DEBUG_CARVERS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_ORE_VEINS")) System.setProperty("MC_DEBUG_ORE_VEINS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_AQUIFERS")) System.setProperty("MC_DEBUG_AQUIFERS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_FEATURE_COUNT")) System.setProperty("MC_DEBUG_FEATURE_COUNT", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_DEV_COMMANDS")) System.setProperty("MC_DEBUG_DEV_COMMANDS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_VERBOSE_COMMAND_ERRORS")) System.setProperty("MC_DEBUG_VERBOSE_COMMAND_ERRORS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_UNLOCK_ALL_TRADES")) System.setProperty("MC_DEBUG_UNLOCK_ALL_TRADES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_IGNORE_LOCAL_MOB_CAP")) System.setProperty("MC_DEBUG_IGNORE_LOCAL_MOB_CAP", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_OPEN_INCOMPATIBLE_WORLDS")) System.setProperty("MC_DEBUG_OPEN_INCOMPATIBLE_WORLDS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_ALLOW_LOW_SIM_DISTANCE")) System.setProperty("MC_DEBUG_ALLOW_LOW_SIM_DISTANCE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_CHASE_COMMAND")) System.setProperty("MC_DEBUG_CHASE_COMMAND", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_BYPASS_REALMS_VERSION_CHECK")) System.setProperty("MC_DEBUG_BYPASS_REALMS_VERSION_CHECK", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS")) System.setProperty("MC_DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS")) System.setProperty("MC_DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN")) System.setProperty("MC_DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_STRUCTURE_EDIT_MODE")) System.setProperty("MC_DEBUG_STRUCTURE_EDIT_MODE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SAVE_STRUCTURES_AS_SNBT")) System.setProperty("MC_DEBUG_SAVE_STRUCTURES_AS_SNBT", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SHOW_SERVER_DEBUG_VALUES")) System.setProperty("MC_DEBUG_SHOW_SERVER_DEBUG_VALUES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_MONITOR_TICK_TIMES")) System.setProperty("MC_DEBUG_MONITOR_TICK_TIMES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SUBTITLES")) System.setProperty("MC_DEBUG_SUBTITLES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_CURSOR_POS")) System.setProperty("MC_DEBUG_CURSOR_POS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SOCIAL_INTERACTIONS")) System.setProperty("MC_DEBUG_SOCIAL_INTERACTIONS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_WORLD_RECREATE")) System.setProperty("MC_DEBUG_WORLD_RECREATE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_PANORAMA_SCREENSHOT")) System.setProperty("MC_DEBUG_PANORAMA_SCREENSHOT", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_FORCE_ONBOARDING_SCREEN")) System.setProperty("MC_DEBUG_FORCE_ONBOARDING_SCREEN", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_DUMP_TEXTURE_ATLAS")) System.setProperty("MC_DEBUG_DUMP_TEXTURE_ATLAS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SYNCHRONOUS_GL_LOGS")) System.setProperty("MC_DEBUG_SYNCHRONOUS_GL_LOGS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING")) System.setProperty("MC_DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_VERBOSE_SERVER_EVENTS")) System.setProperty("MC_DEBUG_VERBOSE_SERVER_EVENTS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND")) System.setProperty("MC_DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_NAMED_RUNNABLES")) System.setProperty("MC_DEBUG_NAMED_RUNNABLES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_VALIDATE_RESOURCE_PATH_CASE")) System.setProperty("MC_DEBUG_VALIDATE_RESOURCE_PATH_CASE", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_ACTIVE_TEXT_AREAS")) System.setProperty("MC_DEBUG_ACTIVE_TEXT_AREAS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_RENDER_UI_LAYERING_RECTANGLES")) System.setProperty("MC_DEBUG_RENDER_UI_LAYERING_RECTANGLES", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_UI_NARRATION")) System.setProperty("MC_DEBUG_UI_NARRATION", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SHUFFLE_UI_RENDERING_ORDER")) System.setProperty("MC_DEBUG_SHUFFLE_UI_RENDERING_ORDER", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_SHUFFLE_MODELS")) System.setProperty("MC_DEBUG_SHUFFLE_MODELS", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_PREFER_WAYLAND")) System.setProperty("MC_DEBUG_PREFER_WAYLAND", "true");
        if (DevFlagsConfig.isEnabled("MC_DEBUG_DEFAULT_SKIN_OVERRIDE")) System.setProperty("MC_DEBUG_DEFAULT_SKIN_OVERRIDE", "true");
        LoggerFactory.getLogger("devflags").warn("[DevFlags] MC_DEBUG_* properties set from config");
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void devflags$forceDevelopment(CallbackInfo ci) {
        IS_RUNNING_IN_IDE = true;
        LoggerFactory.getLogger("devflags").warn(
            "[DevFlags] SharedConstants.IS_RUNNING_IN_IDE forced to true at class init"
        );
    }
}
