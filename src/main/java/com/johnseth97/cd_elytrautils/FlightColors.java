package com.johnseth97.cd_elytrautils;

/**
 * Shared color math for the elytra flight instruments: the green→orange→red
 * gradient the text HUD row uses to score pitch/descent, plus the raw-int
 * versions the {@link FlightInstrumentOverlay} needs (it draws lines and
 * shapes, not styled {@code Component}s, so it can't reuse HudMixin's
 * {@code Component}-returning helpers).
 *
 * <p>Promoted out of {@code HudMixin} alongside {@link FlightConstants} so the
 * text row and the instrument overlay color identical flight states
 * identically. The bucket logic in {@link #flightStateColor} mirrors
 * {@code HudMixin.buildFlightLine} exactly.
 */
public final class FlightColors {

    public static final int COLOR_RED = 0xFF5555;
    public static final int COLOR_ORANGE = 0xFFAA00;
    public static final int COLOR_GREEN = 0x55FF55;

    private FlightColors() {
    }

    /**
     * Forces a fully-opaque alpha byte onto an RGB color. Every {@code
     * GuiGraphics} draw call ({@code fill}, {@code drawString}, …) silently
     * no-ops when {@code ARGB.alpha(color) == 0}, and the RGB constants above
     * carry a zero alpha byte — so any color handed to a draw call must go
     * through here first. This is the same zero-alpha gotcha that cost a whole
     * debugging session on the Master Caution overlay (see agents.md).
     */
    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    /**
     * Applies an explicit alpha byte (0-255) instead of forcing full opacity.
     * Used for the flight instrument overlay's immersive-HUD-height fade,
     * where alpha 0 is the deliberate "faded fully out" state at the edge of
     * the simulated windshield — not the zero-alpha silent-no-op bug this
     * class's {@link #opaque} exists to avoid; here a draw call quietly doing
     * nothing at alpha 0 is exactly the wanted behavior.
     */
    public static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static int lerpColor(int from, int to, float t) {
        t = clamp01(t);
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int r = Math.round(fr + (tr - fr) * t);
        int g = Math.round(fg + (tg - fg) * t);
        int b = Math.round(fb + (tb - fb) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Green at t=0, orange at t=0.5, red at t=1. */
    public static int threeStopGradient(float t) {
        t = clamp01(t);
        if (t < 0.5f) {
            return lerpColor(COLOR_GREEN, COLOR_ORANGE, t / 0.5f);
        }
        return lerpColor(COLOR_ORANGE, COLOR_RED, (t - 0.5f) / 0.5f);
    }

    /**
     * Gradient color for how far {@code pitch} sits from {@code idealPitch}.
     * When {@code symmetric}, distance is measured in both directions (climb);
     * otherwise only pitch beyond ideal counts (glide's one-directional
     * distance-vs-speed spectrum).
     */
    public static int gradientColor(float pitch, float idealPitch, float span, boolean symmetric) {
        float signedOffset = pitch - idealPitch;
        float distance = symmetric ? Math.abs(signedOffset) : Math.max(0f, signedOffset);
        return threeStopGradient(clamp01(distance / span));
    }

    /** DIVE's danger gradient: green at a mild descent rate, red at the old LAND NOW threshold (issue #8). */
    public static int diveGradientColor(double vy) {
        float t = clamp01((float) ((FlightConstants.DIVE_GREEN_VY - vy)
                / (FlightConstants.DIVE_GREEN_VY - FlightConstants.DIVE_RED_VY)));
        return threeStopGradient(t);
    }

    /**
     * The single flight-state color, matching the CLIMB / GLIDE / DIVE / STALL
     * bucket order of {@code HudMixin.buildFlightLine}. Used to tint the flight
     * path marker and AoA readout so the overlay and the text row agree.
     */
    public static int flightStateColor(float pitch, double vy, double horizontalSpeed, boolean fallFlying) {
        if (!fallFlying) {
            // Pre-launch / no airspeed: score the launch angle like CLIMB.
            return gradientColor(pitch, FlightConstants.IDEAL_CLIMB_PITCH,
                    FlightConstants.CLIMB_GRADIENT_HALF_WIDTH, true);
        }
        if (horizontalSpeed < FlightConstants.STALL_HORIZONTAL_SPEED) {
            return COLOR_RED;
        }
        if (pitch < FlightConstants.CLIMB_UPPER_PITCH) {
            return gradientColor(pitch, FlightConstants.IDEAL_CLIMB_PITCH,
                    FlightConstants.CLIMB_GRADIENT_HALF_WIDTH, true);
        }
        if (pitch < FlightConstants.GLIDE_RED_PITCH) {
            return gradientColor(pitch, FlightConstants.IDEAL_GLIDE_PITCH,
                    FlightConstants.GLIDE_RED_PITCH, false);
        }
        return diveGradientColor(vy);
    }
}
