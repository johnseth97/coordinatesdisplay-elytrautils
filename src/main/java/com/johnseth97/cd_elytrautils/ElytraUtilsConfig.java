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
    // Default matches vanilla ChatFormatting.RED (0xFF5555), the color the
    // warning used before it became configurable.
    public int masterCautionColor = 0xFF5555;

    // Blocks of lead time before the exact bingo-fuel crossover to warn at.
    // 0 disables Master Caution entirely. See FlightMath's class doc and
    // MasterCautionOverlay for how this converts to a tick margin.
    public double masterCautionThresholdBlocks = 100.0;

    // See AltimeterMode / FlightMath#findGroundDistance.
    public AltimeterMode altimeterMode = AltimeterMode.AUTO;
    public double autoSwitchoverHeight = 100.0;
    public boolean barometricUseSeaLevel = true;
    public double customBarometricY = 63.0;

    // Per-line HUD toggles. The main status line (pitch/CLIMB-GLIDE-etc) is
    // always shown whenever showElytraOverlay is on; these gate the
    // additional informational lines beneath it independently.
    public boolean showImpactLine = true;
    public boolean showRangeLine = true;
    public boolean showFlightTimeLine = true;
}
