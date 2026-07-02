package com.johnseth97.cd_elytrautils.mixin;

import com.johnseth97.cd_elytrautils.CoordinatesDisplayElytraUtils;
import dev.boxadactle.boxlib.layouts.RenderingLayout;
import dev.boxadactle.boxlib.layouts.component.LayoutContainerComponent;
import dev.boxadactle.boxlib.layouts.component.ParagraphComponent;
import dev.boxadactle.boxlib.layouts.layout.ColumnLayout;
import dev.boxadactle.coordinatesdisplay.Hud;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wraps CoordinatesDisplay's assembled HUD layout in a new outer column that
 * adds an elytra telemetry row, so the extra row's size is folded into the
 * layout's rect before {@link Hud#preRender} computes screen position.
 */
@Mixin(value = Hud.class, remap = false)
public abstract class HudMixin {

    @ModifyVariable(method = "preRender", at = @At(value = "STORE", ordinal = 0))
    private RenderingLayout cd_elytrautils$appendElytraRow(RenderingLayout layout) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isFallFlying() || !CoordinatesDisplayElytraUtils.getConfig().showElytraOverlay) {
            return layout;
        }

        ColumnLayout wrapper = new ColumnLayout(0, 0, 2);
        wrapper.addComponent(new LayoutContainerComponent(layout));
        wrapper.addComponent(new ParagraphComponent(0, buildElytraRow(player)));
        return wrapper;
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
