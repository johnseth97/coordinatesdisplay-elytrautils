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

        String statusText;
        ChatFormatting statusColor;
        if (vy < -0.45) {
            statusText = "LAND NOW";
            statusColor = ChatFormatting.RED;
        } else if (pitch < -55f) {
            statusText = "⚠ STALL";
            statusColor = ChatFormatting.RED;
        } else if (pitch < -30f) {
            statusText = "↑ CLIMB";
            statusColor = ChatFormatting.YELLOW;
        } else if (pitch < -12f) {
            statusText = "✓ GLIDE";
            statusColor = ChatFormatting.GREEN;
        } else if (pitch <= 3f) {
            statusText = "→ APPROACH";
            statusColor = ChatFormatting.AQUA;
        } else {
            statusText = "↓ DIVE";
            statusColor = ChatFormatting.GOLD;
        }

        MutableComponent row = Component.literal("Elytra  ").withStyle(ChatFormatting.GRAY);
        row.append(Component.literal(String.format("%.1f°  ", pitch)).withStyle(ChatFormatting.WHITE));
        row.append(Component.literal(statusText + "  ").withStyle(statusColor));
        row.append(Component.literal(String.format("Vy %.2f  ", vy)).withStyle(ChatFormatting.WHITE));
        row.append(Component.literal(String.format("H %.2f", horizontalSpeed)).withStyle(ChatFormatting.WHITE));
        return row;
    }
}
