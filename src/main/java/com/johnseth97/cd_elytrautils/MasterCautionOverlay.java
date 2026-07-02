package com.johnseth97.cd_elytrautils;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

/**
 * Screen-centered warning shown when the elytra will break (durability)
 * before the player reaches the ground at their current glide slope —
 * breaking mid-flight means falling to your death. See GitHub issue #7.
 *
 * Renders as its own HUD layer via Fabric API's HudElementRegistry rather
 * than the CoordinatesDisplay mixin used elsewhere in this mod: this is a
 * screen-centered alert, not part of CD's corner-anchored HUD block, so it
 * doesn't belong in that layout.
 */
public final class MasterCautionOverlay {

    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(
            CoordinatesDisplayElytraUtils.MOD_ID, "master_caution");

    private static final String WARNING_TEXT = "ELYTRA DESTRUCTION IMMINENT";

    // Epilepsy-safe flash rate: 1 Hz (500ms on / 500ms off), well outside the
    // 3-30 Hz range associated with photosensitive seizure risk.
    private static final long FLASH_PERIOD_MS = 1000;

    private MasterCautionOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(LAYER_ID, MasterCautionOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay || !player.isFallFlying()) {
            return;
        }

        double vy = player.getDeltaMovement().y;

        // Bingo-fuel comparison: ticks until the ground vs. ticks until the
        // elytra could break. Neither depends on horizontal speed, so a
        // transient rocket-boost speed spike can't corrupt this the way
        // multiplying a long time window by instantaneous speed did before
        // (see FlightMath's class doc).
        double ticksToGround = FlightMath.ticksToGround(player, vy);
        double ticksUntilDurabilityFloor = FlightMath.durabilityLimitedTicks(player);
        if (ticksToGround < 0.0 || ticksUntilDurabilityFloor < 0.0 || ticksUntilDurabilityFloor >= ticksToGround) {
            return;
        }

        if (System.currentTimeMillis() % FLASH_PERIOD_MS >= FLASH_PERIOD_MS / 2) {
            return;
        }

        // Config stores RGB only (no alpha) — see ElytraUtilsConfig — so the
        // user can't reintroduce the zero-alpha bug that silently no-op'd
        // every draw call before it was fixed. Style carries the actual
        // color; the trailing draw-call color is always fully opaque so the
        // style's color shows through (matches HudMixin's coloredText usage).
        int configuredColor = CoordinatesDisplayElytraUtils.getConfig().masterCautionColor;
        Component warning = Component.literal(WARNING_TEXT)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(configuredColor)).withBold(true));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 - 40;
        graphics.drawCenteredString(client.font, warning, centerX, centerY, 0xFFFFFFFF);
    }
}
