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

    // Fighter-jet attitude display (issue #10): a screen-centered pitch
    // ladder, flight path marker, and AoA indexer, shown only while actually
    // fall-flying. Gated by showElytraOverlay like everything else, plus its
    // own master toggle and per-element toggles. pixelsPerDegree scales the
    // whole display (how far one degree of pitch moves the ladder on screen).
    public boolean showFlightInstruments = true;
    public double flightInstrumentPixelsPerDegree = 4.0;
    public boolean showPitchLadder = true;
    public boolean showFlightPathMarker = true;
    public boolean showAoaIndicator = true;
    public boolean showBearingTape = true;
    public boolean showAirspeedTape = true;
    public boolean showAltitudeTape = true;

    // Half-height, in pixels from screen center, of the simulated "windshield"
    // the vertically-tracking instruments (pitch ladder, airspeed/altitude
    // tapes) are visible through. They fade out approaching this limit rather
    // than hard-clipping or tracking the full screen height, so the display
    // reads like a real HUD's limited glass extent.
    public double immersiveHudHeightPixels = 120.0;
}
