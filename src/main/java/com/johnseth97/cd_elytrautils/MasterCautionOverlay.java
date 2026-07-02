package com.johnseth97.cd_elytrautils;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double vy = velocity.y;

        double groundDistance = FlightMath.glideRangeBlocks(player, horizontalSpeed, vy);
        double durabilityDistance = FlightMath.durabilityLimitedBlocks(player, horizontalSpeed);
        if (groundDistance < 0.0 || durabilityDistance < 0.0 || durabilityDistance >= groundDistance) {
            return;
        }

        if (System.currentTimeMillis() % FLASH_PERIOD_MS >= FLASH_PERIOD_MS / 2) {
            return;
        }

        Component warning = Component.literal(WARNING_TEXT).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 - 40;
        graphics.drawCenteredString(client.font, warning, centerX, centerY, 0xFFFFFF);
    }
}
