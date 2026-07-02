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
import net.minecraft.world.phys.Vec3;

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

    private MasterCautionOverlay() {
    }

    public static void register() {
        HudElementRegistry.addLast(LAYER_ID, MasterCautionOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        if (player == null || !config.showElytraOverlay || !player.isFallFlying() || config.masterCautionThresholdBlocks <= 0.0) {
            return;
        }

        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        // Bingo-fuel comparison: ticks until the ground vs. ticks until the
        // elytra could break. Neither depends on horizontal speed, so a
        // transient rocket-boost speed spike can't corrupt this the way
        // multiplying a long time window by instantaneous speed did before
        // (see FlightMath's class doc).
        double ticksToGround = FlightMath.ticksToGround(player, vy);
        double ticksUntilDurabilityFloor = FlightMath.durabilityLimitedTicks(player);
        if (ticksToGround < 0.0 || ticksUntilDurabilityFloor < 0.0) {
            return;
        }

        // masterCautionThresholdBlocks converts to a tick margin using
        // current speed, giving earlier warning lead time than the exact
        // crossover. A brief rocket-boost speed spike only ever narrows this
        // margin (dividing by a larger number) — the exact crossover itself
        // (ticksUntilDurabilityFloor vs ticksToGround, margin-free) never
        // depends on speed, so a boost can't cause a missed warning at the
        // actual critical point, only a temporarily shorter lead time.
        double marginTicks = horizontalSpeed > 0.001 ? config.masterCautionThresholdBlocks / horizontalSpeed : 0.0;
        if (ticksUntilDurabilityFloor >= ticksToGround + marginTicks) {
            return;
        }

        if (!FlightColors.isFlashOn()) {
            return;
        }

        // Config stores RGB only (no alpha) — see ElytraUtilsConfig — so the
        // user can't reintroduce the zero-alpha bug that silently no-op'd
        // every draw call before it was fixed. Style carries the actual
        // color; the trailing draw-call color is always fully opaque so the
        // style's color shows through (matches HudMixin's coloredText usage).
        int configuredColor = config.masterCautionColor;
        Component warning = Component.literal(WARNING_TEXT)
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(configuredColor)).withBold(true));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 - 40;
        graphics.drawCenteredString(client.font, warning, centerX, centerY, 0xFFFFFFFF);
    }
}
