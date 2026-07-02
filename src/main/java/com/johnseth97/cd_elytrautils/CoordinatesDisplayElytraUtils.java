package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.config.BConfigClass;
import dev.boxadactle.boxlib.config.BConfigHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This mod is client-only ({@code "environment": "client"} in fabric.mod.json)
 * so a {@link ClientModInitializer} + {@code "client"} entrypoint is used
 * rather than the generic {@code ModInitializer}/{@code "main"} pair — client
 * rendering registries like HudElementRegistry are only guaranteed-safe to
 * touch from client-entrypoint timing.
 */
public final class CoordinatesDisplayElytraUtils implements ClientModInitializer {
    public static final String MOD_ID = "coordinatesdisplay_elytrautils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Public (not just exposed via getConfig()) so the config screen can call
    // cacheConfig()/restoreCache()/save() directly, matching CoordinatesDisplay's
    // own precedent.
    public static BConfigClass<ElytraUtilsConfig> CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = BConfigHandler.registerConfig(ElytraUtilsConfig.class);
        // Register the attitude display before Master Caution so the caution
        // alert draws last (on top of the instruments).
        FlightInstrumentOverlay.register();
        MasterCautionOverlay.register();
        LOGGER.info("[{}] initialized", MOD_ID);
    }

    public static ElytraUtilsConfig getConfig() {
        return CONFIG.get();
    }
}
