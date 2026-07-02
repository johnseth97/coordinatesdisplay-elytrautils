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
 * is passed through {@link FlightColors#opaque} or {@link
 * FlightColors#withAlpha} because a zero alpha byte makes a draw call
 * silently no-op (see agents.md) — {@code hSeg}/{@code vSeg} below take a
 * fully-composed ARGB int for exactly that reason, so alpha is never an
 * afterthought at a call site.
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
 * (pitch ladder, airspeed/altitude tapes) fade to transparent over {@link
 * #FADE_BAND_PX} as they approach that half-height from center, via {@link
 * #verticalFadeAlpha}, instead of hard-clipping or tracking the full screen.
 * The bearing tape and the fixed boxed readouts are not subject to this fade
 * — they sit at a fixed position near center/top rather than tracking.
 */
public final class FlightInstrumentOverlay {

    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            CoordinatesDisplayElytraUtils.MOD_ID, "flight_instruments");

    // Ladder geometry (screen pixels). The rung gap keeps the middle clear so
    // the boresight and FPM read cleanly against the ladder.
    private static final int RUNG_GAP = 26;        // half-width of the clear center gap
    private static final int RUNG_LENGTH = 34;     // length of each side rung segment
    private static final int HORIZON_HALF_WIDTH = 90;
    private static final int RUNG_STEP_DEGREES = 10;
    private static final int LADDER_MAX_ELEVATION = 80;

    // Reference marks, grounded in the derived constants (not eyeballed):
    // ideal rocket climb and the dive-caution threshold, both converted from
    // pitch (positive-down) to ladder elevation (positive-up).
    private static final float IDEAL_CLIMB_ELEVATION = -FlightConstants.IDEAL_CLIMB_PITCH; // +34
    private static final float DIVE_CAUTION_ELEVATION = -FlightConstants.GLIDE_RED_PITCH;  // -30

    private static final int COLOR_HORIZON = 0xFFFFFF;
    private static final int COLOR_LADDER = 0xC0C0C0;
    private static final int COLOR_BORESIGHT = 0xFFFF55;

    // AoA indexer geometry: a fixed vertical tape left of center. Small and
    // fixed-height, so it's not "tracking" the way the ladder/airspeed/
    // altitude tapes are — no immersive-height fade applied.
    private static final int AOA_TAPE_OFFSET_X = 118;
    private static final int AOA_TAPE_HALF_HEIGHT = 40;
    private static final float AOA_DISPLAY_SPAN = 15f; // degrees mapped over the tape half-height

    // Immersive-HUD-height fade band: vertically-tracking elements fade to
    // zero alpha over this many pixels as they approach the configured
    // half-height from center, rather than hard-clipping.
    private static final int FADE_BAND_PX = 24;

    // Airspeed tape (left). Displayed in blocks/second (horizontalSpeed * 20)
    // rather than raw blocks/tick, since that reads as a much more HUD-scale
    // "airspeed" number (elytra cruise is roughly 8-12, boosted well past 40).
    private static final int AIRSPEED_TAPE_OFFSET_X = 150;
    private static final double AIRSPEED_STEP = 10.0;
    private static final double AIRSPEED_PX_PER_UNIT = 3.0;

    // Altitude tape (right) — BALT, from FlightMath.barometricAltitude, plus
    // a RALT box beneath the BALT readout from FlightMath.radarAltitude.
    private static final int ALTITUDE_TAPE_OFFSET_X = 150;
    private static final double ALTITUDE_STEP = 50.0;
    private static final double ALTITUDE_PX_PER_UNIT = 1.0;
    private static final int RALT_BOX_Y_OFFSET = 26;

    // Bearing tape (top). Not immersive-height-faded — it's a fixed row near
    // the top, not a vertically-tracking element.
    private static final int BEARING_TAPE_Y = 34;
    private static final double BEARING_STEP = 30.0;
    private static final double BEARING_PX_PER_DEGREE = 3.0;
    private static final int BEARING_TAPE_HALF_WIDTH = 100;

    private FlightInstrumentOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(LAYER_ID, FlightInstrumentOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        if (player == null || !config.showElytraOverlay || !config.showFlightInstruments
                || !player.isFallFlying()) {
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

        double pxPerDeg = config.flightInstrumentPixelsPerDegree;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int immersiveHalfHeight = (int) Math.round(config.immersiveHudHeightPixels);
        int stateColor = FlightColors.flightStateColor(pitch, vy, horizontalSpeed, true);
        Font font = client.font;

        if (config.showPitchLadder) {
            drawPitchLadder(graphics, font, cx, cy, pitch, pxPerDeg, immersiveHalfHeight);
        }
        drawBoresight(graphics, cx, cy);
        if (config.showFlightPathMarker) {
            drawFlightPathMarker(graphics, cx, cy, aoa, yaw, velocity, horizontalSpeed, pxPerDeg,
                    immersiveHalfHeight, stateColor);
        }
        if (config.showAoaIndicator) {
            drawAoaIndicator(graphics, font, cx, cy, pitch, gamma, horizontalSpeed, stateColor);
        }
        if (config.showBearingTape) {
            drawBearingTape(graphics, font, cx, yaw);
        }
        if (config.showAirspeedTape) {
            drawAirspeedTape(graphics, font, cx, cy, horizontalSpeed * 20.0, immersiveHalfHeight);
        }
        if (config.showAltitudeTape) {
            drawAltitudeTape(graphics, font, player, cx, cy, immersiveHalfHeight);
        }
    }

    // ── Pitch ladder ──────────────────────────────────────────────────────

    private static void drawPitchLadder(GuiGraphics g, Font font, int cx, int cy, float pitch, double pxPerDeg,
                                        int immersiveHalfHeight) {
        for (int elevation = -LADDER_MAX_ELEVATION; elevation <= LADDER_MAX_ELEVATION; elevation += RUNG_STEP_DEGREES) {
            // A rung for elevation e sits (e - noseElevation) degrees above the
            // nose, where noseElevation = -pitch; screen-y grows downward, so
            // being above center subtracts. → cy - (e + pitch) * pxPerDeg.
            int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
            int alpha = verticalFadeAlpha(Math.abs(y - cy), immersiveHalfHeight);
            if (alpha == 0) {
                continue;
            }

            if (elevation == 0) {
                drawHorizonLine(g, cx, y, alpha);
                continue;
            }
            boolean climb = elevation > 0;
            drawRung(g, cx, y, climb, COLOR_LADDER, alpha);
            drawRungLabel(g, font, cx, y, Math.abs(elevation), COLOR_LADDER, alpha);
        }

        // Derived-constant reference brackets, drawn on top of the grid.
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, immersiveHalfHeight, IDEAL_CLIMB_ELEVATION, FlightColors.COLOR_GREEN);
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, immersiveHalfHeight, DIVE_CAUTION_ELEVATION, FlightColors.COLOR_RED);
    }

    private static void drawHorizonLine(GuiGraphics g, int cx, int y, int alpha) {
        int argb = FlightColors.withAlpha(COLOR_HORIZON, alpha);
        hSeg(g, cx - HORIZON_HALF_WIDTH, cx - RUNG_GAP, y, argb);
        hSeg(g, cx + RUNG_GAP, cx + HORIZON_HALF_WIDTH, y, argb);
        // Small downward end caps mark the true horizon ends.
        vSeg(g, cx - HORIZON_HALF_WIDTH, y, y + 4, argb);
        vSeg(g, cx + HORIZON_HALF_WIDTH, y, y + 4, argb);
    }

    /** A climb rung is solid with ticks pointing down toward the horizon; a dive rung is dashed with ticks up. */
    private static void drawRung(GuiGraphics g, int cx, int y, boolean climb, int rgb, int alpha) {
        int argb = FlightColors.withAlpha(rgb, alpha);
        int leftInner = cx - RUNG_GAP;
        int rightInner = cx + RUNG_GAP;
        int leftOuter = leftInner - RUNG_LENGTH;
        int rightOuter = rightInner + RUNG_LENGTH;

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

    private static void drawRungLabel(GuiGraphics g, Font font, int cx, int y, int elevationMagnitude, int rgb, int alpha) {
        String label = Integer.toString(elevationMagnitude);
        int textY = y - font.lineHeight / 2;
        int argb = FlightColors.withAlpha(rgb, alpha);
        // Labels just outside each rung's outer end.
        g.drawString(font, label, cx - RUNG_GAP - RUNG_LENGTH - 4 - font.width(label), textY, argb, false);
        g.drawString(font, label, cx + RUNG_GAP + RUNG_LENGTH + 4, textY, argb, false);
    }

    private static void drawReferenceMark(GuiGraphics g, int cx, int cy, float pitch, double pxPerDeg,
                                          int immersiveHalfHeight, float elevation, int rgb) {
        int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
        int alpha = verticalFadeAlpha(Math.abs(y - cy), immersiveHalfHeight);
        if (alpha == 0) {
            return;
        }
        int argb = FlightColors.withAlpha(rgb, alpha);
        // A short colored bar bridging the center gap: an unmistakable target
        // line at a physics-derived elevation (ideal climb / dive caution).
        hSeg(g, cx - RUNG_GAP + 2, cx + RUNG_GAP - 2, y, argb);
        hSeg(g, cx - RUNG_GAP + 2, cx + RUNG_GAP - 2, y - 1, argb);
    }

    // ── Boresight (fixed nose reference) ──────────────────────────────────

    private static void drawBoresight(GuiGraphics g, int cx, int cy) {
        // Waterline symbol: two short wings flanking a center pip. The gap
        // between this (where the nose points) and the FPM (where you're
        // actually going) is the angle of attack, read directly off screen.
        // Fixed at center, so never subject to the immersive-height fade.
        int argb = FlightColors.opaque(COLOR_BORESIGHT);
        hSeg(g, cx - 18, cx - 6, cy, argb);
        hSeg(g, cx + 6, cx + 18, cy, argb);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, argb);
    }

    // ── Flight path marker (velocity vector) ──────────────────────────────

    private static void drawFlightPathMarker(GuiGraphics g, int cx, int cy, double aoa, float yaw,
                                             Vec3 velocity, double horizontalSpeed, double pxPerDeg,
                                             int immersiveHalfHeight, int rgb) {
        // Vertical: the FPM sits below/above the boresight by the AoA, since
        // its elevation minus the nose's equals (pitch − γ) = aoa.
        int fy = cy - (int) Math.round(aoa * pxPerDeg);

        // Lateral: drift between where the nose points (yaw) and where the
        // velocity vector actually points. SIGN TO VERIFY (2): atan2(-vx, vz)
        // matches Minecraft's yaw convention (forward = (-sin y, cos y)).
        int fx = cx;
        if (horizontalSpeed > 0.05) {
            double velocityYaw = Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
            double drift = Mth.wrapDegrees(velocityYaw - yaw);
            fx = cx + (int) Math.round(drift * pxPerDeg);
        }
        fx = Mth.clamp(fx, cx - HORIZON_HALF_WIDTH, cx + HORIZON_HALF_WIDTH);
        fy = Mth.clamp(fy, cy - immersiveHalfHeight, cy + immersiveHalfHeight);

        int alpha = verticalFadeAlpha(Math.abs(fy - cy), immersiveHalfHeight);
        if (alpha == 0) {
            return;
        }
        int argb = FlightColors.withAlpha(rgb, alpha);

        int r = 4;
        // Ring (square outline approximating the FPM circle).
        hSeg(g, fx - r, fx + r, fy - r, argb);
        hSeg(g, fx - r, fx + r, fy + r, argb);
        vSeg(g, fx - r, fy - r, fy + r, argb);
        vSeg(g, fx + r, fy - r, fy + r, argb);
        // Wings and top stub.
        hSeg(g, fx - r - 8, fx - r, fy, argb);
        hSeg(g, fx + r, fx + r + 8, fy, argb);
        vSeg(g, fx, fy - r - 6, fy - r, argb);
    }

    // ── Angle-of-attack indexer ───────────────────────────────────────────

    private static void drawAoaIndicator(GuiGraphics g, Font font, int cx, int cy, float pitch, double gamma,
                                         double horizontalSpeed, int rgb) {
        int ax = cx - AOA_TAPE_OFFSET_X;
        int top = cy - AOA_TAPE_HALF_HEIGHT;
        int bottom = cy + AOA_TAPE_HALF_HEIGHT;
        float pxPerDeg = AOA_TAPE_HALF_HEIGHT / AOA_DISPLAY_SPAN;

        // Vertical scale.
        int ladderColor = FlightColors.opaque(COLOR_LADDER);
        vSeg(g, ax, top, bottom, ladderColor);
        for (int tick = -15; tick <= 15; tick += 5) {
            int ty = cy - Math.round(tick * pxPerDeg);
            int len = tick == 0 ? 7 : 4; // emphasize the 0 datum (nose aligned with velocity)
            hSeg(g, ax - len, ax, ty, ladderColor);
        }

        // Displayed AoA. SIGN TO VERIFY (3): shown in the aviation-intuitive
        // sense (positive = nose above the flight path = generating lift),
        // which is (γ − pitch), the negation of the internal "pitch − γ". The
        // caret rides up for higher displayed AoA.
        double displayAoa = gamma - pitch;
        int caretY = cy - Math.round((float) Mth.clamp(displayAoa, -AOA_DISPLAY_SPAN, AOA_DISPLAY_SPAN) * pxPerDeg);
        int caretColor = FlightColors.opaque(rgb);
        // Left-pointing caret whose apex touches the tape at the current AoA.
        for (int i = 0; i < 5; i++) {
            g.fill(ax + 2 + i, caretY - i, ax + 3 + i, caretY + i + 1, caretColor);
        }

        // Numeric readout above the tape.
        String text = String.format("AoA %.0f°", displayAoa);
        g.drawString(font, text, ax - font.width(text) + 7, top - font.lineHeight - 2, caretColor, false);

        // Stall flag, grounded in the derived airspeed floor (issue #9) rather
        // than any AoA value — elytra stall is an airspeed floor, not an angle.
        if (horizontalSpeed < FlightConstants.STALL_HORIZONTAL_SPEED) {
            String stall = "STALL";
            g.drawString(font, stall, ax - font.width(stall) + 7, bottom + 3,
                    FlightColors.opaque(FlightColors.COLOR_RED), false);
        }
    }

    // ── Bearing tape (top) ─────────────────────────────────────────────────

    private static void drawBearingTape(GuiGraphics g, Font font, int cx, float yaw) {
        // Compass heading, 0 = north: Minecraft yaw 0 is south, 180/-180 is
        // north, increasing clockwise (west at +90) — so heading = yaw + 180,
        // wrapped positive.
        float heading = Mth.positiveModulo(yaw + 180f, 360f);
        int y = BEARING_TAPE_Y;
        int ladderColor = FlightColors.opaque(COLOR_LADDER);

        int windowDegrees = (int) Math.ceil(BEARING_TAPE_HALF_WIDTH / BEARING_PX_PER_DEGREE) + (int) BEARING_STEP;
        double firstTick = Math.floor((heading - windowDegrees) / BEARING_STEP) * BEARING_STEP;
        for (double tick = firstTick; tick <= heading + windowDegrees; tick += BEARING_STEP) {
            float delta = Mth.wrapDegrees((float) (tick - heading));
            int x = cx + Math.round(delta * (float) BEARING_PX_PER_DEGREE);
            if (Math.abs(x - cx) > BEARING_TAPE_HALF_WIDTH) {
                continue;
            }

            int wrapped = (int) (((Math.round(tick) % 360) + 360) % 360);
            String label = cardinalOrDegrees(wrapped);
            vSeg(g, x, y, y + 5, ladderColor);
            g.drawString(font, label, x - font.width(label) / 2, y + 7, ladderColor, false);
        }

        hSeg(g, cx - BEARING_TAPE_HALF_WIDTH, cx + BEARING_TAPE_HALF_WIDTH, y, ladderColor);
        // Fixed index caret marking "straight ahead" at center.
        int caret = FlightColors.opaque(COLOR_BORESIGHT);
        for (int i = 0; i < 4; i++) {
            hSeg(g, cx - i, cx + i, y - 2 - i, caret);
        }

        String readout = String.format("%03d", Math.round(heading) % 360);
        drawValueBox(g, font, cx, y - 15, readout, COLOR_BORESIGHT);
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
                                         int immersiveHalfHeight) {
        int spineX = cx - AIRSPEED_TAPE_OFFSET_X;
        drawVerticalTape(g, font, spineX, cy, airspeedBlocksPerSecond, AIRSPEED_STEP, AIRSPEED_PX_PER_UNIT,
                immersiveHalfHeight, false, false, COLOR_LADDER);
        String readout = Long.toString(Math.round(airspeedBlocksPerSecond));
        drawValueBox(g, font, spineX, cy, readout, COLOR_BORESIGHT);
    }

    private static void drawAltitudeTape(GuiGraphics g, Font font, LocalPlayer player, int cx, int cy,
                                         int immersiveHalfHeight) {
        int spineX = cx + ALTITUDE_TAPE_OFFSET_X;
        double altitude = FlightMath.barometricAltitude(player);
        drawVerticalTape(g, font, spineX, cy, altitude, ALTITUDE_STEP, ALTITUDE_PX_PER_UNIT,
                immersiveHalfHeight, true, true, COLOR_LADDER);

        String baltReadout = Long.toString(Math.round(altitude));
        drawValueBox(g, font, spineX, cy, baltReadout, COLOR_BORESIGHT);

        FlightMath.GroundReading ralt = FlightMath.radarAltitude(player);
        String raltReadout = ralt.raycastHit() ? "R " + Math.round(ralt.distance()) : "R ---";
        drawValueBox(g, font, spineX, cy + RALT_BOX_Y_OFFSET, raltReadout, COLOR_LADDER);
    }

    /**
     * A moving vertical scale (airspeed or altitude tape): ticks/labels at
     * multiples of {@code step} scroll past a fixed spine as {@code
     * currentValue} changes, fading via {@link #verticalFadeAlpha} near the
     * immersive-height limit. {@code labelsToRight} controls which side of
     * the spine the ticks and labels extend toward (away from screen center:
     * true for the right-side altitude tape, false for the left-side airspeed
     * tape). A small gap around dy=0 is left clear for the boxed current-value
     * readout drawn separately by the caller, mirroring the pitch ladder's
     * center gap.
     */
    private static void drawVerticalTape(GuiGraphics g, Font font, int spineX, int cy, double currentValue,
                                         double step, double pxPerUnit, int immersiveHalfHeight,
                                         boolean labelsToRight, boolean allowNegative, int rgb) {
        int windowUnits = (int) Math.ceil(immersiveHalfHeight / pxPerUnit) + (int) step;
        double firstTick = Math.floor((currentValue - windowUnits) / step) * step;
        int tickLen = 6;
        int centerGapPx = 10;

        for (double tickValue = firstTick; tickValue <= currentValue + windowUnits; tickValue += step) {
            if (!allowNegative && tickValue < 0) {
                continue;
            }
            int y = cy - (int) Math.round((tickValue - currentValue) * pxPerUnit);
            int dy = Math.abs(y - cy);
            if (dy < centerGapPx) {
                continue;
            }
            int alpha = verticalFadeAlpha(dy, immersiveHalfHeight);
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

        // Spine drawn solid within the non-faded inner zone only; ticks keep
        // fading further out, so the spine doesn't hard-edge against the fade.
        int innerHalf = Math.max(0, immersiveHalfHeight - FADE_BAND_PX);
        vSeg(g, spineX, cy - innerHalf, cy + innerHalf, FlightColors.opaque(rgb));
    }

    private static void drawValueBox(GuiGraphics g, Font font, int centerX, int centerY, String text, int rgb) {
        int textWidth = font.width(text);
        int padX = 3;
        int padY = 2;
        int left = centerX - textWidth / 2 - padX;
        int right = centerX + textWidth / 2 + padX;
        int top = centerY - font.lineHeight / 2 - padY;
        int bottom = centerY + font.lineHeight / 2 + padY;
        int argb = FlightColors.opaque(rgb);

        g.fill(left, top, right, bottom, 0x80000000);
        hSeg(g, left, right, top, argb);
        hSeg(g, left, right, bottom, argb);
        vSeg(g, left, top, bottom, argb);
        vSeg(g, right, top, bottom, argb);
        g.drawString(font, text, centerX - textWidth / 2, centerY - font.lineHeight / 2, argb, false);
    }

    // ── Low-level primitives ──────────────────────────────────────────────

    /**
     * Alpha (0-255) for a vertically-tracking element at distance {@code dy}
     * from center against the immersive-HUD-height fade: full opacity inside
     * {@code halfHeight - FADE_BAND_PX}, linearly fading to 0 at {@code
     * halfHeight}. Alpha 0 here is a deliberate "faded fully out", not the
     * zero-alpha no-op bug — see {@link FlightColors#withAlpha}.
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

    /** 1px horizontal segment [x1, x2] at row y, in a pre-composed ARGB color (see class doc). */
    private static void hSeg(GuiGraphics g, int x1, int x2, int y, int argb) {
        g.fill(x1, y, x2, y + 1, argb);
    }

    /** 1px vertical segment [y1, y2] at column x, in a pre-composed ARGB color. */
    private static void vSeg(GuiGraphics g, int x, int y1, int y2, int argb) {
        g.fill(x, y1, x + 1, y2, argb);
    }

    /** Dashed horizontal segment (dive rungs) — 4px on, 3px off. */
    private static void dashedHSeg(GuiGraphics g, int x1, int x2, int y, int argb) {
        for (int x = x1; x < x2; x += 7) {
            hSeg(g, x, Math.min(x + 4, x2), y, argb);
        }
    }
}
