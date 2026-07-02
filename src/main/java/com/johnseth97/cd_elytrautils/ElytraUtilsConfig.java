package com.johnseth97.cd_elytrautils;

import dev.boxadactle.boxlib.config.BConfig;
import dev.boxadactle.boxlib.config.BConfigFile;

@BConfigFile("coordinatesdisplay_elytrautils")
public class ElytraUtilsConfig implements BConfig {
    public boolean showElytraOverlay = true;

    // RGB only — MasterCautionOverlay always forces full alpha when
    // rendering (0xFF in the top byte), regardless of what's stored here, so
    // a user picking a color here can't reintroduce the zero-alpha bug that
    // silently no-op'd every draw call before it was fixed.
    public int masterCautionColor = 0xFFFFFF;
}
