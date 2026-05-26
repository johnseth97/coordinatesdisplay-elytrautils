package com.johnseth97.devflags.mixin;

import net.minecraft.SharedConstants;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forces all {@code MC_DEBUG_*} system properties and {@code IS_RUNNING_IN_IDE} to
 * true as early as possible during {@link SharedConstants} class initialization.
 *
 * Two injections are required because {@code SharedConstants.<clinit>} reads the
 * system properties to compute its final fields (e.g. {@code DEBUG_HOTKEYS}) and
 * separately assigns {@code IS_RUNNING_IN_IDE}. The HEAD injection sets the properties
 * before those reads; the TAIL injection sets {@code IS_RUNNING_IN_IDE} after any
 * value the vanilla static initializer may have written.
 */
@Mixin(SharedConstants.class)
public class SharedConstantsMixin {

    // IS_RUNNING_IN_IDE is public static boolean (not final) — no @Mutable needed.
    @Shadow
    public static boolean IS_RUNNING_IN_IDE;

    // Must run at HEAD so the system properties are visible when booleanProperty/debugFlag
    // reads them during <clinit>. DEBUG_HOTKEYS is final — it can only be set this way.
    // Property naming: SharedConstants uses "MC_DEBUG_" prefix (e.g. MC_DEBUG_ENABLED).
    @Inject(method = "<clinit>", at = @At("HEAD"))
    private static void devflags$setDebugProperties(CallbackInfo ci) {
        System.setProperty("MC_DEBUG_ENABLED", "true");
        // Hotkeys (F3+chunk debug keys)
        System.setProperty("MC_DEBUG_HOTKEYS", "true");
        // Debug renderers
        System.setProperty("MC_DEBUG_PATHFINDING", "true");
        System.setProperty("MC_DEBUG_NEIGHBORSUPDATE", "true");
        System.setProperty("MC_DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER", "true");
        System.setProperty("MC_DEBUG_STRUCTURES", "true");
        System.setProperty("MC_DEBUG_GAME_EVENT_LISTENERS", "true");
        System.setProperty("MC_DEBUG_VILLAGE_SECTIONS", "true");
        System.setProperty("MC_DEBUG_BRAIN", "true");
        System.setProperty("MC_DEBUG_POI", "true");
        System.setProperty("MC_DEBUG_BEES", "true");
        System.setProperty("MC_DEBUG_RAIDS", "true");
        System.setProperty("MC_DEBUG_GOAL_SELECTOR", "true");
        LoggerFactory.getLogger("devflags").warn("[DevFlags] All MC_DEBUG_* properties set");
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void devflags$forceDevelopment(CallbackInfo ci) {
        IS_RUNNING_IN_IDE = true;
        LoggerFactory.getLogger("devflags").warn(
            "[DevFlags] SharedConstants.IS_RUNNING_IN_IDE forced to true at class init"
        );
    }
}
