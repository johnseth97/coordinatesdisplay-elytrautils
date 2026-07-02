package com.johnseth97.cd_elytrautils;

/**
 * Derived elytra-flight thresholds and optimal angles, shared between the
 * text HUD row ({@code mixin/HudMixin}) and the fighter-jet-style flight
 * instrument overlay ({@link FlightInstrumentOverlay}, issue #10).
 *
 * <p>Every value here is derived by decompiling and numerically simulating the
 * actual vanilla physics — not intuition or wiki paraphrase. These lived as
 * {@code private} fields in {@code HudMixin} until the instrument overlay
 * needed the same numbers to draw its angle brackets; they were promoted here
 * (mirroring how {@link FlightMath} was extracted) rather than duplicated, so
 * the ladder's reference marks and the text row can never drift apart. See
 * GitHub issues #2, #3, #8, #9 for the full derivation and simulation method.
 */
public final class FlightConstants {

    private FlightConstants() {
    }

    // CLIMB / GLIDE / DIVE pitch-bucket edges (Minecraft convention: positive
    // pitch = nose DOWN). STALL is not a pitch bucket — see
    // STALL_HORIZONTAL_SPEED. There is no APPROACH bucket: pitch past
    // GLIDE_RED_PITCH goes straight to DIVE.
    public static final float CLIMB_UPPER_PITCH = -8f;
    public static final float GLIDE_RED_PITCH = 30f;

    // STALL keys off horizontal airspeed, not pitch (issue #9). ~0.35
    // blocks/tick is the simulated steady-state minimum airspeed confirmed in
    // #3's derivation (matches the wiki's ~7.2 m/s claim); 0.4 gives a small
    // buffer above that asymptote.
    public static final float STALL_HORIZONTAL_SPEED = 0.4f;

    // DIVE's danger gradient is keyed on descent rate (vy), not pitch. -0.45
    // is the exact threshold the old standalone "LAND NOW" override used
    // (issue #8), kept as the gradient's red endpoint.
    public static final float DIVE_GREEN_VY = -0.05f;
    public static final float DIVE_RED_VY = -0.45f;

    // Ideal pitch to hold during a rocket's burn to maximize peak altitude
    // gained from a single burst (simulated: boost formula + burn duration +
    // post-burn glide, scanned across pitch). -34 deg sits just past the
    // -30 deg stall-onset threshold — the best climb angle is deliberately
    // right at the edge of losing airspeed, not comfortably inside it.
    public static final float IDEAL_CLIMB_PITCH = -34f;
    public static final float CLIMB_GRADIENT_HALF_WIDTH = 25f;

    // Ideal pitch for maximum glide ratio (distance per altitude lost) is
    // dead level; the gradient's speed-optimal end is trimmed to +30 deg
    // (already ~93% of max simulated horizontal speed and where players
    // actually fly).
    public static final float IDEAL_GLIDE_PITCH = 0f;
}
