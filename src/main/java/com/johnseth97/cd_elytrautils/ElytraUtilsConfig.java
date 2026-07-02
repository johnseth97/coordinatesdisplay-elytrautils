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
    // ladder, flight path marker, AoA indexer, bearing tape, and airspeed/
    // altitude tapes, shown only while actually fall-flying. Deliberately
    // has its own master toggle independent of showElytraOverlay (the legacy
    // text HUD row) — see FlightInstrumentOverlay's render() gate — so either
    // display can run without the other.
    public boolean showFlightInstruments = true;

    // Uniform multiplier applied to every layout constant in
    // FlightInstrumentOverlay (ladder rung spacing, tape offsets, pixels per
    // degree/unit, marker sizes, ...) so the whole display grows or shrinks
    // as one unit rather than each element scaling independently. 1.0 matches
    // the original fixed layout.
    public double flightInstrumentScale = 1.0;

    // RGB only, same convention as masterCautionColor (alpha forced opaque at
    // render time). Applied uniformly to the display's structural elements
    // (ladder, horizon, boresight, tape lines/labels/boxes, bearing tape) —
    // real HUD combiner glass renders everything in one phosphor color, so
    // unifying these into one user-configurable color is more authentic, not
    // less. The physics-derived signal colors (climb/dive gradient, STALL,
    // reference marks) stay independent of this: they carry safety meaning
    // (green/orange/red), not just aesthetics, so they aren't user-overridden.
    public int flightInstrumentColor = 0xFFFF55;

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
    // reads like a real HUD's limited glass extent. Independent of
    // flightInstrumentScale — this is the size of the simulated glass, not a
    // zoom level for the symbols drawn on it.
    public double immersiveHudHeightPixels = 120.0;
}
