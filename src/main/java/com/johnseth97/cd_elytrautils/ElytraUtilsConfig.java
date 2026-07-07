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

    // Radar (raycast AGL, not barometric) altitude below which the
    // instruments stay hidden even while fall-flying — lets a user suppress
    // the display right at launch/landing, close to the ground, and only
    // have it appear once actually up at altitude. 0 (default) preserves the
    // old behavior of showing immediately on fall-flying, no minimum. See
    // FlightInstrumentOverlay's render() gate — a raycast miss (nothing
    // within radar range) counts as "above the minimum" regardless of this
    // value, since there's no ground close enough to be a landing.
    public double flightInstrumentMinRadarAltitude = 0.0;

    // Once the player has climbed above flightInstrumentMinRadarAltitude
    // during the current fall-flying session, keep the instruments showing
    // even if they later dip back below it — e.g. buzzing the treetops after
    // having already climbed out. Latch resets the next time fall-flying
    // stops (landing), so it never leaks into the next flight. See
    // FlightInstrumentOverlay's render() gate. Off by default: matches the
    // strict "hidden below the minimum, always" behavior
    // flightInstrumentMinRadarAltitude had before this existed.
    public boolean flightInstrumentMinRadarAltitudeSticky = false;

    // Uniform multiplier applied to every layout constant in
    // FlightInstrumentOverlay (ladder rung spacing, tape offsets, pixels per
    // degree/unit, marker sizes, ...) so the whole display grows or shrinks
    // as one unit rather than each element scaling independently. 1.0 matches
    // the original fixed layout.
    public double flightInstrumentScale = 1.0;

    // Full ARGB, unlike masterCautionColor — this one's BColorPickerButton is
    // constructed with alpha support enabled (see ElytraUtilsConfigScreen), a
    // deliberate exception to the "RGB only" convention elsewhere: the user
    // explicitly wants a HUD-brightness-style transparency control, not just
    // hue. FlightInstrumentOverlay extracts the alpha byte and composes it
    // into every color it draws (structural AND the physics-derived signal
    // colors — reference marks, STALL, the FPM/AoA gradient tint), so the
    // whole display can be dimmed as one unit; only the hue stays fixed for
    // the signal colors, never the RGB itself. Default's top byte must stay
    // 0xFF (fully opaque) — an all-zero default would render nothing, unlike
    // the masterCautionColor pattern where alpha is forced at render time
    // regardless of what's stored.
    public int flightInstrumentColor = 0xFFFFFF55;

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
