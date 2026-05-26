package com.johnseth97.devflags;

import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric mod entry point.
 *
 * The primary work is done by {@link com.johnseth97.devflags.mixin.SharedConstantsMixin}
 * at class-init time, before this method runs. The fallback here catches the case where
 * the mixin fires too late or the field was overwritten between class load and mod init.
 */
public final class DevFlagsMod implements ModInitializer {
    public static final String MOD_ID = "devflags";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[DevFlags] SharedConstants.IS_RUNNING_IN_IDE = {}", SharedConstants.IS_RUNNING_IN_IDE);
        // Belt-and-suspenders: also set it here in case the mixin was too early
        // or the field was reset between class load and mod init.
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            SharedConstants.IS_RUNNING_IN_IDE = true;
            LOGGER.warn("[DevFlags] Mixin did not fire; set IS_RUNNING_IN_IDE = true via onInitialize fallback.");
        }
    }
}
