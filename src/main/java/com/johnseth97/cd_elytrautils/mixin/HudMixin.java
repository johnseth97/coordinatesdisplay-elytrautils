package com.johnseth97.cd_elytrautils.mixin;

import com.johnseth97.cd_elytrautils.CoordinatesDisplayElytraUtils;
import dev.boxadactle.boxlib.layouts.RenderingLayout;
import dev.boxadactle.boxlib.layouts.component.LayoutContainerComponent;
import dev.boxadactle.boxlib.layouts.component.ParagraphComponent;
import dev.boxadactle.boxlib.layouts.layout.ColumnLayout;
import dev.boxadactle.boxlib.math.geometry.Rect;
import dev.boxadactle.coordinatesdisplay.Hud;
import dev.boxadactle.coordinatesdisplay.position.Position;
import dev.boxadactle.coordinatesdisplay.registry.DisplayMode;
import dev.boxadactle.coordinatesdisplay.registry.StartCorner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends an elytra telemetry row next to CoordinatesDisplay's HUD.
 *
 * Injects at {@code RETURN} of {@link Hud#preRender}, after CoordinatesDisplay
 * has already resolved the layout's on-screen anchor position from its own
 * (un-widened) size. The original layout is pinned at that resolved position
 * and the elytra row is stacked next to it — it must never feed back into the
 * anchor math, or corner-anchored HUDs visibly jump every time flight state
 * toggles (the row's height would change the size the anchor is computed from).
 */
@Mixin(value = Hud.class, remap = false)
public abstract class HudMixin {

    @Inject(method = "preRender", at = @At("RETURN"), cancellable = true)
    private void cd_elytrautils$appendElytraRow(
            Hud.RenderType thread, Position pos, int x, int y, DisplayMode renderMode, StartCorner startCorner,
            CallbackInfoReturnable<RenderingLayout> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !shouldShowOverlay(player)) {
            return;
        }

        RenderingLayout original = cir.getReturnValue();
        Rect<Integer> rect = original.calculateRect();

        boolean growUpward = startCorner == StartCorner.BOTTOM_LEFT
                || startCorner == StartCorner.BOTTOM_RIGHT
                || startCorner == StartCorner.BOTTOM;

        ParagraphComponent rowComponent = new ParagraphComponent(0, buildElytraRow(player));
        ColumnLayout wrapper = new ColumnLayout(rect.getX(), rect.getY(), 2);
        if (growUpward) {
            // Keep the original block's bottom edge fixed: shift the wrapper
            // up by the row's height (+ the same padding ColumnLayout uses
            // between components) so the original still renders at rect.getY().
            wrapper.addComponent(rowComponent);
            wrapper.addComponent(new LayoutContainerComponent(original));
            wrapper.setPosition(rect.getX(), rect.getY() - rowComponent.getHeight() - 4);
        } else {
            wrapper.addComponent(new LayoutContainerComponent(original));
            wrapper.addComponent(rowComponent);
        }

        cir.setReturnValue(wrapper);
    }

    // Bucket boundaries and gradient targets below are derived by decompiling
    // the actual vanilla physics (LivingEntity#updateFallFlyingMovement,
    // FireworkRocketEntity#tick's boost term against an attached fall-flying
    // entity) and numerically simulating them, rather than assumed — see
    // GitHub issues #2 and #3 for the full derivation and simulation method.

    // STALL / CLIMB / GLIDE / APPROACH / DIVE bucket edges.
    private static final float STALL_PITCH = -45f;
    private static final float CLIMB_UPPER_PITCH = -8f;
    private static final float GLIDE_RED_PITCH = 30f;
    private static final float APPROACH_UPPER_PITCH = 45f;

    // Ideal pitch to hold during a rocket's burn to maximize peak altitude
    // gained from that single burst (simulated: boost formula + burn duration
    // + post-burn glide, scanned across pitch). -34 deg sits just past the
    // -30 deg stall-onset threshold — the best climb angle is deliberately
    // right at the edge of losing airspeed, not comfortably inside it.
    private static final float IDEAL_CLIMB_PITCH = -34f;
    private static final float CLIMB_GRADIENT_HALF_WIDTH = 25f;

    // Ideal pitch for maximum glide ratio (distance per altitude lost) is
    // dead level; the gradient's speed-optimal end is the pitch nearest the
    // maximum simulated steady-state horizontal speed (~+53 deg), trimmed to
    // +30 deg since that's already ~93% of max speed and more likely to be
    // where players actually fly.
    private static final float IDEAL_GLIDE_PITCH = 0f;

    private static final float ARROW_TOLERANCE = 3f;

    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_ORANGE = 0xFFAA00;
    private static final int COLOR_GREEN = 0x55FF55;

    private static boolean shouldShowOverlay(LocalPlayer player) {
        if (!CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay) {
            return false;
        }
        if (player.isFallFlying()) {
            return true;
        }
        boolean hasElytraEquipped = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        boolean holdingRockets = player.getMainHandItem().is(Items.FIREWORK_ROCKET)
                || player.getOffhandItem().is(Items.FIREWORK_ROCKET);
        return hasElytraEquipped && holdingRockets;
    }

    private static Component buildElytraRow(LocalPlayer player) {
        float pitch = player.getXRot();
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        Component status;
        if (vy < -0.45) {
            status = coloredText("LAND NOW", COLOR_RED);
        } else if (pitch < STALL_PITCH) {
            status = coloredText("⚠ STALL", COLOR_RED);
        } else if (pitch < CLIMB_UPPER_PITCH) {
            status = gradientStatus("CLIMB", pitch, IDEAL_CLIMB_PITCH, CLIMB_GRADIENT_HALF_WIDTH, true);
        } else if (pitch < GLIDE_RED_PITCH) {
            status = gradientStatus("GLIDE", pitch, IDEAL_GLIDE_PITCH, GLIDE_RED_PITCH, false);
        } else if (pitch <= APPROACH_UPPER_PITCH) {
            status = coloredText("→ APPROACH", 0x55FFFF);
        } else {
            status = coloredText("↓ DIVE", 0xFFAA00);
        }

        MutableComponent row = Component.literal("Elytra  ").withStyle(ChatFormatting.GRAY);
        row.append(Component.literal(String.format("%.1f°  ", pitch)).withStyle(ChatFormatting.WHITE));
        row.append(status);
        row.append(Component.literal("  "));
        row.append(Component.literal(String.format("Vy %.2f  ", vy)).withStyle(ChatFormatting.WHITE));
        row.append(Component.literal(String.format("H %.2f", horizontalSpeed)).withStyle(ChatFormatting.WHITE));
        return row;
    }

    /**
     * Builds a status label whose color shifts green -> orange -> red as pitch
     * moves away from an ideal value, with a leading arrow indicating which
     * way to correct. If {@code symmetric}, distance from ideal is measured in
     * both directions (climb); otherwise only pitch beyond ideal counts
     * (glide's one-directional distance-vs-speed spectrum).
     */
    private static Component gradientStatus(String label, float pitch, float idealPitch, float span, boolean symmetric) {
        float signedOffset = pitch - idealPitch;
        float distance = symmetric ? Math.abs(signedOffset) : Math.max(0f, signedOffset);
        float t = clamp01(distance / span);
        int color = threeStopGradient(t);

        // Pitch is more positive (more nose-down) than ideal in both the climb
        // and glide cases, so "pitch up to correct" always means the same
        // sign of offset regardless of which bucket this is called from.
        String arrow;
        if (signedOffset > ARROW_TOLERANCE) {
            arrow = "▲";
        } else if (signedOffset < -ARROW_TOLERANCE) {
            arrow = "▼";
        } else {
            arrow = "●";
        }

        return coloredText(arrow + " " + label, color);
    }

    private static Component coloredText(String text, int rgb) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static int threeStopGradient(float t) {
        t = clamp01(t);
        if (t < 0.5f) {
            return lerpColor(COLOR_GREEN, COLOR_ORANGE, t / 0.5f);
        }
        return lerpColor(COLOR_ORANGE, COLOR_RED, (t - 0.5f) / 0.5f);
    }

    private static int lerpColor(int from, int to, float t) {
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

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
