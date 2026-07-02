package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.config.BConfigClass;
import dev.boxadactle.boxlib.config.BConfigHandler;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoordinatesDisplayElytraUtils implements ModInitializer {
    public static final String MOD_ID = "coordinatesdisplay_elytrautils";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static BConfigClass<ElytraUtilsConfig> CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = BConfigHandler.registerConfig(ElytraUtilsConfig.class);
        LOGGER.info("[{}] initialized", MOD_ID);
    }

    public static ElytraUtilsConfig getConfig() {
        return CONFIG.get();
    }
}
