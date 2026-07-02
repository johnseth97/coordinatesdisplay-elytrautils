package com.johnseth97.cd_elytrautils.mixin;

import com.johnseth97.cd_elytrautils.CoordinatesDisplayElytraUtils;
import com.johnseth97.cd_elytrautils.ElytraUtilsConfig;
import com.johnseth97.cd_elytrautils.FlightColors;
import com.johnseth97.cd_elytrautils.FlightConstants;
import com.johnseth97.cd_elytrautils.FlightMath;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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

        ParagraphComponent rowComponent = new ParagraphComponent(0, buildFlightLine(player));
        ElytraUtilsConfig config = CoordinatesDisplayElytraUtils.getConfig();
        if (config.showImpactLine) {
            rowComponent.add(buildImpactLine(player));
        }
        if (config.showRangeLine) {
            rowComponent.add(buildRangeLine(player));
        }
        if (config.showFlightTimeLine) {
            rowComponent.add(buildFlightTimeLine(player));
        }
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

    // Bucket boundaries and gradient targets are derived from decompiled +
    // simulated vanilla physics; they were promoted to FlightConstants /
    // FlightColors so the fighter-jet instrument overlay (issue #10) draws its
    // angle brackets from the same numbers this text row scores against. See
    // GitHub issues #2 and #3 for the derivation.

    private static final float ARROW_TOLERANCE = 3f;

    // Impact damage estimate. Derived by decompiling LivingEntity's actual
    // damage formulas — not guessed:
    //   - Wall impact (fly_into_wall damage type): raw damage = horizontalSpeed*10 - 3
    //     (LivingEntity#handleFallFlyingCollisions models this as
    //     (speedBefore - speedAfter)*10 - 3; a HUD prediction assumes a wall
    //     fully arrests horizontal speed, i.e. speedAfter ~= 0).
    //   - Ground impact (fall damage type): raw damage = floor(max(0, fallDistance - safeFallDistance) * fallDamageMultiplier),
    //     using the player's live Entity#fallDistance and their actual
    //     SAFE_FALL_DISTANCE/FALL_DAMAGE_MULTIPLIER attributes rather than
    //     hardcoded vanilla defaults (3.0 / 1.0), so effects/attribute
    //     modifiers are respected.
    //   - Both damage types are tagged bypasses_armor (base armor points/
    //     toughness never apply), but enchantment "damage_protection" effects
    //     are a separate mechanic that still applies. Protection (1 point/level,
    //     summed across all 4 armor pieces) reduces both. Feather Falling
    //     (3 points/level, summed across all 4 armor pieces) is only tagged
    //     for is_fall damage, so it reduces ground impact only, NOT wall impact.
    //   - Reduction formula (CombatRules#getDamageAfterMagicAbsorb):
    //     damage * (1 - min(20, totalPoints) / 25).
    // See GitHub issue #4 for the full derivation.
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

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

    private static Component buildFlightLine(LocalPlayer player) {
        float pitch = player.getXRot();
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        Component status;
        if (!player.isFallFlying()) {
            // Pre-launch: elytra equipped + rockets in hand, but not airborne
            // yet (see shouldShowOverlay). Horizontal airspeed is ~0 on the
            // ground, so the STALL check below is meaningless here and would
            // always fire — show the same climb-angle gradient CLIMB uses
            // instead, since that's exactly what matters before jumping: is
            // this look angle a good one to launch and boost into?
            status = gradientStatus("LAUNCH", pitch, FlightConstants.IDEAL_CLIMB_PITCH, FlightConstants.CLIMB_GRADIENT_HALF_WIDTH, true);
        } else if (horizontalSpeed < FlightConstants.STALL_HORIZONTAL_SPEED) {
            status = coloredText("⚠ STALL", FlightColors.COLOR_RED);
        } else if (pitch < FlightConstants.CLIMB_UPPER_PITCH) {
            status = gradientStatus("CLIMB", pitch, FlightConstants.IDEAL_CLIMB_PITCH, FlightConstants.CLIMB_GRADIENT_HALF_WIDTH, true);
        } else if (pitch < FlightConstants.GLIDE_RED_PITCH) {
            status = gradientStatus("GLIDE", pitch, FlightConstants.IDEAL_GLIDE_PITCH, FlightConstants.GLIDE_RED_PITCH, false);
        } else {
            status = diveGradientStatus(vy);
        }

        MutableComponent row = Component.literal("Elytra  ").withStyle(ChatFormatting.GRAY);
        row.append(Component.literal(String.format("%.1f°  ", pitch)).withStyle(ChatFormatting.WHITE));
        row.append(status);
        row.append(Component.literal("  "));
        row.append(Component.literal(String.format("Vy %.2f  ", vy)).withStyle(ChatFormatting.WHITE));
        row.append(Component.literal(String.format("H %.2f", horizontalSpeed)).withStyle(ChatFormatting.WHITE));
        return row;
    }

    private static Component buildImpactLine(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float wallImpactHearts = estimateWallImpactHearts(player, horizontalSpeed);
        float fallImpactHearts = estimateFallImpactHearts(player);

        MutableComponent row = Component.literal("  Impact ").withStyle(ChatFormatting.GRAY);
        row.append(impactHeartsText("H", wallImpactHearts));
        row.append(Component.literal(" "));
        row.append(impactHeartsText("V", fallImpactHearts));
        return row;
    }

    /** Damage from flying horizontally into a wall right now, in hearts, after Protection. */
    private static float estimateWallImpactHearts(LocalPlayer player, double horizontalSpeed) {
        float rawDamage = (float) (horizontalSpeed * 10.0 - 3.0);
        if (rawDamage <= 0f) {
            return 0f;
        }
        float protection = sumEnchantmentPoints(player, Enchantments.PROTECTION, 1f, 1f);
        float epf = Math.min(20f, protection);
        return (rawDamage * (1f - epf / 25f)) / 2f;
    }

    /** Damage from hitting the ground right now, in hearts, after Protection + Feather Falling. */
    private static float estimateFallImpactHearts(LocalPlayer player) {
        float safeFallDistance = (float) player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        float fallDamageMultiplier = (float) player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        float fallDistance = (float) player.fallDistance;
        float rawDamage = (float) Math.floor(Math.max(0f, fallDistance - safeFallDistance) * fallDamageMultiplier);
        if (rawDamage <= 0f) {
            return 0f;
        }
        float protection = sumEnchantmentPoints(player, Enchantments.PROTECTION, 1f, 1f);
        float featherFalling = sumEnchantmentPoints(player, Enchantments.FEATHER_FALLING, 3f, 3f);
        float epf = Math.min(20f, protection + featherFalling);
        return (rawDamage * (1f - epf / 25f)) / 2f;
    }

    // Glide range math lives in FlightMath, shared with MasterCautionOverlay
    // (issue #7), which needs the same ground-distance and durability-range
    // numbers to decide when to warn. See GitHub issue #6.
    private static Component buildRangeLine(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        double vy = velocity.y;
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

        MutableComponent row = Component.literal("  Range ").withStyle(ChatFormatting.GRAY);
        if (vy >= 0.0) {
            row.append(Component.literal("-- (not descending)").withStyle(ChatFormatting.DARK_GRAY));
            return row;
        }

        FlightMath.GroundReading ground = FlightMath.findGroundDistance(player);
        double blocksRemaining = ground.distance() * (horizontalSpeed / -vy);

        row.append(Component.literal(String.format("%.0f blocks", blocksRemaining)).withStyle(ChatFormatting.WHITE));
        if (!ground.raycastHit()) {
            row.append(Component.literal(" (sea level est.)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return row;
    }

    // Time to ground: how long until the current glide slope actually meets
    // terrain, i.e. groundDistance / descentRate — the real answer to "can I
    // tab out for a bit." Deliberately NOT durability-based (that number
    // doesn't change tick to tick with your angle, which is why the previous
    // version of this line was stuck showing a near-constant value regardless
    // of how you were flying). Shares FlightMath.findGroundDistance with the
    // Range line (issue #6); Master Caution (#7) still uses the durability
    // math separately, that estimate wasn't wrong, just wrong for this row.
    private static Component buildFlightTimeLine(LocalPlayer player) {
        double vy = player.getDeltaMovement().y;

        MutableComponent row = Component.literal("  Time to ground ").withStyle(ChatFormatting.GRAY);
        if (vy >= FlightConstants.DIVE_GREEN_VY) {
            row.append(Component.literal("-- (not descending)").withStyle(ChatFormatting.DARK_GRAY));
            return row;
        }

        // accurateTicksToImpact accounts for what actually happens if the
        // elytra breaks before you reach the ground — simulated post-break
        // free-fall physics for the remainder, not the glide rate
        // extrapolated the whole way (see FlightMath's doc). Margin-free
        // "will this genuinely happen" check, independent of Master
        // Caution's configurable early-warning lead time (masterCautionThresholdBlocks
        // can be 0/off and this still tells the truth) — so it flashes red
        // on the same shared clock as Master Caution's banner (issue: keep
        // both warning treatments of the same underlying signal in sync)
        // whenever durability is genuinely the limiting factor.
        double ticksToGroundRaw = FlightMath.ticksToGround(player, vy);
        double accurateTicks = FlightMath.accurateTicksToImpact(player, vy);
        double durabilityTicks = FlightMath.durabilityLimitedTicks(player);
        boolean willBreakBeforeLanding = durabilityTicks >= 0.0 && durabilityTicks < ticksToGroundRaw;

        row.append(Component.literal("~").withStyle(ChatFormatting.GRAY));
        String durationText = formatDuration(accurateTicks / 20.0);
        if (willBreakBeforeLanding && FlightColors.isFlashOn()) {
            row.append(coloredText(durationText, FlightColors.COLOR_RED));
        } else {
            row.append(Component.literal(durationText).withStyle(ChatFormatting.WHITE));
        }
        FlightMath.GroundReading ground = FlightMath.findGroundDistance(player);
        if (!ground.raycastHit()) {
            row.append(Component.literal(" (sea level est.)").withStyle(ChatFormatting.DARK_GRAY));
        }
        return row;
    }

    private static String formatDuration(double seconds) {
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    /** Sums (base + perLevelAboveFirst*(level-1)) across every worn armor piece that has this enchantment. */
    private static float sumEnchantmentPoints(LocalPlayer player, net.minecraft.resources.ResourceKey<Enchantment> enchantmentKey, float base, float perLevelAboveFirst) {
        Holder<Enchantment> holder;
        try {
            holder = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
        } catch (RuntimeException e) {
            return 0f;
        }
        float total = 0f;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            int level = player.getItemBySlot(slot).getEnchantments().getLevel(holder);
            if (level > 0) {
                total += base + perLevelAboveFirst * (level - 1);
            }
        }
        return total;
    }

    private static Component impactHeartsText(String axisLabel, float hearts) {
        int color = hearts <= 0f ? FlightColors.COLOR_GREEN : hearts >= 10f ? FlightColors.COLOR_RED : FlightColors.threeStopGradient(hearts / 10f);
        return coloredText(String.format("%s:%.1f❤", axisLabel, hearts), color);
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
        int color = FlightColors.gradientColor(pitch, idealPitch, span, symmetric);

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

    /** DIVE's danger gradient: green at a mild descent rate, red at the old LAND NOW threshold. See issue #8. */
    private static Component diveGradientStatus(double vy) {
        float t = FlightColors.clamp01((float) ((FlightConstants.DIVE_GREEN_VY - vy)
                / (FlightConstants.DIVE_GREEN_VY - FlightConstants.DIVE_RED_VY)));
        int color = FlightColors.diveGradientColor(vy);
        String arrow = t > 0.5f ? "▲" : "↓";
        return coloredText(arrow + " DIVE", color);
    }

    private static Component coloredText(String text, int rgb) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }
}
