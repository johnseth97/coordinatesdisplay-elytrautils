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
 * a fixed boresight (nose reference), and an angle-of-attack indexer.
 *
 * <p>Like {@link MasterCautionOverlay}, this is its own {@code
 * HudElementRegistry} layer, NOT part of CoordinatesDisplay's corner HUD
 * block: it's screen-space custom drawing (lines, ticks, shapes) centered on
 * the crosshair, not text rows in CD's layout, so it doesn't belong in the
 * {@code HudMixin}. It draws with {@code GuiGraphics#fill} rectangles; every
 * color goes through {@link FlightColors#opaque} because a zero alpha byte
 * makes a draw call silently no-op (see agents.md).
 *
 * <h2>Angle conventions</h2>
 * All angles use Minecraft's pitch convention — <b>positive = nose DOWN</b>
 * ({@link LocalPlayer#getXRot()}). The pitch ladder is labeled the way pilots
 * read one, in <i>elevation</i> (positive = climb, i.e. {@code -pitch}). The
 * ladder is fixed to the horizon and the boresight/FPM float against it,
 * mirroring a real HUD.
 *
 * <p><b>Signs to confirm in flight.</b> The vertical geometry (a rung for
 * elevation {@code e} sits at {@code cy - (e + pitch) * pxPerDeg}) was derived
 * and checked against concrete cases, so the ladder and FPM vertical placement
 * are self-consistent. But three things genuinely can't be settled without
 * flying and are called out at their use sites: (1) the flight-path-angle sign
 * from {@code atan2(-vy, h)}, (2) the lateral FPM drift sign, and (3) the AoA
 * display sign. Each is isolated so flipping one is a one-line change.
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
    private static final float LADDER_WINDOW_FRACTION = 0.42f; // of screen height, each side of center

    // Reference marks, grounded in the derived constants (not eyeballed):
    // ideal rocket climb and the dive-caution threshold, both converted from
    // pitch (positive-down) to ladder elevation (positive-up).
    private static final float IDEAL_CLIMB_ELEVATION = -FlightConstants.IDEAL_CLIMB_PITCH; // +34
    private static final float DIVE_CAUTION_ELEVATION = -FlightConstants.GLIDE_RED_PITCH;  // -30

    private static final int COLOR_HORIZON = 0xFFFFFF;
    private static final int COLOR_LADDER = 0xC0C0C0;
    private static final int COLOR_BORESIGHT = 0xFFFF55;

    // AoA indexer geometry: a fixed vertical tape left of center.
    private static final int AOA_TAPE_OFFSET_X = 118;
    private static final int AOA_TAPE_HALF_HEIGHT = 40;
    private static final float AOA_DISPLAY_SPAN = 15f; // degrees mapped over the tape half-height

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
        int stateColor = FlightColors.flightStateColor(pitch, vy, horizontalSpeed, true);

        if (config.showPitchLadder) {
            drawPitchLadder(graphics, client.font, cx, cy, pitch, pxPerDeg);
        }
        drawBoresight(graphics, cx, cy);
        if (config.showFlightPathMarker) {
            drawFlightPathMarker(graphics, cx, cy, aoa, yaw, velocity, horizontalSpeed, pxPerDeg, stateColor);
        }
        if (config.showAoaIndicator) {
            drawAoaIndicator(graphics, client.font, cx, cy, pitch, gamma, horizontalSpeed, stateColor);
        }
    }

    // ── Pitch ladder ──────────────────────────────────────────────────────

    private static void drawPitchLadder(GuiGraphics g, Font font, int cx, int cy, float pitch, double pxPerDeg) {
        int windowHalf = Math.round(g.guiHeight() * LADDER_WINDOW_FRACTION);

        for (int elevation = -LADDER_MAX_ELEVATION; elevation <= LADDER_MAX_ELEVATION; elevation += RUNG_STEP_DEGREES) {
            // A rung for elevation e sits (e - noseElevation) degrees above the
            // nose, where noseElevation = -pitch; screen-y grows downward, so
            // being above center subtracts. → cy - (e + pitch) * pxPerDeg.
            int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
            if (Math.abs(y - cy) > windowHalf) {
                continue;
            }

            if (elevation == 0) {
                drawHorizonLine(g, cx, y);
                continue;
            }
            boolean climb = elevation > 0;
            drawRung(g, cx, y, climb, COLOR_LADDER);
            drawRungLabel(g, font, cx, y, Math.abs(elevation), COLOR_LADDER);
        }

        // Derived-constant reference brackets, drawn on top of the grid.
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, windowHalf, IDEAL_CLIMB_ELEVATION, FlightColors.COLOR_GREEN);
        drawReferenceMark(g, cx, cy, pitch, pxPerDeg, windowHalf, DIVE_CAUTION_ELEVATION, FlightColors.COLOR_RED);
    }

    private static void drawHorizonLine(GuiGraphics g, int cx, int y) {
        hSeg(g, cx - HORIZON_HALF_WIDTH, cx - RUNG_GAP, y, COLOR_HORIZON);
        hSeg(g, cx + RUNG_GAP, cx + HORIZON_HALF_WIDTH, y, COLOR_HORIZON);
        // Small downward end caps mark the true horizon ends.
        vSeg(g, cx - HORIZON_HALF_WIDTH, y, y + 4, COLOR_HORIZON);
        vSeg(g, cx + HORIZON_HALF_WIDTH, y, y + 4, COLOR_HORIZON);
    }

    /** A climb rung is solid with ticks pointing down toward the horizon; a dive rung is dashed with ticks up. */
    private static void drawRung(GuiGraphics g, int cx, int y, boolean climb, int rgb) {
        int leftInner = cx - RUNG_GAP;
        int rightInner = cx + RUNG_GAP;
        int leftOuter = leftInner - RUNG_LENGTH;
        int rightOuter = rightInner + RUNG_LENGTH;

        if (climb) {
            hSeg(g, leftOuter, leftInner, y, rgb);
            hSeg(g, rightInner, rightOuter, y, rgb);
            // End ticks point toward the horizon (down for climb rungs).
            vSeg(g, leftInner, y, y + 4, rgb);
            vSeg(g, rightInner, y, y + 4, rgb);
        } else {
            dashedHSeg(g, leftOuter, leftInner, y, rgb);
            dashedHSeg(g, rightInner, rightOuter, y, rgb);
            vSeg(g, leftInner, y - 4, y, rgb);
            vSeg(g, rightInner, y - 4, y, rgb);
        }
    }

    private static void drawRungLabel(GuiGraphics g, Font font, int cx, int y, int elevationMagnitude, int rgb) {
        String label = Integer.toString(elevationMagnitude);
        int textY = y - font.lineHeight / 2;
        int color = FlightColors.opaque(rgb);
        // Labels just outside each rung's outer end.
        g.drawString(font, label, cx - RUNG_GAP - RUNG_LENGTH - 4 - font.width(label), textY, color, false);
        g.drawString(font, label, cx + RUNG_GAP + RUNG_LENGTH + 4, textY, color, false);
    }

    private static void drawReferenceMark(GuiGraphics g, int cx, int cy, float pitch, double pxPerDeg,
                                          int windowHalf, float elevation, int rgb) {
        int y = cy - (int) Math.round((elevation + pitch) * pxPerDeg);
        if (Math.abs(y - cy) > windowHalf) {
            return;
        }
        // A short colored bar bridging the center gap: an unmistakable target
        // line at a physics-derived elevation (ideal climb / dive caution).
        hSeg(g, cx - RUNG_GAP + 2, cx + RUNG_GAP - 2, y, rgb);
        hSeg(g, cx - RUNG_GAP + 2, cx + RUNG_GAP - 2, y - 1, rgb);
    }

    // ── Boresight (fixed nose reference) ──────────────────────────────────

    private static void drawBoresight(GuiGraphics g, int cx, int cy) {
        // Waterline symbol: two short wings flanking a center pip. The gap
        // between this (where the nose points) and the FPM (where you're
        // actually going) is the angle of attack, read directly off screen.
        hSeg(g, cx - 18, cx - 6, cy, COLOR_BORESIGHT);
        hSeg(g, cx + 6, cx + 18, cy, COLOR_BORESIGHT);
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, FlightColors.opaque(COLOR_BORESIGHT));
    }

    // ── Flight path marker (velocity vector) ──────────────────────────────

    private static void drawFlightPathMarker(GuiGraphics g, int cx, int cy, double aoa, float yaw,
                                             Vec3 velocity, double horizontalSpeed, double pxPerDeg, int rgb) {
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
        // Keep the marker on screen even in extreme states.
        fx = Mth.clamp(fx, cx - HORIZON_HALF_WIDTH, cx + HORIZON_HALF_WIDTH);
        fy = Mth.clamp(fy, cy - g.guiHeight() / 2 + 12, cy + g.guiHeight() / 2 - 12);

        int r = 4;
        // Ring (square outline approximating the FPM circle).
        hSeg(g, fx - r, fx + r, fy - r, rgb);
        hSeg(g, fx - r, fx + r, fy + r, rgb);
        vSeg(g, fx - r, fy - r, fy + r, rgb);
        vSeg(g, fx + r, fy - r, fy + r, rgb);
        // Wings and top stub.
        hSeg(g, fx - r - 8, fx - r, fy, rgb);
        hSeg(g, fx + r, fx + r + 8, fy, rgb);
        vSeg(g, fx, fy - r - 6, fy - r, rgb);
    }

    // ── Angle-of-attack indexer ───────────────────────────────────────────

    private static void drawAoaIndicator(GuiGraphics g, Font font, int cx, int cy, float pitch, double gamma,
                                         double horizontalSpeed, int rgb) {
        int ax = cx - AOA_TAPE_OFFSET_X;
        int top = cy - AOA_TAPE_HALF_HEIGHT;
        int bottom = cy + AOA_TAPE_HALF_HEIGHT;
        float pxPerDeg = AOA_TAPE_HALF_HEIGHT / AOA_DISPLAY_SPAN;

        // Vertical scale.
        vSeg(g, ax, top, bottom, COLOR_LADDER);
        for (int tick = -15; tick <= 15; tick += 5) {
            int ty = cy - Math.round(tick * pxPerDeg);
            int len = tick == 0 ? 7 : 4; // emphasize the 0 datum (nose aligned with velocity)
            hSeg(g, ax - len, ax, ty, COLOR_LADDER);
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

    // ── Low-level primitives ──────────────────────────────────────────────

    /** 1px horizontal segment [x1, x2] at row y. Alpha forced opaque (zero-alpha draws no-op). */
    private static void hSeg(GuiGraphics g, int x1, int x2, int y, int rgb) {
        g.fill(x1, y, x2, y + 1, FlightColors.opaque(rgb));
    }

    /** 1px vertical segment [y1, y2] at column x. */
    private static void vSeg(GuiGraphics g, int x, int y1, int y2, int rgb) {
        g.fill(x, y1, x + 1, y2, FlightColors.opaque(rgb));
    }

    /** Dashed horizontal segment (dive rungs) — 4px on, 3px off. */
    private static void dashedHSeg(GuiGraphics g, int x1, int x2, int y, int rgb) {
        for (int x = x1; x < x2; x += 7) {
            hSeg(g, x, Math.min(x + 4, x2), y, rgb);
        }
    }
}
