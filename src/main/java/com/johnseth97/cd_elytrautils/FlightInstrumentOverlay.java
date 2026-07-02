package com.johnseth97.cd_elytrautils;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Fighter-jet-style attitude display for elytra flight (GitHub issue #10):
 * a horizon-referenced pitch ladder, a flight path marker (velocity vector),
 * a fixed boresight (nose reference), an angle-of-attack indexer, a bearing
 * tape, and airspeed/altitude tapes (BALT with a RALT box beneath it).
 *
 * <p>Like {@link MasterCautionOverlay}, this is its own {@code
 * HudElementRegistry} layer, NOT part of CoordinatesDisplay's corner HUD
 * block: it's screen-space custom drawing (lines, ticks, shapes) centered on
 * the crosshair, not text rows in CD's layout, so it doesn't belong in {@code
 * HudMixin}. It draws with {@code GuiGraphics#fill} rectangles; every color
 * is passed through {@link FlightColors#withAlpha} because a zero alpha byte
 * makes a draw call silently no-op (see agents.md) — {@code hSeg}/{@code
 * vSeg} below take a fully-composed ARGB int for exactly that reason, so
 * alpha is never an afterthought at a call site.
 *
 * <p>{@code config.showFlightInstruments} is this overlay's only visibility
 * gate (besides fall-flying) — deliberately independent of {@code
 * showElytraOverlay} (the legacy CD text row's toggle), so either display can
 * run without the other.
 *
 * <h2>Angle conventions</h2>
 * All angles use Minecraft's pitch convention — <b>positive = nose DOWN</b>
 * ({@link LocalPlayer#getXRot()}). The pitch ladder is labeled the way pilots
 * read one, in <i>elevation</i> (positive = climb, i.e. {@code -pitch}). The
 * ladder is fixed to the horizon and the boresight/FPM float against it,
 * mirroring a real HUD. Bearing is a standard compass heading (0 = north),
 * derived from yaw via {@code (yaw + 180) mod 360} — Minecraft yaw 0 is
 * south, 180/-180 is north.
 *
 * <p><b>Signs to confirm in flight.</b> The vertical geometry (a rung for
 * elevation {@code e} sits at {@code cy - (e + pitch) * pxPerDeg}) was derived
 * and checked against concrete cases, so the ladder and FPM vertical placement
 * are self-consistent. Three things genuinely can't be settled without flying
 * and are called out at their use sites: (1) the flight-path-angle sign from
 * {@code atan2(-vy, h)}, (2) the lateral FPM drift sign, and (3) the AoA
 * display sign. Each is isolated so flipping one is a one-line change.
 *
 * <h2>Immersive HUD height</h2>
 * Real HUD combiner glass only covers part of the pilot's view — the tapes
 * and ladder don't extend to the edges of the whole windshield. {@code
 * config.immersiveHudHeightPixels} models that: vertically-tracking elements
 * (pitch ladder, airspeed/altitude tapes, FPM) fade to transparent over
 * {@link #FADE_BAND_PX} as they approach that half-height from center, via
 * {@link #verticalFadeAlpha}, instead of hard-clipping or tracking the full
 * screen. The bearing tape and the fixed boxed readouts (BALT/RALT/airspeed/
 * heading boxes) are not subject to this fade — they sit at a fixed position
 * rather than tracking.
 *
 * <h2>Scale and color</h2>
 * {@code config.flightInstrumentScale} is a single multiplier applied to
 * every layout constant below (offsets, spacing, marker sizes, pixels-per-
 * unit) via {@link #scaled}, so the whole display grows or shrinks as one
 * unit. {@code config.flightInstrumentColor} is a full ARGB value (unlike
 * most of this mod's other colors, which are RGB-only with alpha forced at
 * render time — see {@code ElytraUtilsConfig}): its RGB recolors the
 * structural elements (ladder, horizon, boresight, tape lines/labels/boxes,
 * bearing tape) uniformly, matching how a real HUD combiner renders
 * everything in one phosphor color, and its alpha byte is extracted as
 * {@code baseAlpha} and composed (via {@link FlightColors#combineAlpha})
 * into literally every color this class draws, including the physics-derived
 * signal colors ({@link FlightColors#flightStateColor}, the climb/dive
 * reference marks, the STALL flag) — a transparency/"HUD brightness" control,
 * not a recolor, so it doesn't compromise what those colors mean, only how
 * visible they are.
 */
public final class FlightInstrumentOverlay {

    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            CoordinatesDisplayElytraUtils.MOD_ID, "flight_instruments");

    // Ladder geometry (screen pixels at scale 1.0). The rung gap keeps the
    // middle clear so the boresight and FPM read cleanly against the ladder.
    private static final int BASE_RUNG_GAP = 26;        // half-width of the clear center gap
    private static final int BASE_RUNG_LENGTH = 34;     // length of each side rung segment
    private static final int BASE_HORIZON_HALF_WIDTH = 90;
    private static final int RUNG_STEP_DEGREES = 10;    // value-space, not scaled
    private static final int LADDER_MAX_ELEVATION = 80; // value-space, not scaled

    // Reference marks, grounded in the derived constants (not eyeballed):
    // ideal rocket climb and the dive-caution threshold, both converted from
    // pitch (positive-down) to ladder elevation (positive-up).
    private static final float IDEAL_CLIMB_ELEVATION = -FlightConstants.IDEAL_CLIMB_PITCH; // +34
    private static final float DIVE_CAUTION_ELEVATION = -FlightConstants.GLIDE_RED_PITCH;  // -30

    private static final int BASE_PIXELS_PER_DEGREE = 4; // ladder / AoA span / FPM lateral drift

    private static final int BASE_BORESIGHT_INNER = 6;
    private static final int BASE_BORESIGHT_OUTER = 18;

    private static final int BASE_FPM_RADIUS = 4;
    private static final int BASE_FPM_WING = 8;
    private static final int BASE_FPM_STUB = 6;

    // AoA indexer geometry: a fixed vertical tape left of center. Small and
    // fixed-height, so it's not "tracking" the way the ladder/airspeed/
    // altitude tapes are — no immersive-height fade applied. Offset chosen
    // (95, not the airspeed tape's 150) so its numeric readout — anchored to
    // the RIGHT at ax+7, extending left — can never reach the airspeed tape's
    // label column regardless of vertical position.
    private static final int BASE_AOA_TAPE_OFFSET_X = 95;
    private static final int BASE_AOA_TAPE_HALF_HEIGHT = 40;
    private static final float AOA_DISPLAY_SPAN = 15f; // value-space: degrees mapped over the tape half-height

    // Immersive-HUD-height fade band: vertically-tracking elements fade to
    // zero alpha over this many pixels as they approach the configured
    // half-height from center, rather than hard-clipping. Fixed regardless of
    // scale — it's a glass-edge softness, not a symbol size.
    private static final int FADE_BAND_PX = 24;

    // Airspeed tape (left). Displayed in blocks/second (horizontalSpeed * 20)
    // rather than raw blocks/tick, since that reads as a much more HUD-scale
    // "airspeed" number (elytra cruise is roughly 8-12, boosted well past 40).
    private static final int BASE_AIRSPEED_TAPE_OFFSET_X = 150;
    private static final double AIRSPEED_STEP = 10.0; // value-space
    private static final double BASE_AIRSPEED_PX_PER_UNIT = 3.0;

    // Altitude tape (right) — BALT, from FlightMath.barometricAltitude, plus
    // a RALT box from FlightMath.radarAltitude anchored to the *bottom* of the
    // immersive HUD extent (not directly under BALT): it's a separate
    // fixed-position instrument, like a real radar altimeter readout, not
    // part of the scrolling BALT tape, so it needs clearance from BALT's own
    // moving tick labels rather than sitting right where they pass through.
    private static final int BASE_ALTITUDE_TAPE_OFFSET_X = 150;
    private static final double ALTITUDE_STEP = 50.0; // value-space
    private static final double BASE_ALTITUDE_PX_PER_UNIT = 1.0;
    private static final int BASE_RALT_BOTTOM_MARGIN = 14;

    // Bearing tape (top). Not immersive-height-faded — it's a fixed row near
    // the top, not a vertically-tracking element.
    private static final int BASE_BEARING_TAPE_Y = 34;
    private static final double BEARING_STEP = 30.0; // value-space
    private static final double BASE_BEARING_PX_PER_DEGREE = 3.0;
    private static final int BASE_BEARING_TAPE_HALF_WIDTH = 100;

    private FlightInstrumentOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(LAYER_ID, FlightInstrumentOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        // Deliberately independent of config.showElytraOverlay — see class doc.
        if (player == null || !config.showFlightInstruments || !player.isFallFlying()) {
            return;
        }

        float pitch = player.getXRot(); // + = nose down
        float yaw = player.getYRot();
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Flight path angle γ, in the same positive-down convention as pitch,
        // so it's directly comparable (AoA = pitch − γ). SIGN TO VERIFY (1):
        // -vy so that descending (vy < 0) gives γ > 0 = flight path pointing
        // down, matching "positive pitch = nose down".
        double gamma = Math.toDegrees(Math.atan2(-vy, horizontalSpeed));
        double aoa = pitch - gamma;

        float scale = (float) config.flightInstrumentScale;
        int structColor = config.flightInstrumentColor & 0xFFFFFF;
        int baseAlpha = (config.flightInstrumentColor >>> 24) & 0xFF;
        double pxPerDeg = BASE_PIXELS_PER_DEGREE * scale;
        // Vanilla's own crosshair (Gui#renderCrosshair) blits a 15x15 sprite
        // at ((guiWidth-15)/2, (guiHeight-15)/2), NOT guiWidth()/2 — with an
        // odd sprite size, that's a different integer-division rounding.
        // For an even guiWidth this puts the sprite's true center pixel at
        // guiWidth/2 - 1, one pixel left of the naive guiWidth()/2 this used
        // to use — matched exactly here so the boresight/ladder/FPM line up
        // with the actual crosshair instead of sitting 1px off it.
        int cx = (graphics.guiWidth() - 15) / 2 + 7;
        int cy = (graphics.guiHeight() - 15) / 2 + 7;
        int immersiveHalfHeight = (int) Math.round(config.immersiveHudHeightPixels);
        int stateColor = FlightColors.flightStateColor(pitch, vy, horizontalSpeed, true);
        Font font = client.font;

        if (config.showPitchLadder) {
            drawPitchLadder(graphics, font, cx, cy, pitch, pxPerDeg, immersiveHalfHeight, scale, structColor, baseAlpha);
        }
        drawBoresight(graphics, cx, cy, scale, structColor, baseAlpha);
        if (config.showFlightPathMarker) {
            drawFlightPathMarker(graphics, cx, cy, aoa, yaw, velocity, horizontalSpeed, pxPerDeg,
                    immersiveHalfHeight, scale, stateColor, baseAlpha);
        }
        if (config.showAoaIndicator) {
            drawAoaIndicator(graphics, font, cx, cy, pitch, gamma, horizontalSpeed, scale, structColor, stateColor, baseAlpha);
        }
        if (config.showBearingTape) {
            drawBearingTape(graphics, font, cx, yaw, scale, structColor, baseAlpha);
        }
        if (config.showAirspeedTape) {
            drawAirspeedTape(graphics, font, cx, cy, horizontalSpeed * 20.0, immersiveHalfHeight, scale, structColor, baseAlpha);
        }
        if (config.showAltitudeTape) {
            drawAltitudeTape(graphics, font, player, cx, cy, immersiveHalfHeight, scale, structColor, baseAlpha);
        }
    }

    // ── Pitch ladder ──────────────────────────────────────────────────────

    private static void drawPitchLadder(GuiGraphics g, Font font, int cx, int cy, float pitch, double pxPerDeg,
                                        int immersiveHalfHeight, float scale, int structColor, int baseAlpha) {
        int rungGap = scaled(BASE_RUNG_GAP, scale);
        int rungLength = scaled(BASE_RUNG_LENGTH, scale);
        int horizonHalfWidth = scaled(BASE_HORIZON_HALF_WIDTH, scale);

        for (int elevation = -LADDER_MAX_ELEVATION; elevation <= LADDER_MAX_ELEVATION; elevation += RUNG_STEP_DEGREES) {
            // A rung for elevation e sits (e - noseElevation) degrees above the
            // nose, where noseElevation = -pitch; screen-y grows downward, so
            // being above center subtracts. → cy - (e + pitch) * pxPerDeg.
            int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
            int alpha = FlightColors.combineAlpha(verticalFadeAlpha(Math.abs(y - cy), immersiveHalfHeight), baseAlpha);
            if (alpha == 0) {
                continue;
            }

            if (elevation == 0) {
                drawHorizonLine(g, cx, y, alpha, rungGap, horizonHalfWidth, structColor);
                continue;
            }
            boolean climb = elevation > 0;
            drawRung(g, cx, y, climb, structColor, alpha, rungGap, rungLength);
            drawRungLabel(g, font, cx, y, Math.abs(elevation), structColor, alpha, rungGap, rungLength);
        }

        // Derived-constant reference brackets, drawn on top of the grid. These
        // stay red/green (physics-derived signal colors), not structColor —
        // only their alpha (transparency) respects the user's instrument color.
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, immersiveHalfHeight, IDEAL_CLIMB_ELEVATION, FlightColors.COLOR_GREEN, rungGap, baseAlpha);
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, immersiveHalfHeight, DIVE_CAUTION_ELEVATION, FlightColors.COLOR_RED, rungGap, baseAlpha);
    }

    private static void drawHorizonLine(GuiGraphics g, int cx, int y, int alpha, int rungGap, int horizonHalfWidth, int rgb) {
        int argb = FlightColors.withAlpha(rgb, alpha);
        hSeg(g, cx - horizonHalfWidth, cx - rungGap, y, argb);
        hSeg(g, cx + rungGap, cx + horizonHalfWidth, y, argb);
        // Small downward end caps mark the true horizon ends.
        vSeg(g, cx - horizonHalfWidth, y, y + 4, argb);
        vSeg(g, cx + horizonHalfWidth, y, y + 4, argb);
    }

    /** A climb rung is solid with ticks pointing down toward the horizon; a dive rung is dashed with ticks up. */
    private static void drawRung(GuiGraphics g, int cx, int y, boolean climb, int rgb, int alpha, int rungGap, int rungLength) {
        int argb = FlightColors.withAlpha(rgb, alpha);
        int leftInner = cx - rungGap;
        int rightInner = cx + rungGap;
        int leftOuter = leftInner - rungLength;
        int rightOuter = rightInner + rungLength;

        if (climb) {
            hSeg(g, leftOuter, leftInner, y, argb);
            hSeg(g, rightInner, rightOuter, y, argb);
            // End ticks point toward the horizon (down for climb rungs).
            vSeg(g, leftInner, y, y + 4, argb);
            vSeg(g, rightInner, y, y + 4, argb);
        } else {
            dashedHSeg(g, leftOuter, leftInner, y, argb);
            dashedHSeg(g, rightInner, rightOuter, y, argb);
            vSeg(g, leftInner, y - 4, y, argb);
            vSeg(g, rightInner, y - 4, y, argb);
        }
    }

    private static void drawRungLabel(GuiGraphics g, Font font, int cx, int y, int elevationMagnitude, int rgb,
                                      int alpha, int rungGap, int rungLength) {
        String label = Integer.toString(elevationMagnitude);
        int textY = y - font.lineHeight / 2;
        int argb = FlightColors.withAlpha(rgb, alpha);
        // Labels just outside each rung's outer end.
        g.drawString(font, label, cx - rungGap - rungLength - 4 - font.width(label), textY, argb, false);
        g.drawString(font, label, cx + rungGap + rungLength + 4, textY, argb, false);
    }

    private static void drawReferenceMark(GuiGraphics g, int cx, int cy, float pitch, double pxPerDeg,
                                          int immersiveHalfHeight, float elevation, int rgb, int rungGap, int baseAlpha) {
        int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
        int alpha = FlightColors.combineAlpha(verticalFadeAlpha(Math.abs(y - cy), immersiveHalfHeight), baseAlpha);
        if (alpha == 0) {
            return;
        }
        int argb = FlightColors.withAlpha(rgb, alpha);
        // A short colored bar bridging the center gap: an unmistakable target
        // line at a physics-derived elevation (ideal climb / dive caution).
        hSeg(g, cx - rungGap + 2, cx + rungGap - 2, y, argb);
        hSeg(g, cx - rungGap + 2, cx + rungGap - 2, y - 1, argb);
    }

    // ── Boresight (fixed nose reference) ──────────────────────────────────

    private static void drawBoresight(GuiGraphics g, int cx, int cy, float scale, int rgb, int baseAlpha) {
        // Waterline symbol: two short wings, no center dot — matches a real
        // HUD's waterline marker more closely than a dot-plus-wings would.
        // Fixed at center, so never subject to the immersive-height fade.
        int inner = scaled(BASE_BORESIGHT_INNER, scale);
        int outer = scaled(BASE_BORESIGHT_OUTER, scale);
        int argb = FlightColors.withAlpha(rgb, baseAlpha);
        hSeg(g, cx - outer, cx - inner, cy, argb);
        hSeg(g, cx + inner, cx + outer, cy, argb);
    }

    // ── Flight path marker (velocity vector) ──────────────────────────────

    private static void drawFlightPathMarker(GuiGraphics g, int cx, int cy, double aoa, float yaw,
                                             Vec3 velocity, double horizontalSpeed, double pxPerDeg,
                                             int immersiveHalfHeight, float scale, int rgb, int baseAlpha) {
        // Vertical: the FPM sits below/above the boresight by the AoA, since
        // its elevation minus the nose's equals (pitch − γ) = aoa.
        int fy = cy - (int) Math.round(aoa * pxPerDeg);

        // Lateral: drift between where the nose points (yaw) and where the
        // velocity vector actually points. SIGN TO VERIFY (2): atan2(-vx, vz)
        // matches Minecraft's yaw convention (forward = (-sin y, cos y)).
        int horizonHalfWidth = scaled(BASE_HORIZON_HALF_WIDTH, scale);
        int fx = cx;
        if (horizontalSpeed > 0.05) {
            double velocityYaw = Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            double drift = Mth.wrapDegrees(velocityYaw - yaw);
            fx = cx + (int) Math.round(drift * pxPerDeg);
        }
        fx = Mth.clamp(fx, cx - horizonHalfWidth, cx + horizonHalfWidth);
        fy = Mth.clamp(fy, cy - immersiveHalfHeight, cy + immersiveHalfHeight);

        int alpha = FlightColors.combineAlpha(verticalFadeAlpha(Math.abs(fy - cy), immersiveHalfHeight), baseAlpha);
        if (alpha == 0) {
            return;
        }
        int argb = FlightColors.withAlpha(rgb, alpha);

        int r = scaled(BASE_FPM_RADIUS, scale);
        int wing = scaled(BASE_FPM_WING, scale);
        int stub = scaled(BASE_FPM_STUB, scale);
        // Ring (square outline approximating the FPM circle).
        hSeg(g, fx - r, fx + r, fy - r, argb);
        hSeg(g, fx - r, fx + r, fy + r, argb);
        vSeg(g, fx - r, fy - r, fy + r, argb);
        vSeg(g, fx + r, fy - r, fy + r, argb);
        // Wings and top stub.
        hSeg(g, fx - r - wing, fx - r, fy, argb);
        hSeg(g, fx + r, fx + r + wing, fy, argb);
        vSeg(g, fx, fy - r - stub, fy - r, argb);
    }

    // ── Angle-of-attack indexer ───────────────────────────────────────────

    private static void drawAoaIndicator(GuiGraphics g, Font font, int cx, int cy, float pitch, double gamma,
                                         double horizontalSpeed, float scale, int structColor, int stateColor, int baseAlpha) {
        int offsetX = scaled(BASE_AOA_TAPE_OFFSET_X, scale);
        int halfHeight = scaled(BASE_AOA_TAPE_HALF_HEIGHT, scale);
        int ax = cx - offsetX;
        int top = cy - halfHeight;
        int bottom = cy + halfHeight;
        float pxPerDeg = halfHeight / AOA_DISPLAY_SPAN;

        // Vertical scale.
        int structArgb = FlightColors.withAlpha(structColor, baseAlpha);
        vSeg(g, ax, top, bottom, structArgb);
        for (int tick = -15; tick <= 15; tick += 5) {
            int ty = cy - Math.round(tick * pxPerDeg);
            int len = scaled(tick == 0 ? 7 : 4, scale); // emphasize the 0 datum (nose aligned with velocity)
            hSeg(g, ax - len, ax, ty, structArgb);
        }

        // Displayed AoA. SIGN TO VERIFY (3): shown in the aviation-intuitive
        // sense (positive = nose above the flight path = generating lift),
        // which is (γ − pitch), the negation of the internal "pitch − γ". The
        // caret rides up for higher displayed AoA.
        double displayAoa = gamma - pitch;
        int caretY = cy - Math.round((float) Mth.clamp(displayAoa, -AOA_DISPLAY_SPAN, AOA_DISPLAY_SPAN) * pxPerDeg);
        int caretColor = FlightColors.withAlpha(stateColor, baseAlpha);
        int caretSize = scaled(5, scale);
        // Left-pointing caret whose apex touches the tape at the current AoA.
        for (int i = 0; i < caretSize; i++) {
            g.fill(ax + 2 + i, caretY - i, ax + 3 + i, caretY + i + 1, caretColor);
        }

        // "α" (the aviation angle-of-attack symbol) instead of the word "AoA"
        // — shorter still than the bare-degree version, and reads as the
        // actual instrument symbol rather than an abbreviation. Kept short so
        // its left-extending bounds stay well clear of the airspeed tape
        // regardless of font metrics (see BASE_AOA_TAPE_OFFSET_X).
        String text = String.format("α%.0f°", displayAoa);
        g.drawString(font, text, ax - font.width(text) + 7, top - font.lineHeight - 2, caretColor, false);

        // Stall flag, grounded in the derived airspeed floor (issue #9) rather
        // than any AoA value — elytra stall is an airspeed floor, not an angle.
        if (horizontalSpeed < FlightConstants.STALL_HORIZONTAL_SPEED) {
            String stall = "STALL";
            g.drawString(font, stall, ax - font.width(stall) + 7, bottom + 3,
                    FlightColors.withAlpha(FlightColors.COLOR_RED, baseAlpha), false);
        }
    }

    // ── Bearing tape (top) ─────────────────────────────────────────────────

    private static void drawBearingTape(GuiGraphics g, Font font, int cx, float yaw, float scale, int structColor, int baseAlpha) {
        // Compass heading, 0 = north: Minecraft yaw 0 is south, 180/-180 is
        // north, increasing clockwise (west at +90) — so heading = yaw + 180,
        // wrapped positive.
        float heading = Mth.positiveModulo(yaw + 180f, 360f);
        int y = scaled(BASE_BEARING_TAPE_Y, scale);
        int halfWidth = scaled(BASE_BEARING_TAPE_HALF_WIDTH, scale);
        double pxPerDegree = BASE_BEARING_PX_PER_DEGREE * scale;
        int structArgb = FlightColors.withAlpha(structColor, baseAlpha);

        int windowDegrees = (int) Math.ceil(halfWidth / pxPerDegree) + (int) BEARING_STEP;
        double firstTick = Math.floor((heading - windowDegrees) / BEARING_STEP) * BEARING_STEP;
        for (double tick = firstTick; tick <= heading + windowDegrees; tick += BEARING_STEP) {
            float delta = Mth.wrapDegrees((float) (tick - heading));
            int x = cx + Math.round(delta * (float) pxPerDegree);
            if (Math.abs(x - cx) > halfWidth) {
                continue;
            }

            int wrapped = (int) (((Math.round(tick) % 360) + 360) % 360);
            String label = cardinalOrDegrees(wrapped);
            vSeg(g, x, y, y + 5, structArgb);
            g.drawString(font, label, x - font.width(label) / 2, y + 7, structArgb, false);
        }

        hSeg(g, cx - halfWidth, cx + halfWidth, y, structArgb);
        // Fixed index caret marking "straight ahead" at center.
        for (int i = 0; i < 4; i++) {
            hSeg(g, cx - i, cx + i, y - 2 - i, structArgb);
        }

        String readout = String.format("%03d", Math.round(heading) % 360);
        drawValueBox(g, font, cx, y - 15, readout, structColor, baseAlpha);
    }

    private static String cardinalOrDegrees(int wrappedHeading) {
        return switch (wrappedHeading) {
            case 0 -> "N";
            case 90 -> "E";
            case 180 -> "S";
            case 270 -> "W";
            default -> String.format("%03d", wrappedHeading);
        };
    }

    // ── Airspeed / altitude tapes ────────────────────────────────────────

    private static void drawAirspeedTape(GuiGraphics g, Font font, int cx, int cy, double airspeedBlocksPerSecond,
                                         int immersiveHalfHeight, float scale, int structColor, int baseAlpha) {
        int spineX = cx - scaled(BASE_AIRSPEED_TAPE_OFFSET_X, scale);
        double pxPerUnit = BASE_AIRSPEED_PX_PER_UNIT * scale;
        drawVerticalTape(g, font, spineX, cy, airspeedBlocksPerSecond, AIRSPEED_STEP, pxPerUnit,
                immersiveHalfHeight, false, false, scale, structColor, baseAlpha);
        String readout = Long.toString(Math.round(airspeedBlocksPerSecond));
        drawValueBox(g, font, spineX, cy, readout, structColor, baseAlpha);
    }

    private static void drawAltitudeTape(GuiGraphics g, Font font, LocalPlayer player, int cx, int cy,
                                         int immersiveHalfHeight, float scale, int structColor, int baseAlpha) {
        int spineX = cx + scaled(BASE_ALTITUDE_TAPE_OFFSET_X, scale);
        double pxPerUnit = BASE_ALTITUDE_PX_PER_UNIT * scale;
        double altitude = FlightMath.barometricAltitude(player);
        drawVerticalTape(g, font, spineX, cy, altitude, ALTITUDE_STEP, pxPerUnit,
                immersiveHalfHeight, true, true, scale, structColor, baseAlpha);

        String baltReadout = Long.toString(Math.round(altitude));
        drawValueBox(g, font, spineX, cy, baltReadout, structColor, baseAlpha);

        // RALT sits at the bottom of the immersive HUD extent, not directly
        // under BALT (see class doc) — a fixed instrument, clear of BALT's
        // own scrolling tick labels.
        FlightMath.GroundReading ralt = FlightMath.radarAltitude(player);
        String raltReadout = ralt.raycastHit() ? "R " + Math.round(ralt.distance()) : "R ---";
        int raltY = cy + immersiveHalfHeight - scaled(BASE_RALT_BOTTOM_MARGIN, scale);
        drawValueBox(g, font, spineX, raltY, raltReadout, structColor, baseAlpha);
    }

    /**
     * A moving vertical scale (airspeed or altitude tape): ticks/labels at
     * multiples of {@code step} scroll past a fixed spine as {@code
     * currentValue} changes, fading via {@link #verticalFadeAlpha} near the
     * immersive-height limit. {@code labelsToRight} controls which side of
     * the spine the ticks and labels extend toward (away from screen center:
     * true for the right-side altitude tape, false for the left-side airspeed
     * tape). A gap around dy=0 — matching the boxed current-value readout's
     * own footprint (drawn separately by the caller) — is left clear of both
     * ticks and the spine itself, so the spine visibly terminates at the box
     * rather than passing behind it.
     */
    private static void drawVerticalTape(GuiGraphics g, Font font, int spineX, int cy, double currentValue,
                                         double step, double pxPerUnit, int immersiveHalfHeight,
                                         boolean labelsToRight, boolean allowNegative, float scale, int rgb, int baseAlpha) {
        int windowUnits = (int) Math.ceil(immersiveHalfHeight / pxPerUnit) + (int) step;
        double firstTick = Math.floor((currentValue - windowUnits) / step) * step;
        int tickLen = scaled(6, scale);
        // Matches drawValueBox's own half-height (font.lineHeight/2 + padY=2)
        // plus a small buffer, so the box drawn by the caller at (spineX, cy)
        // lines up with the gap cut here.
        int boxHalfHeight = font.lineHeight / 2 + 4;

        for (double tickValue = firstTick; tickValue <= currentValue + windowUnits; tickValue += step) {
            if (!allowNegative && tickValue < 0) {
                continue;
            }
            int y = cy - (int) Math.round((tickValue - currentValue) * pxPerUnit);
            int dy = Math.abs(y - cy);
            if (dy < boxHalfHeight) {
                continue;
            }
            int alpha = FlightColors.combineAlpha(verticalFadeAlpha(dy, immersiveHalfHeight), baseAlpha);
            if (alpha == 0) {
                continue;
            }
            int argb = FlightColors.withAlpha(rgb, alpha);

            String label = Long.toString(Math.round(tickValue));
            int textY = y - font.lineHeight / 2;
            if (labelsToRight) {
                hSeg(g, spineX, spineX + tickLen, y, argb);
                g.drawString(font, label, spineX + tickLen + 3, textY, argb, false);
            } else {
                hSeg(g, spineX - tickLen, spineX, y, argb);
                g.drawString(font, label, spineX - tickLen - 3 - font.width(label), textY, argb, false);
            }
        }

        // Spine drawn solid within the non-faded inner zone, split around the
        // boxed-readout gap so it terminates at the box instead of passing
        // behind it.
        int innerHalf = Math.max(0, immersiveHalfHeight - FADE_BAND_PX);
        int spineArgb = FlightColors.withAlpha(rgb, baseAlpha);
        if (boxHalfHeight < innerHalf) {
            vSeg(g, spineX, cy - innerHalf, cy - boxHalfHeight, spineArgb);
            vSeg(g, spineX, cy + boxHalfHeight, cy + innerHalf, spineArgb);
        }
    }

    private static void drawValueBox(GuiGraphics g, Font font, int centerX, int centerY, String text, int rgb, int baseAlpha) {
        int textWidth = font.width(text);
        int padX = 3;
        int padY = 2;
        int left = centerX - textWidth / 2 - padX;
        int right = centerX + textWidth / 2 + padX;
        int top = centerY - font.lineHeight / 2 - padY;
        int bottom = centerY + font.lineHeight / 2 + padY;
        int argb = FlightColors.withAlpha(rgb, baseAlpha);
        int backdropAlpha = FlightColors.combineAlpha(0x80, baseAlpha);

        g.fill(left, top, right, bottom, FlightColors.withAlpha(0x000000, backdropAlpha));
        hSeg(g, left, right, top, argb);
        hSeg(g, left, right, bottom, argb);
        vSeg(g, left, top, bottom, argb);
        vSeg(g, right, top, bottom, argb);
        g.drawString(font, text, centerX - textWidth / 2, centerY - font.lineHeight / 2, argb, false);
    }

    // ── Low-level primitives ──────────────────────────────────────────────

    /** Rounds a base (scale-1.0) pixel constant by the configured instrument scale. */
    private static int scaled(int base, float scale) {
        return Math.round(base * scale);
    }

    /**
     * Alpha (0-255) for a vertically-tracking element at distance {@code dy}
     * from center against the immersive-HUD-height fade: full opacity inside
     * {@code halfHeight - FADE_BAND_PX}, linearly fading to 0 at {@code
     * halfHeight}. Composed with the user's configured instrument alpha via
     * {@link FlightColors#combineAlpha} at each call site — this value alone
     * is only the fade's contribution.
     */
    private static int verticalFadeAlpha(int dy, int halfHeight) {
        if (dy >= halfHeight) {
            return 0;
        }
        if (dy <= halfHeight - FADE_BAND_PX) {
            return 255;
        }
        float t = (halfHeight - dy) / (float) FADE_BAND_PX;
        return Math.round(255 * FlightColors.clamp01(t));
    }

    /**
     * 1px horizontal segment [x1, x2] at row y, in a pre-composed ARGB color
     * (see class doc). Both endpoints inclusive — {@code GuiGraphics#fill}'s
     * upper x bound is exclusive, so this adds 1 to reach x2 itself; without
     * it, closed 4-sided shapes built from hSeg+vSeg (the FPM ring,
     * drawValueBox's border) drop their bottom-right corner pixel, since
     * neither the bottom hSeg nor the right vSeg's exclusive ends cover it.
     */
    private static void hSeg(GuiGraphics g, int x1, int x2, int y, int argb) {
        g.fill(x1, y, x2 + 1, y + 1, argb);
    }

    /** 1px vertical segment [y1, y2] at column x, in a pre-composed ARGB color. Both endpoints inclusive — see hSeg. */
    private static void vSeg(GuiGraphics g, int x, int y1, int y2, int argb) {
        g.fill(x, y1, x + 1, y2 + 1, argb);
    }

    /** Dashed horizontal segment (dive rungs) — 4px on, 3px off. */
    private static void dashedHSeg(GuiGraphics g, int x1, int x2, int y, int argb) {
        for (int x = x1; x < x2; x += 7) {
            hSeg(g, x, Math.min(x + 4, x2), y, argb);
        }
    }
}
