package com.johnseth97.cd_elytrautils;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Screen-centered text popup that fades out on a fixed timer, used by
 * {@link KeyBindings} to confirm a toggle hotkey registered even when the
 * corresponding overlay isn't currently visible to show the change directly
 * (e.g. pressing the text-HUD toggle while not flying and not holding
 * rockets — nothing on screen would otherwise change).
 *
 * <p>Same {@code HudElementRegistry}-layer approach as {@link
 * MasterCautionOverlay}, but the fade is baked directly into the draw
 * call's color argument rather than a {@code Style} color — this is the same
 * technique vanilla uses for its action-bar message fade ({@code
 * Gui#renderOverlayMessage}), since a {@code Style}'s {@code TextColor}
 * doesn't carry the alpha byte that actually drives blending.
 */
public final class HudToastOverlay {

    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            CoordinatesDisplayElytraUtils.MOD_ID, "hud_toast");

    private static final long HOLD_MS = 1500;
    private static final long FADE_MS = 500;

    private static String message = "";
    private static int rgbColor = 0xFFFFFF;
    private static long shownAtMs = -1;

    private HudToastOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(LAYER_ID, HudToastOverlay::render);
    }

    /** Shows (or replaces) the toast in white, resetting its hold/fade timer. */
    public static void show(String text) {
        show(text, 0xFFFFFF);
    }

    /** Shows (or replaces) the toast in the given RGB color, resetting its hold/fade timer. */
    public static void show(String text, int rgbColor) {
        message = text;
        HudToastOverlay.rgbColor = rgbColor;
        shownAtMs = System.currentTimeMillis();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (shownAtMs < 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - shownAtMs;
        if (elapsed >= HOLD_MS + FADE_MS) {
            shownAtMs = -1;
            return;
        }

        int alpha = elapsed < HOLD_MS
                ? 255
                : Math.round(255 * (1f - (float) (elapsed - HOLD_MS) / FADE_MS));
        if (alpha <= 0) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int centerX = graphics.guiWidth() / 2;
        // Vanilla's own action-bar message Y offset — unobtrusive, and clear
        // of both the crosshair-centered flight instruments and Master
        // Caution's banner (guiHeight()/2 - 40).
        int y = graphics.guiHeight() - 68;
        graphics.drawCenteredString(client.font, Component.literal(message), centerX, y,
                FlightColors.withAlpha(rgbColor, alpha));
    }
}
